package com.google.android.keyboard.client.delight5;

// Firmas JNI idénticas a Gboard 18.5.6 para que RegisterNatives las encuentre.
public final class ConceptPredictionApi {
    public static native byte[] nativeConceptPredictionConceptList();
    public static native byte[] nativeConceptPredictionConceptListWithHandleNative(long j);
    public static native byte[] nativeConceptPredictionEmojiFromConcept(byte[] bArr);
    public static native byte[] nativeConceptPredictionEmojiFromConceptWithHandleNative(long j, byte[] bArr);
    public static native byte[] nativeConceptPredictionGetUserPreference();
    public static native byte[] nativeConceptPredictionGetUserPreferenceWithHandleNative(long j);
    public static native boolean nativeConceptPredictionIsReady();
    public static native boolean nativeConceptPredictionIsReadyWithHandleNative(long j);
    public static native String[] nativeConceptPredictionPredictConcepts(byte[] bArr);
    public static native String[] nativeConceptPredictionPredictConceptsWithHandleNative(long j, byte[] bArr);
    public static native String[] nativeConceptPredictionPredictEmojis(byte[] bArr, int i, boolean z);
    public static native String[] nativeConceptPredictionPredictEmojisWithHandleNative(long j, byte[] bArr, int i, boolean z);
    public static native byte[] nativeConceptPredictionRankedEmojiPrediction(byte[] bArr);
    public static native byte[] nativeConceptPredictionRankedEmojiPredictionWithHandleNative(long j, byte[] bArr);
    public static native boolean nativeConceptPredictionSetUserPreference(byte[] bArr);
    public static native boolean nativeConceptPredictionSetUserPreferenceForConcept(byte[] bArr, byte[] bArr2);
    public static native boolean nativeConceptPredictionSetUserPreferenceForConceptWithHandleNative(long j, byte[] bArr, byte[] bArr2);
    public static native boolean nativeConceptPredictionSetUserPreferenceWithHandleNative(long j, byte[] bArr);
    public static native byte[] nativeConceptPredictionStopwordList();
    public static native byte[] nativeConceptPredictionStopwordListWithHandleNative(long j);
}
