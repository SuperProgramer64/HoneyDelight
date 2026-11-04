package gbport;

import android.content.Context;
import java.io.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/** First-run setup only for the separately signed experimental APK. */
final class ApkBootstrap {
    private static final AtomicBoolean running = new AtomicBoolean();
    private static volatile long retryAfter;

    static void prepare(Context context) {
        if (context == null || System.nanoTime() < retryAfter || !running.compareAndSet(false, true)) return;
        Context app = context.getApplicationContext();
        File directory = new File(context.getFilesDir(), "gbport");
        if (new File(directory, "es_us.dict").isFile() && new File(directory, "en_us.dict").isFile()) {
            running.set(false);
            return;
        }
        new Thread(() -> {
            try {
                Properties pins = new Properties();
                try (InputStream input = app.getAssets().open("honeydelight/base.properties")) { pins.load(input); }
                for (String name : new String[]{"es_us.dict", "en_us.dict"})
                    ApkModels.installIfMissing(new File(directory, name),
                        () -> app.getAssets().open("honeydelight/" + name), pins.getProperty(name, ""));
                synchronized (Bridge.class) {
                    if (directory.getAbsolutePath().equals(Bridge.DIR) && Bridge.state != 1 && Bridge.state != 2) {
                        Bridge.state = 0;
                        Bridge.retryAtNanos = 0;
                        Bridge.start();
                    }
                }
            } catch (IOException error) {
                retryAfter = System.nanoTime() + 5_000_000_000L;
                android.util.Log.e(Bridge.TAG, "No se pudieron preparar los modelos empaquetados", error);
            } finally { running.set(false); }
        }, "HoneyDelight-apk-models").start();
    }
}
