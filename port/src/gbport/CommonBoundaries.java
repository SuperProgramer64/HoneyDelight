package gbport;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Atajos de límites de expresiones frecuentes. No se aprende de mensajes ni usa la red. */
final class CommonBoundaries {
    private static final Map<String, String> PHRASES = new HashMap<>();
    static {
        for (String phrase : new String[]{
                "buenos días", "buenas tardes", "buenas noches", "hasta mañana", "hasta luego",
                "hasta pronto", "por favor", "muchas gracias", "de nada", "nos vemos", "te quiero",
                "te amo", "todo bien", "qué tal", "qué pasa", "lo siento"})
            PHRASES.put(Decide.norm(phrase.replace(" ", "")), phrase);
    }

    private CommonBoundaries() {}

    static String find(String before, String after, String word) {
        if (word.length() < 5) return null;
        String lower = word.toLowerCase(Locale.ROOT), upper = word.toUpperCase(Locale.ROOT);
        boolean allCaps = word.equals(upper);
        if (!allCaps && !word.substring(1).equals(lower.substring(1))) return null;
        for (int i = 0; i < word.length(); i++) if (!Character.isLetter(word.charAt(i))) return null;
        int boundary = Math.max(before.lastIndexOf(' '), Math.max(before.lastIndexOf('\n'), before.lastIndexOf('\t')));
        String token = before.substring(boundary + 1);
        if (token.indexOf('@') >= 0 || token.indexOf('/') >= 0 || token.indexOf('.') >= 0
                || token.indexOf('#') >= 0 || !after.isEmpty() && (after.charAt(0) == '.' || after.charAt(0) == '@')) return null;
        String phrase = PHRASES.get(Decide.norm(word));
        if (phrase == null) return null;
        if (allCaps) return phrase.toUpperCase(Locale.ROOT);
        if (Character.isUpperCase(word.charAt(0)))
            return Character.toUpperCase(phrase.charAt(0)) + phrase.substring(1);
        return phrase;
    }
}
