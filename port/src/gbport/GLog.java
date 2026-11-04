package gbport;

import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Registro persistente de decisiones del motor (logcat solo guarda unas horas). Se escribe en un hilo aparte
// en files/gbport/log/decisiones.txt (privado del teclado); al pasar de 4 MB se rota a decisiones.1.txt.
final class GLog {
    static final long MAX = 4L << 20;
    static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "GboardPort-log");
        t.setDaemon(true);
        return t;
    });
    static final SimpleDateFormat TS = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);
    static volatile boolean enabled = false;

    private GLog() {}

    static void w(String msg) {
        if (!enabled) return;
        Log.i(Bridge.TAG, msg);
        final String line = TS.format(new Date()) + " " + msg.replace('\n', '⏎') + "\n";
        try {
            IO.execute(() -> append(line));
        } catch (Throwable e) {
            // cola cerrada: solo logcat
        }
    }

    // Toques reales por palabra (files/gbport/log/toques.txt) para medir en el arnés si la posición del dedo mejora la
    // corrección: palabra tecleada, confirmada, contexto y coordenadas en el marco del motor.
    static void touches(String line) {
        if (!enabled) return;
        try {
            IO.execute(() -> appendTo("toques.txt", line + "\n", 2L << 20));
        } catch (Throwable e) {
            // ignorar
        }
    }

    private static void appendTo(String name, String line, long max) {
        try {
            File dir = new File(Bridge.DIR, "log");
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            File f = new File(dir, name);
            if (f.length() > max) {
                File old = new File(dir, name.replace(".txt", ".1.txt"));
                old.delete();
                f.renameTo(old);
            }
            try (FileOutputStream o = new FileOutputStream(f, true)) {
                o.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable e) {
            // sin disco
        }
    }

    private static void append(String line) {
        try {
            File dir = new File(Bridge.DIR, "log");
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            File f = new File(dir, "decisiones.txt");
            if (f.length() > MAX) {
                File old = new File(dir, "decisiones.1.txt");
                old.delete();
                f.renameTo(old);
            }
            try (FileOutputStream o = new FileOutputStream(f, true)) {
                o.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable e) {
            // sin disco: solo logcat
        }
    }
}
