package gbport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Palabras pegadas (holacomo -> hola como) y pegadas a un signo (hola,como -> hola, como), decididas con las
// puntuaciones del motor. Incremental: mientras se teclea, cada prefijo deja su puntuación (la de la decodificación
// normal de esa tecla) y su mejor corte; con la palabra completa solo se decodifican las partes derechas de los
// pocos prefijos que son palabra. Así no hace falta re-decodificar prefijos (el motor no los puntúa por tecla).
public final class Split {
    // Cada parte debe ser una palabra corriente para el motor (como UNKNOWN_BEST_MIN de Decide).
    static final double PART_MIN = -15.0;
    // Ventaja mínima del corte sobre la mejor palabra única (log-probabilidad conjunta).
    static final double MARGIN = 1.0;
    // Coste por cada palabra del corte (penalización de inserción de palabra): evita trocear en piezas cortas
    // (holacomo -> "ho la como", skibidi -> "ski bi di").
    static final double PART_PENALTY = 2.0;
    // Se intenta cortar cuando lo tecleado no es una palabra corriente (lovethis -17.3, gustamucho -38.9).
    public static final double TRY_BELOW = -15.0;
    // Partes derechas a decodificar por tecla como máximo (las izquierdas con mejor puntuación).
    static final int MAX_RIGHTS = 4;
    // Palabras de una letra que pueden ir sueltas a la izquierda.
    static final String SINGLES = "ayoeuiAYOEUI";

    public static final class Result {
        public final String text; public final double score;
        Result(String text, double score) { this.text = text; this.score = score; }
    }

    private Split() {}

    // Memoria de la palabra en curso (por contexto): forma y puntuación de cada prefijo, su mejor corte y las
    // partes derechas ya puntuadas.
    static String ctxKey = "", afterKey = "";
    static final Map<String, Object[]> lit = new HashMap<>();        // prefijo -> {forma, puntuación}
    static final Map<String, Result> splits = new HashMap<>();      // prefijo -> mejor corte (o ausente)
    static final Map<String, Object[]> rights = new HashMap<>();     // "izq|der" -> {forma, puntuación}

    static synchronized void clearCache() {
        ctxKey = "";
        afterKey = "";
        lit.clear(); splits.clear(); rights.clear();
    }

    static synchronized void reset(String before) {
        if (before.equals(ctxKey) && lit.size() < 256 && rights.size() < 512) return;
        ctxKey = before;
        afterKey = "";
        lit.clear(); splits.clear(); rights.clear();
    }

    // Mejor forma de la palabra (exacta, con otra tilde/ñ o con mayúscula inicial normalizada) entre los candidatos. null si no.
    static Object[] bestForm(List<Delight.Cand> cs, String w) {
        String nw = Decide.norm(w);
        Object[] best = null;
        for (Delight.Cand c : cs) {
            if (Float.isNaN(c.score) || c.type == 3) continue;
            boolean exact = c.text.equals(w);
            boolean caseOnly = !exact && c.text.equalsIgnoreCase(w) && !Decide.innerUpper(c.text);
            boolean accent = !exact && !c.text.equalsIgnoreCase(w) && Decide.norm(c.text).equals(nw);
            if (!exact && !caseOnly && !accent) continue;
            double sc = exact || caseOnly ? c.score : Decide.fair(c, w);
            if (best == null || sc > (Double) best[1]) best = new Object[]{caseOnly ? w : c.text, sc};
        }
        return best;
    }

    static Object[] rightForm(List<Delight.Cand> candidates, String word) {
        Object[] exact = bestForm(candidates, word);
        if (exact != null && (Double) exact[1] >= PART_MIN) return exact;
        Decide.Result correction = Decide.decide(candidates, word, false);
        Delight.Cand candidate = correction.cand;
        if (word.length() >= 3 && candidate != null && Float.isFinite(candidate.score)
                && candidate.score >= PART_MIN && !Decide.innerUpper(word)
                && Decide.oneEdit(Decide.norm(word), Decide.norm(candidate.text))
                && !candidate.text.contains(" ") && !Decide.repeatedExpansion(word, candidate.text)) {
            String text = candidate.text;
            if (Character.isLowerCase(word.charAt(0)) && !Decide.innerUpper(text))
                text = Character.toLowerCase(text.charAt(0)) + text.substring(1);
            return new Object[]{text, (double)candidate.score};
        }
        return exact;
    }

    // Apunta la decodificación normal de la palabra en curso (llamar en cada tecla con sus candidatos).
    public static synchronized void note(String before, String w, List<Delight.Cand> cs) {
        reset(before);
        Object[] f = bestForm(cs, w);
        if (f != null) lit.put(w, f); else lit.remove(w);
    }

    static boolean leftOk(String p) {
        return p.length() >= 2 || (p.length() == 1 && SINGLES.indexOf(p.charAt(0)) >= 0);
    }

