package android.util;

// Host fixture for Bridge initialization. These classes are excluded from the mod.
public final class Log {
    public static int calls;
    public static int i(String tag, String message) { calls++; return 0; }
    public static int w(String tag, String message) { calls++; return 0; }
}
