package gbport.test;

import gbport.Bridge;
import java.io.File;
import java.lang.reflect.*;
import java.nio.file.Files;

public final class StartupRegression {
    static Field field(String name) throws Exception {
        Field f = Bridge.class.getDeclaredField(name); f.setAccessible(true); return f;
    }
    public static void main(String[] args) throws Exception {
        Method start = Bridge.class.getDeclaredMethod("start"); start.setAccessible(true);
        start.invoke(null);
        if (field("state").getInt(null) != 0)
            throw new AssertionError("Prediction before the service context permanently disables Delight");
        Method configure = Bridge.class.getDeclaredMethod("configureDirectory", File.class);
        configure.setAccessible(true);
        File first = Files.createTempDirectory("honeydelight-profile-a").toFile();
        File second = Files.createTempDirectory("honeydelight-profile-b").toFile();
        try {
            configure.invoke(null, first);
            start.invoke(null);
            if (field("state").getInt(null) != -1)
                throw new AssertionError("Missing base dictionaries must fail without invoking JNI");
            long retry = field("retryAtNanos").getLong(null);
            start.invoke(null);
            if (field("retryAtNanos").getLong(null) != retry)
                throw new AssertionError("Every keystroke repeats failed initialization instead of respecting cooldown");
            configure.invoke(null, second);
            if (!new File(second, "gbport").getAbsolutePath().equals(field("DIR").get(null)))
                throw new AssertionError("The service profile does not own the model directory");
            if (field("state").getInt(null) != 0)
                throw new AssertionError("A corrected profile directory cannot recover after an early failure");
        } finally { first.delete(); second.delete(); }
        System.out.println("PASS context-before-start, per-profile directory, startup recovery and retry cooldown");
    }
}