    // Mejor corte de w con lo apuntado de sus prefijos; decodifica como mucho MAX_RIGHTS partes derechas.
    // Guarda el resultado para los prefijos siguientes (cortes en 3 palabras).
    public static synchronized Result update(String before, String after, String w, long deadlineNanos) {
        reset(before);
        if (!after.equals(afterKey)) {
            Object[] current = lit.get(w);
            lit.clear(); splits.clear(); rights.clear();
            if (current != null) lit.put(w, current);
            afterKey = after;
        }
        if (w.length() < 3 || w.length() > 24) return null;
        String common = CommonBoundaries.find(before, after, w);
        if (common != null) {
            Result result = new Result(common, -4.0);
            splits.put(w, result);
            return result;
        }
        Object[] whole = lit.get(w);
        if (whole != null && w.equals(whole[0]) && (Double) whole[1] >= TRY_BELOW) return null;
        // Recover missing prefixes when Samsung delivered several letters at once.
        // Start near the middle, then expand. Each invocation has a fixed work budget.
        int probes = 0;
        int maxProbes = w.length() <= 5 ? 1 : 3;
        for (int distance = 0; distance < w.length() && probes < maxProbes; distance++) {
            int middle = w.length() <= 5 ? 1 : w.length() / 2;
            int i = middle + (distance % 2 == 0 ? -(distance / 2) : (distance + 1) / 2);
            if (i < 1 || i > w.length() - 2 || (w.length() <= 5 && i != 1)) continue;
            String prefix = w.substring(0, i);
            if (!leftOk(prefix) || lit.containsKey(prefix) || rights.containsKey("prefix:" + prefix)) continue;
            if (System.nanoTime() > deadlineNanos) break;
            Object[] form = bestForm(Delight.parseCands(Delight.suggestEchoRaw(before, after, prefix, 8)), prefix);
            rights.put("prefix:" + prefix, form);
            if (form != null) lit.put(prefix, form);
            probes++;
        }
        // Izquierdas posibles: el prefijo como palabra, o el mejor corte del prefijo.
        List<Object[]> opts = new ArrayList<>();       // {i, texto izquierdo, puntuación con penalizaciones}
        for (int i = 1; i <= w.length() - 2; i++) {
            String p = w.substring(0, i);
            Object[] f = lit.get(p);
            if (f != null && leftOk(p) && (Double) f[1] >= PART_MIN) opts.add(new Object[]{i, f[0], (Double) f[1] - PART_PENALTY});
            Result sp = splits.get(p);
            if (sp != null) opts.add(new Object[]{i, sp.text, sp.score});
        }
        opts.sort((a, b) -> {
            boolean firstPhrase = ((String)a[1]).contains(" "), secondPhrase = ((String)b[1]).contains(" ");
            if (firstPhrase != secondPhrase) return firstPhrase ? 1 : -1;
            // Priorizar una palabra izquierda completa frente a prefijos breves muy frecuentes.
            int length = Integer.compare((Integer)b[0], (Integer)a[0]);
            return length != 0 ? length : Double.compare((Double)b[2], (Double)a[2]);
        });
        Result best = splits.get(w); // Un plazo agotado no invalida un corte ya comprobado con este contexto.
        int done = 0;
        for (Object[] o : opts) {
            if (done >= MAX_RIGHTS || System.nanoTime() > deadlineNanos) break;
            int i = (Integer) o[0];
            String left = (String) o[1], right = w.substring(i);
            if (right.length() < 2) continue;
            String key = left + "|" + right;
            Object[] rf = rights.get(key);
            if (rf == null && !rights.containsKey(key)) {
                rf = rightForm(Delight.parseCands(Delight.suggestEchoRaw(before + left + " ", after, right, 8)), right);
                rights.put(key, rf);
                done++;
            }
            if (rf == null || (Double) rf[1] < PART_MIN) continue;
            Result cand = new Result(left + " " + rf[0], (Double) o[2] + (Double) rf[1] - PART_PENALTY);
            if (best == null || cand.score > best.score) best = cand;
        }
        if (best != null) splits.put(w, best); else splits.remove(w);
        return best;
    }

    // ¿Aplicar el corte? single: puntuación de la mejor palabra única (corrección o lo tecleado), NaN si ninguna.
    public static boolean wins(Result r, double litScore, double single) {
        if (r == null) return false;
        if (!Double.isNaN(litScore) && litScore >= TRY_BELOW) return false;     // lo tecleado ya es palabra corriente
        double ref = (Double.isNaN(single) ? Decide.OOV_LITERAL - MARGIN : single) - PART_PENALTY;
        return r.score >= ref + MARGIN;
    }

    // Palabra pegada al signo anterior ("hola,como"): true si hay que anteponer un espacio.
    // Solo tras letra + signo de cierre (, ; : ! ? .) sin espacio; no en URLs, correos, cifras, abreviaturas
    // ni emoticonos (:v, :D, xD).
    public static boolean needsSpace(String before, String word) {
        if (before == null || before.length() < 2 || word.isEmpty()) return false;
        char p = before.charAt(before.length() - 1), l = before.charAt(before.length() - 2);
        if (",;:!?.".indexOf(p) < 0 || !Character.isLetter(l)) return false;
        if ((p == ':' || p == ';') && word.length() <= 2) return false;   // :v :D ;)
        int sp = Math.max(before.lastIndexOf(' '), before.lastIndexOf('\n'));
        String token = before.substring(sp + 1, before.length() - 1);
        if (token.contains("@") || token.contains("/") || token.contains("www") || token.contains("http")) return false;
        if (p == '.') {
            // Abreviaturas y dominios: "e.g.", "a.m.", "google.com", "sr.".
            if (token.contains(".") || token.length() <= 3) return false;
            String w = word.toLowerCase(java.util.Locale.ROOT);
            if (w.equals("com") || w.equals("net") || w.equals("org") || w.equals("es") || w.equals("mx") || w.equals("io")
                    || w.equals("co") || w.equals("ar") || w.equals("app") || w.equals("dev")) return false;
        }
        return true;
    }
}
