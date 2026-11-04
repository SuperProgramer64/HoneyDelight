package gbport;

/** Snapshot for Samsung's EngineBuilderThread while the local editor's main thread waits for it. */
final class LocalEditorText {
    final CharSequence before, after;
    final long capturedAt = System.nanoTime();
    private LocalEditorText(CharSequence before, CharSequence after) { this.before = before; this.after = after; }
    static LocalEditorText capture(CharSequence text, int start, int end) {
        if (text == null || start < 0 || end < 0 || start > text.length() || end > text.length()) return null;
        CharSequence copy = text instanceof android.text.Spanned ? new android.text.SpannedString(text) : text.toString();
        int left = Math.min(start,end), right = Math.max(start,end);
        return new LocalEditorText(copy.subSequence(0,left), copy.subSequence(right,copy.length()));
    }
    CharSequence read(boolean preceding, int count, int flags) {
        if (count < 0) return null;
        CharSequence side = preceding ? before : after;
        int n = Math.min(count, side.length());
        CharSequence result = preceding ? side.subSequence(side.length()-n,side.length()) : side.subSequence(0,n);
        return (flags & 1) != 0 ? result : result.toString();
    }
    EditorContext context(String word) { return EditorContext.fromRaw(before.toString(),after.toString(),word); }
}
