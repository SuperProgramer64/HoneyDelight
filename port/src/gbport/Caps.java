package gbport;

import android.text.TextUtils;

// Mayúscula automática tras signos de apertura. Samsung decide el shift con TextUtils.getCapsMode
// (V7.a.getCursorCapsMode), que solo salta comillas rectas y paréntesis. Como Gboard:
//  - tras ¿ o ¡ siempre mayúscula, también a mitad de frase ("Hola ¿Qué tal?", "Oye ¡Mira!");
//  - tras « “ ‘ ‹ „ " ' ( [ { solo si ahí empieza frase ("Ya. «Buenas»"; "dijo «hola»" sigue en minúscula).
// Sin dependencias de Samsung, para probarla en el arnés.
public final class Caps {
    static final String OPENERS = "¿¡«“‘‹„\"'([{";
    static volatile int logged;

    private Caps() {}

    public static int caps(CharSequence cs, int off, int req) {
        int r = TextUtils.getCapsMode(cs, off, req);
        if (r != 0) return r;
        try { return openerCaps(cs, off, req); } catch (Throwable e) { return r; }
    }

    // Si justo antes del cursor hay signos de apertura, se decide como Gboard.
    public static int openerCaps(CharSequence cs, int off, int req) {
        if ((req & (TextUtils.CAP_MODE_SENTENCES | TextUtils.CAP_MODE_WORDS)) == 0 || off <= 0 || off > cs.length()) return 0;
        int i = off;
        boolean inverted = false;
        while (i > 0 && OPENERS.indexOf(cs.charAt(i - 1)) >= 0) {
            char c = cs.charAt(i - 1);
            if (c == '¿' || c == '¡') inverted = true;
            i--;
        }
        if (i == off) return 0;
        int r = inverted ? TextUtils.CAP_MODE_SENTENCES : TextUtils.getCapsMode(cs, i, req);
        if (r != 0 && logged < 20) {
            logged++;
            try { GLog.w("mayúscula tras '" + cs.subSequence(i, off) + "'"); } catch (Throwable e) { /* arnés */ }
        }
        return r;
    }
}
