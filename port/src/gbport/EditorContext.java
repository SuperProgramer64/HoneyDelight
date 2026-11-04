package gbport;

/** Immutable surrounding text, captured before the caller waits for the decoder. */
final class EditorContext {
    final String before, after;
    EditorContext(String before, String after) { this.before = before; this.after = after; }

    static EditorContext fromRaw(String rawBefore, String rawAfter, String word) {
        String before = rawBefore;
        String after = Bridge.afterComposition(rawBefore, rawAfter, word);
        if (word != null && !word.isEmpty()) {
            if (before.endsWith(word)) before = before.substring(0, before.length() - word.length());
            else {
                int i = before.length();
                while (i > 0 && (Character.isLetter(before.charAt(i - 1)) || before.charAt(i - 1) == '\'' || before.charAt(i - 1) == '’')) i--;
                String run = Decide.norm(before.substring(i)), normalized = Decide.norm(word);
                int removedAfter = rawAfter.length() - after.length();
                String fullRun = run + Decide.norm(rawAfter.substring(0, removedAfter));
                if (!run.isEmpty() && (normalized.startsWith(run) || run.startsWith(normalized)
                        || (run.length() >= 4 && normalized.length() >= 5 && Decide.oneEdit(run, normalized))
                        || (removedAfter > 0 && fullRun.length() >= 4 && normalized.length() >= 5
                            && (fullRun.equals(normalized) || Decide.oneEdit(fullRun, normalized)))))
                    before = before.substring(0, i);
            }
        }
        return new EditorContext(before, after);
    }

    static EditorContext fromSelection(CharSequence text, int start, int end, String word) {
        if (text == null || start < 0 || end < 0 || start > text.length() || end > text.length()) return null;
        int left = Math.min(start, end), right = Math.max(start, end);
        return fromRaw(text.subSequence(Math.max(0, left - Bridge.CTX_BEFORE), left).toString(),
                text.subSequence(right, Math.min(text.length(), right + Bridge.CTX_AFTER)).toString(), word);
    }
}
