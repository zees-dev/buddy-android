package app.buddy.assistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** The "Buddy is hidden" notification shown after the bubble is dragged away, and its actions. */
public final class HiddenNotice extends BroadcastReceiver {
    private static final String CHANNEL = "bubble_hidden";
    private static final int ID = 1;
    static final String ACTION_SHOW = "app.buddy.assistant.SHOW_BUBBLE";
    static final String ACTION_EXIT = "app.buddy.assistant.EXIT";

    static void show(Context c) {
        if (Prefs.exited(c)) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, c.getString(R.string.notice_channel_name),
                NotificationManager.IMPORTANCE_LOW));
        PendingIntent showBubble = PendingIntent.getBroadcast(c, 0,
                new Intent(c, HiddenNotice.class).setAction(ACTION_SHOW),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent openApp = PendingIntent.getActivity(c, 1,
                new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent exit = PendingIntent.getBroadcast(c, 2,
                new Intent(c, HiddenNotice.class).setAction(ACTION_EXIT),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent forceStop = PendingIntent.getActivity(c, 3,
                new Intent(c, ExitActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(c.getString(R.string.notice_hidden_title))
                .setContentText(c.getString(R.string.notice_hidden_text))
                .setContentIntent(showBubble)
                .addAction(new Notification.Action.Builder(null, c.getString(R.string.notice_open_chats), openApp).build())
                .addAction(new Notification.Action.Builder(null, c.getString(R.string.notice_exit), exit).build())
                .addAction(new Notification.Action.Builder(null, c.getString(R.string.notice_force_stop), forceStop).build())
                .setOngoing(false)
                .setAutoCancel(true)
                .build();
        try {
            nm.notify(ID, n);
        } catch (SecurityException ignored) {
            // notifications not allowed; the bubble can still be shown again from Settings
        }
    }

    static void cancel(Context c) {
        c.getSystemService(NotificationManager.class).cancel(ID);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (ACTION_EXIT.equals(intent.getAction())) {
            AppLifecycle.exit(c, false);
            return;
        }
        if (Prefs.exited(c)) return;
        if (ControlIndicator.ACTION_STOP.equals(intent.getAction())) {
            BuddyService s = BuddyService.get();
            if (s != null) s.takeBackControl();
            return;
        }
        if (CodexLive.ACTION_END.equals(intent.getAction())) { // the live voice notification's End
            CodexLive.end(null);
            return;
        }
        if (AutoPause.ACTION_RESUME.equals(intent.getAction())) { // "Call ended": resume the paused task
            BuddyService s = BuddyService.get();
            if (s != null) s.resume();
            AutoPause.cancelCallEnded(c); // also when it had ended already
            return;
        }
        if (!ACTION_SHOW.equals(intent.getAction())) return;
        Prefs.setBubbleEnabled(c, true);
        cancel(c);
        BuddyService svc = BuddyService.get();
        if (svc != null) svc.applyBubbleSetting();
    }
}
