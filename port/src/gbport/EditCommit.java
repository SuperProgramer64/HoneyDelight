package gbport;

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import java.lang.ref.WeakReference;

/** Returns a capsule rewrite to the original field after Samsung closes its editor. */
public final class EditCommit {
    private static EditCommit active;
    private final WeakReference<Object> model;
    private final String packageName, original;
    private final int fieldId, offset, start, end;
    private WeakReference<EditText> editor;
    private boolean rewritten;
    private String result;
    private long deadline;

    private EditCommit(Object model, EditorInfo info, ExtractedText text) {
        this.model = new WeakReference<>(model);
        packageName = info.packageName;
        fieldId = info.fieldId;
        original = text.text.toString();
        offset = text.startOffset;
        start = Math.min(text.selectionStart, text.selectionEnd);
        end = Math.max(text.selectionStart, text.selectionEnd);
    }

    public static void begin(Object model) {
        active = null;
        try {
            InputMethodService service = Bridge.svc == null ? null : Bridge.svc.get();
            if (service == null || Bridge.secret) return;
            EditorInfo info = service.getCurrentInputEditorInfo();
            InputConnection connection = service.getCurrentInputConnection();
            ExtractedText text = connection == null ? null
                    : connection.getExtractedText(new ExtractedTextRequest(), 0);
            if (info == null || text == null || text.text == null
                    || text.selectionStart < 0 || text.selectionEnd < 0
                    || text.selectionStart > text.text.length() || text.selectionEnd > text.text.length()
                    || text.selectionStart == text.selectionEnd) {
                GLog.w("IA confirmar: no hay selección de origen válida");
                return;
            }
            active = new EditCommit(model, info, text);
            GLog.w("IA confirmar: origen capturado, selección=" + active.start + ".." + active.end);
        } catch (Throwable e) {
            GLog.w("IA confirmar: captura fallida " + e);
        }
    }

    static void attach(EditText editor) {
        if (active != null && active.result == null) active.editor = new WeakReference<>(editor);
    }

    static void rewritten(EditText editor) {
        if (active != null && active.editor != null && active.editor.get() == editor)
            active.rewritten = true;
    }

    public static void finish(int action) {
        EditCommit edit = active;
        if (edit == null) return;
        EditText editor = edit.editor == null ? null : edit.editor.get();
        if (action != 2 || !edit.rewritten || editor == null) {
            active = null;
            return;
        }
        edit.result = editor.getText().toString();
        edit.deadline = SystemClock.uptimeMillis() + 10000;
        GLog.w("IA confirmar: Realizado, pendiente=" + edit.result.length());
    }

    static boolean matches(String expected, int expectedOffset, CharSequence actual, int actualOffset) {
        return actual != null && expectedOffset == actualOffset && expected.contentEquals(actual);
    }

    static void onInput(InputMethodService service, EditorInfo info) {
        EditCommit edit = active;
        if (edit == null || edit.result == null || info == null || info.inputType == 0) return;
        if (SystemClock.uptimeMillis() > edit.deadline) { active = null; return; }
        if (!edit.packageName.equals(info.packageName) || edit.fieldId != info.fieldId) return;
        new Handler(Looper.getMainLooper()).postDelayed(() -> edit.commit(service), 250);
    }

    private void commit(InputMethodService service) {
        if (active != this) return;
        if (SystemClock.uptimeMillis() > deadline) { active = null; return; }
        try {
            EditorInfo info = service.getCurrentInputEditorInfo();
            if (info == null || info.inputType == 0 || !packageName.equals(info.packageName)
                    || fieldId != info.fieldId) return;
            InputConnection connection = service.getCurrentInputConnection();
            ExtractedText text = connection == null ? null
                    : connection.getExtractedText(new ExtractedTextRequest(), 0);
            if (text == null) return;
            active = null; // Consume once, including failures and subsequent onStartInput callbacks.
            if (!matches(original, offset, text.text, text.startOffset)) {
                GLog.w("IA confirmar: el campo cambió, edición conservada en Samsung");
                return;
            }
            connection.beginBatchEdit();
            boolean committed = false;
            try {
                if (connection.setSelection(offset + start, offset + end))
                    committed = connection.commitText(result, 1);
                if (!committed)
                    connection.setSelection(text.startOffset + text.selectionStart,
                            text.startOffset + text.selectionEnd);
            } finally {
                connection.endBatchEdit();
            }
            GLog.w("IA confirmar: sustituido=" + committed + ", caracteres=" + result.length());
            Object owner = model.get();
            if (committed && owner != null)
                owner.getClass().getMethod("requestSwitchToDefaultBoard").invoke(owner);
        } catch (Throwable e) {
            active = null;
            GLog.w("IA confirmar: sustitución fallida " + e);
        }
    }
}
