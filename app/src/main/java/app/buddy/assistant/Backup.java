package app.buddy.assistant;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Backup and move (Settings): chats, pictures and appearance settings. Linux state stays private: projects and
 * agent databases can contain credentials alongside ordinary files.
 */
final class Backup {
    private static final String[] PREFS = {"buddy", "buddy_sessions"};
    private static final java.util.Set<String> SETTINGS = new java.util.HashSet<>(java.util.Arrays.asList(
            "bx", "by", "bubble", "tap_action", "panel_size", "face", "theme_mode", "accent", "surface",
            "term_font", "term_awake", "voice_engine", "voice_langs", "voice_lang", "speech_engine", "speech_voice"));

    private Backup() {
    }

    /** Writes the backup to Downloads/Buddy and returns its name. */
    static String create(Context c) throws Exception {
        String name = "buddy-backup-" + new SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.ROOT).format(new Date()) + ".zip";
        File tmp = new File(c.getCacheDir(), "backup.zip");
        File prefsDir = new File(c.getApplicationInfo().dataDir, "shared_prefs");
        try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(tmp))) {
            z.putNextEntry(new ZipEntry("buddy-backup.txt"));
            z.write(("Buddy " + BuildInfo.VERSION + " backup\n").getBytes());
            for (String p : PREFS) {
                File f = new File(prefsDir, p + ".xml");
                if (!f.isFile()) continue;
                String xml = new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
                xml = settingsXml(xml, p);
                z.putNextEntry(new ZipEntry("prefs/" + p + ".xml"));
                z.write(xml.getBytes("UTF-8"));
            }
            addTree(z, new File(c.getFilesDir(), "chats"), "files/chats/");
            addTree(z, new File(c.getFilesDir(), "attachments"), "files/attachments/");
            File face = new File(c.getFilesDir(), "face.png");
            if (face.isFile()) addFile(z, face, "files/face.png");
        }
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        v.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Buddy");
        v.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
        Uri u = c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (u == null) throw new Exception("Couldn't write to Downloads.");
        try (OutputStream o = c.getContentResolver().openOutputStream(u); InputStream in = new FileInputStream(tmp)) {
            pump(in, o);
        } finally {
            tmp.delete();
        }
        return name;
    }

    /**
     * Puts a backup's chats, settings and files in place of this phone's, then restarts Buddy so they're read fresh.
     * Returns false when a legacy Linux archive was excluded.
     */
    static boolean restore(Context c, Uri uri) throws Exception {
        File prefsDir = new File(c.getApplicationInfo().dataDir, "shared_prefs");
        File staging = new File(c.getCacheDir(), "restore");
        Linux.deleteTree(staging);
        staging.mkdirs();
        boolean ok = false;
        String root = staging.getCanonicalPath() + File.separator;
        try (InputStream in = c.getContentResolver().openInputStream(uri); ZipInputStream z = new ZipInputStream(in)) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) {
                if (e.isDirectory()) continue;
                File out = new File(staging, e.getName());
                if (!out.getCanonicalPath().startsWith(root)) continue;
                out.getParentFile().mkdirs();
                try (OutputStream o = new FileOutputStream(out)) {
                    pump(z, o);
                }
                if (e.getName().equals("buddy-backup.txt")) ok = true;
            }
        }
        if (!ok) throw new IllegalArgumentException(c.getString(R.string.backup_not_one));
        File linux = new File(staging, "linux.tar.gz");
        java.util.Map<String, String> settings = new java.util.HashMap<>();
        for (String p : PREFS) {
            File f = new File(staging, "prefs/" + p + ".xml");
            if (f.isFile()) settings.put(p, settingsXml(new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8"), p));
        }
        // Legacy Linux archives are never extracted: they can overwrite credentials or executable configuration.
        File att = new File(staging, "files/attachments");
        if (att.isDirectory()) {
            File dest = new File(c.getFilesDir(), "attachments");
            dest.mkdirs();
            File[] fs = att.listFiles();
            if (fs != null) for (File f : fs) if (f.isFile()) copyFile(f, new File(dest, f.getName()));
        }
        File face = new File(staging, "files/face.png");
        if (face.isFile()) copyFile(face, new File(c.getFilesDir(), "face.png"));
        // the chats are the backup's: its chat files (2.2.26 on), or none, and then the chats in its settings (before);
        // the chats in memory save nothing more until the restart, so they can't write over them
        ChatHub.get(c).sessions.freeze();
        File chats = new File(c.getFilesDir(), "chats");
        Linux.deleteTree(chats);
        chats.mkdirs();
        File savedChats = new File(staging, "files/chats");
        File[] cs = savedChats.listFiles();
        if (cs != null) for (File f : cs) if (f.isFile() && f.getName().endsWith(".json")) copyFile(f, new File(chats, f.getName()));
        // Credentials never come from the backup; invalidate any previously exported control token.
        String token = Prefs.rotateToken(c), providers = Prefs.providers(c);
        java.util.Map<String, String> defaults = new java.util.HashMap<>();
        for (Account a : Account.ALL) defaults.put(a.agent, Prefs.agentDefault(c, a.agent));
        for (String p : PREFS) {
            String xml = settings.get(p);
            if (xml == null) continue;
            if (p.equals("buddy")) {
                StringBuilder keep = new StringBuilder("<string name=\"token\">" + esc(token) + "</string>\n")
                        .append("<string name=\"providers\">").append(esc(providers)).append("</string>\n");
                for (java.util.Map.Entry<String, String> d : defaults.entrySet()) {
                    keep.append("<string name=\"agent_default_").append(d.getKey()).append("\">").append(esc(d.getValue())).append("</string>\n");
                }
                xml = xml.replace("</map>", keep + "</map>");
            }
            c.getSharedPreferences(p, Context.MODE_PRIVATE); // loaded, so nothing pending writes over the new file later
            try (OutputStream o = new FileOutputStream(new File(prefsDir, p + ".xml"))) {
                o.write(xml.getBytes("UTF-8"));
            }
        }
        boolean skipped = linux.isFile();
        Linux.deleteTree(staging);
        return !skipped;
    }

    /** Starts Buddy again in a new process, so the restored settings and chats are what it reads. */
    static void restart(Context c) {
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(c, 0, i,
                android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_CANCEL_CURRENT);
        android.app.AlarmManager am = (android.app.AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 800, pi);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /** The same allowlist applies to export and to old or untrusted imports. Unknown settings stay out. */
    private static String settingsXml(String xml, String prefs) throws Exception {
        org.xmlpull.v1.XmlPullParser p = android.util.Xml.newPullParser();
        p.setInput(new java.io.StringReader(xml));
        java.io.StringWriter out = new java.io.StringWriter();
        org.xmlpull.v1.XmlSerializer w = android.util.Xml.newSerializer();
        w.setOutput(out);
        w.startDocument("UTF-8", true);
        w.startTag(null, "map");
        w.text("\n");
        for (int event; (event = p.nextToken()) != org.xmlpull.v1.XmlPullParser.END_DOCUMENT; ) {
            if (event == org.xmlpull.v1.XmlPullParser.DOCDECL) throw new IllegalArgumentException("DTD in settings");
            if (event != org.xmlpull.v1.XmlPullParser.START_TAG || p.getDepth() != 2) continue;
            String key = p.getAttributeValue(null, "name"), type = p.getName();
            boolean keep = "buddy".equals(prefs) ? SETTINGS.contains(key)
                    : "current".equals(key) || "order".equals(key) || "all".equals(key);
            if (!keep || !("string".equals(type) || "int".equals(type) || "long".equals(type)
                    || "float".equals(type) || "boolean".equals(type))) continue;
            w.startTag(null, type).attribute(null, "name", key);
            if ("string".equals(type)) w.text(p.nextText());
            else w.attribute(null, "value", p.getAttributeValue(null, "value"));
            w.endTag(null, type);
        }
        w.endTag(null, "map");
        w.endDocument();
        return out.toString();
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void addTree(ZipOutputStream z, File dir, String prefix) throws Exception {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) addTree(z, f, prefix + f.getName() + "/");
            else addFile(z, f, prefix + f.getName());
        }
    }

    private static void addFile(ZipOutputStream z, File f, String name) throws Exception {
        z.putNextEntry(new ZipEntry(name));
        try (InputStream in = new FileInputStream(f)) {
            pump(in, z);
        }
        z.closeEntry();
    }

    /** in to out; neither is closed. */
    private static void pump(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

    private static void copyFile(File from, File to) throws Exception {
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
            pump(in, out);
        }
    }
}
