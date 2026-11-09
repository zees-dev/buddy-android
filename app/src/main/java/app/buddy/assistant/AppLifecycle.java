package app.buddy.assistant;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.system.Os;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** User-requested exit is durable; Android may still bind an otherwise idle service. */
final class AppLifecycle {
    private static final Set<Process> processes = new HashSet<>();
    private static final Set<Integer> terminals = new HashSet<>();
    private static volatile boolean shuttingDown;
    private static boolean saveFailed;
    private static boolean shutdownComplete;
    private static volatile int generation;
    private static final InheritableThreadLocal<Integer> workGeneration = new InheritableThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return -1;
        }
    };

    private AppLifecycle() {
    }

    static boolean stopping() {
        return shuttingDown;
    }

    static void initialize() {
        workGeneration.set(generation);
    }

    static int workGeneration() {
        return workGeneration.get();
    }

    static void bindWork(int expected) {
        workGeneration.set(expected);
    }

    static boolean currentWork() {
        return workGeneration.get() == generation;
    }

    static synchronized void publish(Runnable update) {
        if (currentWork()) update.run();
    }

    static Runnable wrapWork(Runnable work) {
        int expected = workGeneration.get();
        return () -> {
            int previous = workGeneration.get();
            workGeneration.set(expected);
            try {
                if (currentWork()) work.run();
            } finally {
                workGeneration.set(previous);
            }
        };
    }

    /** Runtime callbacks cannot revive a stopped runtime or carry an old request into a reopened one. */
    static Handler workHandler() {
        return new Handler(Looper.getMainLooper()) {
            @Override
            public boolean sendMessageAtTime(android.os.Message message, long uptime) {
                Context c = BuddyApp.context();
                if (c == null || !currentWork() || Prefs.exited(c)) return false;
                message.arg1 = workGeneration.get();
                return super.sendMessageAtTime(message, uptime);
            }

            @Override
            public void dispatchMessage(android.os.Message message) {
                Context c = BuddyApp.context();
                if (c != null && message.arg1 == generation && !Prefs.exited(c)) super.dispatchMessage(message);
            }
        };
    }

    /** A network-state change pumps current queues; it is not a replay of the callback's original work. */
    static void postSystemState(Handler handler, Runnable work) {
        int previous = workGeneration.get();
        workGeneration.set(generation);
        try {
            handler.post(work);
        } finally {
            workGeneration.set(previous);
        }
    }

    static synchronized Process start(Context c, ProcessBuilder builder) throws IOException {
        requireRunning(c);
        processes.removeIf(p -> !p.isAlive());
        Process p = builder.start();
        processes.add(p);
        return p;
    }

    static synchronized int[] startTerminal(Context c, String path, String[] args, String[] env,
                                           String cwd, int rows, int cols) throws IOException {
        requireRunning(c);
        int[] result = Pty.start(path, args, env, cwd, rows, cols);
        terminals.add(result[1]);
        return result;
    }

    static synchronized void terminalEnded(int pid) {
        if (currentWork()) terminals.remove(pid);
    }

    static void requireRunning(Context c) throws IOException {
        if (!currentWork() || Prefs.exited(c)) throw new IOException(c.getString(R.string.exit_stopped));
    }

    /** Only explicit launcher/share entry calls this, never a service or a broadcast. Main thread. */
    static boolean reopen(Activity a) {
        synchronized (AppLifecycle.class) {
            if (shuttingDown && !shutdownComplete) {
                if (saveFailed) {
                    saveFailed = false;
                    ChatHub hub = ChatHub.existing();
                    if (hub != null) hub.prepareExit();
                    finishExit(a.getApplicationContext(), hub);
                }
                Toast.makeText(a, R.string.exit_still_stopping, Toast.LENGTH_LONG).show();
                return false;
            }
            if (!Prefs.exited(a)) return true;
            if (!Prefs.setExited(a, false)) {
                Prefs.setExited(a, true);
                Toast.makeText(a, R.string.exit_save_failed, Toast.LENGTH_LONG).show();
                return false;
            }
            generation++;
            workGeneration.set(generation);
            shuttingDown = false;
            shutdownComplete = false;
        }
        BuddyService service = BuddyService.get();
        if (service != null) service.startRuntime();
        return true;
    }

    /** Stops admission first, saves chats and ends native work; the bound Java service stays idle. Main thread. */
    static void exit(Context c, boolean forceStop) {
        Context app = c.getApplicationContext();
        boolean first;
        synchronized (AppLifecycle.class) {
            first = !shuttingDown;
            if (first) {
                generation++;
                workGeneration.set(generation);
                shuttingDown = true;
                shutdownComplete = false;
                Prefs.setExited(app, true); // final flush retries a failed disk write; admission is already stopped
            }
        }
        BuddyService service = BuddyService.get();
        if (service != null) service.stopRuntime();
        if (forceStop) {
            try {
                c.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", app.getPackageName(), null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(app, R.string.exit_settings_unavailable, Toast.LENGTH_LONG).show();
            }
        }
        if (!first) return;
        Voice.stopListening();
        Voice.stopSpeaking();
        ClaudeDictation.shutdown();
        MicServer.endCapture();
        MicServer.stop();
        CodexLive.end(null);
        TermSession.finishAll();
        GitHub.cancel();
        for (Account account : Account.ALL) account.cancelLogin();
        ChatHub hub = ChatHub.existing();
        Schedules.stop(app, hub);
        Triggers.stop();
        if (hub != null) hub.prepareExit();
        app.getSystemService(NotificationManager.class).cancelAll();
        for (ActivityManager.AppTask task : app.getSystemService(ActivityManager.class).getAppTasks()) {
            try {
                task.finishAndRemoveTask();
            } catch (RuntimeException ignored) { // the task was already removed
            }
        }
        finishExit(app, hub);
    }

    private static void finishExit(Context app, ChatHub hub) {
        new Thread(() -> {
            boolean saved = false;
            try {
                try {
                    Browser.stop();
                } finally {
                    stopChildren();
                }
                CodexDictation.shutdown();
                boolean durableExit = Prefs.setExited(app, true);
                saved = (hub == null || hub.sessions.flushForExit()) && durableExit;
            } catch (Exception e) {
                android.util.Log.e("Buddy", "Exit cleanup failed", e);
            }
            final boolean complete = saved;
            new Handler(Looper.getMainLooper()).post(() -> {
                shutdownComplete = complete;
                saveFailed = !complete;
                if (!complete) Toast.makeText(app, R.string.exit_save_failed, Toast.LENGTH_LONG).show();
            });
        }, "buddy-exit").start();
    }

    private static void stopChildren() {
        // A terminal child can detach from its parent/session (the private SSH tunnel does).
        // Android gives this package its own UID; never signal a different UID or this process here.
        for (int pass = 0; pass < 3; pass++) {
            File[] entries = new File("/proc").listFiles();
            if (entries == null) break;
            for (File entry : entries) {
                try {
                    int pid = Integer.parseInt(entry.getName());
                    if (pid != android.os.Process.myPid()
                            && Os.stat(entry.getPath()).st_uid == android.os.Process.myUid()) {
                        android.os.Process.killProcess(pid);
                    }
                } catch (Exception ignored) { // a process exited, or belongs to a different app
                }
            }
        }
        Process[] children;
        Integer[] pids;
        synchronized (AppLifecycle.class) {
            children = processes.toArray(new Process[0]);
            pids = terminals.toArray(new Integer[0]);
            processes.clear();
            terminals.clear();
        }
        for (Process p : children) p.destroy();
        for (int pid : pids) Pty.hangup(pid);
    }
}
