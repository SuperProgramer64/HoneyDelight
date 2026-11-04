package com.google.android.keyboard.client.delight5;

// Firmas JNI idénticas a Gboard 18.5.6 para que RegisterNatives las encuentre.
public final class LanguageIdentifier {
    public static native long createLanguageIdentifierNative(byte[] bArr);
    public static native byte[] identifyLanguageNative(byte[] bArr, long j);
    public static native byte[] identifyLanguagesNative(byte[] bArr, long j);
    public static native void releaseLanguageIdentifierNative(long j);
    public static native void setLanguageFilterNative(byte[] bArr, long j);
    public static native void unloadDictionaryNative(long j);
    public static native void unloadLangIDModelNative(long j);
}
