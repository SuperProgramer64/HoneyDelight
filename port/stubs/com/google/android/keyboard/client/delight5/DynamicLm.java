package com.google.android.keyboard.client.delight5;

// Firmas JNI idénticas a Gboard 18.5.6 para que RegisterNatives las encuentre.
public final class DynamicLm {
    public static native void clearDynamicLmNative(byte[] bArr);
    public static native void closeDynamicLmNative(byte[] bArr);
    public static native void flushDynamicLmNative(byte[] bArr);
    public static native byte[] getDynamicLmStatsNative(byte[] bArr);
    public static native byte[] getNgramFromDynamicLmNative(byte[] bArr);
    public static native byte[] incrementNgramInDynamicLmNative(byte[] bArr);
    public static native byte[] iterateOverDynamicLmNative(byte[] bArr);
    public static native boolean openDynamicLmNative(byte[] bArr);
    public static native void pruneDynamicLmIfNeededNative(byte[] bArr);
    public static native void setNgramInDynamicLmNative(byte[] bArr);
    public static native boolean validateDynamicLmNative(byte[] bArr);
}
