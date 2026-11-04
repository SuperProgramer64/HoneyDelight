package gbport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Orden de las palabras del deslizar. El motor puede proponer formas alargadas que vienen del historial (queeeee,
// pasooooo) cuando el trazo se para sobre una letra; Gboard no las da al deslizar. Se quitan, se usa su forma normal
// y, entre variantes con y sin tilde, gana la que el motor puntúa mejor con el contexto (¿que -> ¿qué).
// Sin dependencias de Samsung, para probarla en el arnés.
public final class Swipe {
    // Penalización de la forma normal sacada de una alargada (el motor no la puntuó).
    static final double COLLAPSE_PENALTY = 0.5;

    // Tartamudeo escrito (H-Hola): no se ofrece al deslizar.
    static final java.util.regex.Pattern STUTTER = java.util.regex.Pattern.compile("^(\\p{L}{1,3})-\\1", java.util.regex.Pattern.CASE_INSENSITIVE);

    private Swipe() {}

    static boolean elongated(String w) {
        for (int i = 2; i < w.length(); i++) {
            char c = Character.toLowerCase(w.charAt(i));
            if (Character.isLetter(c) && c == Character.toLowerCase(w.charAt(i - 1)) && c == Character.toLowerCase(w.charAt(i - 2))) return true;
        }
        return false;
    }

    // Tramos de 3+ letras iguales a 'keep' letras (queeeee -> que con 1, creeee -> cree con 2).
    static String collapse(String w, int keep) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < w.length(); ) {
            int j = i;
            while (j < w.length() && Character.toLowerCase(w.charAt(j)) == Character.toLowerCase(w.charAt(i))) j++;
            b.append(w, i, j - i >= 3 ? i + keep : j);
            i = j;
        }
        return b.toString();
    }

    // g: palabra que escribe el motor (puede estar vacía); cs: candidatos. Devuelve las palabras en orden.
    public static List<String> rank(String g, List<Delight.Cand> cs) { return rank(g, cs, ""); }

    public static List<String> rank(String g, List<Delight.Cand> cs, String before) {
        Map<String, Double> sc = new LinkedHashMap<>();      // en minúsculas -> {mejor puntuación}
        Map<String, String> form = new LinkedHashMap<>();
        double gScore = Double.NaN;
        for (Delight.Cand c : cs) {
            if (Float.isNaN(c.score) || c.type == 2 || STUTTER.matcher(c.text).find()) continue;
            if (c.text.equals(g)) gScore = c.score;
            add(sc, form, c.text, c.score);
        }
        if (!g.isEmpty() && Double.isNaN(gScore)) {
            double top = Double.NEGATIVE_INFINITY;
            for (double v : sc.values()) top = Math.max(top, v);
            gScore = sc.isEmpty() ? 0 : top;
            add(sc, form, g, gScore);
        }
        // Alargadas fuera; su forma normal entra con la puntuación de la alargada (si no estaba ya, mejor puntuada).
        for (String k : new ArrayList<>(sc.keySet())) {
            if (!elongated(k)) continue;
            double v = sc.remove(k);
            String f = form.remove(k);
            String c1 = collapse(f, 1), c2 = collapse(f, 2);
            String pick = sc.containsKey(c2.toLowerCase(Locale.ROOT)) && !sc.containsKey(c1.toLowerCase(Locale.ROOT)) ? c2 : c1;
            add(sc, form, pick, v - COLLAPSE_PENALTY);
        }
        // Lo que escribe el motor va primero salvo que otra variante de la misma palabra (tilde) puntúe mejor.
        String first = null;
        String gk = collapseKey(g);
        if (!gk.isEmpty() && sc.containsKey(gk)) {
            first = gk;
            String ng = Decide.norm(gk);
            for (String k : sc.keySet()) if (!k.equals(first) && Decide.norm(k).equals(ng) && sc.get(k) > sc.get(first)) first = k;
        }
        // La forma que escribe el motor (Hola con mayúscula de inicio) se respeta.
        if (first != null && first.equals(gk) && !elongated(g)) form.put(gk, g);
        // Tras ¿ o ¡ la interrogativa va con tilde.
        if (first != null) {
            String q = Decide.interrogative(before, form.get(first));
            if (q != null) { String qk = q.toLowerCase(Locale.ROOT); if (!sc.containsKey(qk)) sc.put(qk, sc.get(first)); form.put(qk, q); first = qk; }
        }
        List<String> keys = new ArrayList<>(sc.keySet());
        keys.sort((a, b) -> Double.compare(sc.get(b), sc.get(a)));
        if (first != null) { keys.remove(first); keys.add(0, first); }
        List<String> out = new ArrayList<>();
        for (String k : keys) out.add(form.get(k));
        return out;
    }

    static String collapseKey(String g) {
        if (g == null || g.isEmpty()) return "";
        return (elongated(g) ? collapse(g, 1) : g).toLowerCase(Locale.ROOT);
    }

    static void add(Map<String, Double> sc, Map<String, String> form, String w, double v) {
        String k = w.toLowerCase(Locale.ROOT);
        Double o = sc.get(k);
        if (o == null || v > o) { sc.put(k, v); form.put(k, w); }
    }
}
