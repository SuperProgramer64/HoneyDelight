package gbport;

import java.util.List;

// Decisión de autocorrección sobre los candidatos que da el motor de Gboard decodificando CON el contexto
// de la frase (Delight.suggestEchoRaw). Sin dependencias de Samsung, para probarla en el arnés.
//  - Lo tecleado no es palabra (el motor no lo conoce o lo puntúa como desconocido): se aplica el mejor
//    candidato del motor si gana con claridad (erroes->errores, dicf->dice, teh->the, limites->límites).
//  - Lo tecleado es palabra válida: solo se cambia por la misma palabra con otra tilde o mayúscula, y solo si
//    el motor la prefiere con este contexto (hola como->cómo, me gusta como->como, yo supero / el equipo superó).
public final class Decide {
    // Por debajo de esto, lo tecleado no es una palabra (el motor la penaliza como desconocida): limites -31.9,
    // cancion -29.5, sabado -29.3, pais -22.0. Las palabras válidas quedan por encima de ~-17 (supero -16.4).
    static final double OOV_LITERAL = -20.0;
    // Ventaja mínima del candidato sobre una no-palabra (kukuruzo -34.5 frente a Kutuzov -38.0: se queda).
    static final double OOV_MARGIN = 3.0;
    // Si el motor no puntúa lo tecleado (desconocido del todo), el candidato debe ser una palabra corriente:
    // teh->the -10.5, wnt->want -10.7, iphone->iPhone -8.4; jerga sin parecido queda muy por debajo.
    static final double UNKNOWN_BEST_MIN = -15.0;
    // Coste espacial de una tecla bien pulsada. Las variantes con ñ se puntúan como si la ñ no fuera un fallo
    // de tecla (en Gboard sale con pulsación larga de la n, igual que las tildes): extraño sp -12.9 -> -1.0.
    static final double CLEAN_SPATIAL = -1.0;
    // Ventaja mínima (log-probabilidad) para cambiar una palabra válida por otra tilde: el motor debe preferirla
    // claramente con este contexto. Hola, como -0.1 (se queda); la casa está +2.4; el equipo superó +1.2.
    static final double ACCENT_MARGIN = 1.0;
    // Palabra rara pero conocida (bein -17.0 "Estás bein") a un fallo de tecla de una palabra corriente (bien -10.2):
    // se corrige si la corriente gana por mucho. Las palabras raras de verdad (trabas, menospreciarme) no tienen una
    // vecina a un fallo que gane por tanto.
    static final double RARE_LITERAL = -14.0;
    static final double RARE_MARGIN = 5.0;

    // Resultado: candidato a aplicar (null = dejar lo tecleado), quién decidió y la puntuación de lo tecleado.
    public static final class Result {
        public final Delight.Cand cand; public final String why; public final double litScore;
        Result(Delight.Cand cand, String why, double litScore) { this.cand = cand; this.why = why; this.litScore = litScore; }
    }

    private Decide() {}

    // Normaliza una palabra ignorando mayúsculas, tildes y la virgulilla de la ñ (espana -> España).
    static String norm(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = Delight.base(Character.toLowerCase(s.charAt(i)));
            b.append(c == 'ñ' ? 'n' : c);
        }
        return b.toString();
    }

    // Puntuación de una variante de la misma palabra. Solo se perdona el coste espacial de la ñ (en el teclado es una
    // tecla aparte: extraño sp -12.9); cualquier otro coste espacial alto es el motor marcando una forma que no conoce
    // (séqué sp -10.6, tellegué sp -11.0) y se cobra entero.
    static double fair(Delight.Cand c, String typed) {
        if (Float.isNaN(c.lm) || Float.isNaN(c.spatial)) return c.score;
        boolean addsEnye = c.text.toLowerCase().indexOf('ñ') >= 0 && typed.toLowerCase().indexOf('ñ') < 0;
        return addsEnye ? c.lm + Math.max(c.spatial, CLEAN_SPATIAL) : c.score;
    }

    // Interrogativas justo tras ¿ o ¡ (y "¿por que"): en español van con tilde (¿qué, ¿cómo, ¿cuándo). El motor a veces
    // deja "que" tras "¿" (CTX ¿|que -> que). Devuelve la forma con tilde o null.
    static final String[][] INTERROG = {{"que", "qué"}, {"como", "cómo"}, {"donde", "dónde"}, {"adonde", "adónde"},
            {"cuando", "cuándo"}, {"quien", "quién"}, {"quienes", "quiénes"}, {"cual", "cuál"}, {"cuales", "cuáles"},
            {"cuanto", "cuánto"}, {"cuanta", "cuánta"}, {"cuantos", "cuántos"}, {"cuantas", "cuántas"}};

    public static String interrogative(String before, String word) {
        if (before == null || word == null || word.isEmpty()) return null;
        String b = before;
        boolean open = b.endsWith("¿") || b.endsWith("¡");
        String lb = b.toLowerCase(java.util.Locale.ROOT);
        boolean porQue = lb.endsWith("¿por ") && word.equalsIgnoreCase("que");
        if (!open && !porQue) return null;
        String lw = word.toLowerCase(java.util.Locale.ROOT);
        for (String[] p : INTERROG) {
            if (!p[0].equals(lw)) continue;
            String r = p[1];
            if (Character.isUpperCase(word.charAt(0))) r = Character.toUpperCase(r.charAt(0)) + r.substring(1);
            return r;
        }
        return null;
    }

    // Mezcla con la autocorrección del propio motor (lo que confirma al pulsar espacio, Delight.typeAndSpace): si el
    // motor cambia la palabra (no solo mayúsculas), manda él: es la lógica de Gboard. Si la deja, decide Decide (el motor
    // es prudente al empezar el texto: hpla, teh). Devuelve la palabra final.
    // Con 150 palabras reales coinciden el 97 %; el motor acierta al completar lo que no es palabra (realid -> realidad)
    // y falla cambiando palabras válidas (¡Avión -> Acción, ¿qué -> que). Por eso solo manda cuando lo tecleado no es
    // palabra y Decide no lo corrigió.
    public static boolean repeatedExpansion(String word, String candidate) {
        String typed = norm(word), value = norm(candidate);
        if (typed.length() < 3 || value.length() < typed.length() + 2 || candidate.contains(" ")) return false;
        int first = value.indexOf(typed);
        if (first >= 0 && value.indexOf(typed, first + typed.length()) >= 0) return true;
        String head = typed.substring(0, 2);
        if (!typed.startsWith(head + head) && value.startsWith(head + head)) return true;
        String tail = typed.substring(typed.length() - 2);
        return !typed.endsWith(tail + tail) && value.endsWith(tail + tail + tail);
    }

    public static boolean needsEngine(String word, Result dec) {
        boolean verify = dec.cand == null || !norm(dec.cand.text).startsWith(norm(word))
                || (!word.isEmpty() && Character.isLowerCase(word.charAt(0))
                    && Character.isUpperCase(dec.cand.text.charAt(0)));
        return verify && word.codePointCount(0, word.length()) >= 3
                && (Double.isNaN(dec.litScore) || dec.litScore < OOV_LITERAL) && !"aprendida".equals(dec.why);
    }

    public static String hybrid(String word, String engineCommit, Result dec) {
        String e = engineCommit == null ? "" : engineCommit.trim();
        String mine = dec.cand != null ? dec.cand.text : word;
        if (!needsEngine(word, dec)) return mine;
        if (e.isEmpty() || e.contains(" ") || e.equals(word) || repeatedExpansion(word, e)) return mine;
        if (dec.cand != null && !(norm(e).startsWith(norm(word)) && e.length() >= word.length() + 2))
            return mine;
        if (!word.isEmpty() && Character.isLowerCase(word.charAt(0)) && !innerUpper(e))
            e = Character.toLowerCase(e.charAt(0)) + e.substring(1);
        if (e.equalsIgnoreCase(word)) return mine;                 // solo mayúsculas (Teh): no
        return e;
    }

    public static boolean displayAlternative(String word, String correction, String candidate) {
        if (correction == null) return true;
        String trimmed = correction.trim();
        boolean separated = trimmed.contains(" ") && norm(trimmed.replace(" ", "")).equals(norm(word));
        // Si ya se aceptó el corte, las variantes sin ese límite no deben ocupar la barra.
        return !separated || candidate.contains(" ");
    }

    static boolean usableCandidate(Delight.Cand c, String word) {
        if (c == null || c.text == null || c.text.trim().isEmpty()
                || repeatedExpansion(word, c.text)) return false;
        return Float.isFinite(c.score) || (Float.isNaN(c.score)
                && (c.type == 3 || c.text.equals(word)));
    }

    // La barra ordena alternativas por puntuación y descarta bucles incluso si un modelo les da peso alto.
    public static List<Delight.Cand> rankCandidates(List<Delight.Cand> candidates, String word) {
        List<Delight.Cand> ranked = new java.util.ArrayList<>();
        for (Delight.Cand c : candidates) if (usableCandidate(c, word)) ranked.add(c);
        ranked.sort((a, b) -> Float.compare(
                Float.isNaN(b.score) ? Float.NEGATIVE_INFINITY : b.score,
                Float.isNaN(a.score) ? Float.NEGATIVE_INFINITY : a.score));
        return ranked;
    }

    // ¿A un fallo de tecla? Una sustitución, inserción, borrado o dos letras cambiadas de sitio (bein/bien).
    static boolean oneEdit(String a, String b) {
        int la = a.length(), lb = b.length();
        if (Math.abs(la - lb) > 1 || a.equals(b)) return false;
        int i = 0;
        while (i < Math.min(la, lb) && a.charAt(i) == b.charAt(i)) i++;
        if (la == lb) {
            if (a.substring(i + 1).equals(b.substring(i + 1))) return true;
            return i + 1 < la && a.charAt(i) == b.charAt(i + 1) && a.charAt(i + 1) == b.charAt(i) && a.substring(i + 2).equals(b.substring(i + 2));
        }
        return la > lb ? a.substring(i + 1).equals(b.substring(i)) : b.substring(i + 1).equals(a.substring(i));
    }

    static boolean innerUpper(String s) {
        for (int i = 1; i < s.length(); i++) if (Character.isUpperCase(s.charAt(i))) return true;
        return false;
    }

    // learnedWord: Samsung ya memorizó lo tecleado (apodos del usuario).
    public static Result decide(List<Delight.Cand> cs, String word, boolean learnedWord) {
        String nw = norm(word);
        double lit = Double.NaN;
        Delight.Cand best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Delight.Cand c : cs) {
            if (!usableCandidate(c, word)) continue;
            if (c.text.equals(word)) {
                if (!Float.isNaN(c.score) && (Double.isNaN(lit) || c.score > lit)) lit = c.score;
                continue;
            }
            // Completar una letra que falta al final (hol -> hola, pizz -> pizza): Gboard lo hace con lo que no es palabra.
            boolean oneMore = c.type == 13 && c.text.length() == word.length() + 1
                    && norm(c.text).startsWith(nw);     // también con tilde (Aracnid -> Arácnido)
            if ((c.type != 0 && !oneMore) || Float.isNaN(c.score)) continue;
            // Solo mayúsculas sobre algo que no es palabra (Holacomo, Holaa) no es una corrección; las marcas sí (iPhone).
            if (c.text.equalsIgnoreCase(word) && !innerUpper(c.text)) continue;
            double s = norm(c.text).equals(nw) ? fair(c, word) : c.score;     // misma palabra: no se cobra tilde/ñ
            if (s > bestScore) { bestScore = s; best = c; }
        }
        // Mayúscula inicial que el motor añade por costumbre (Hola a mitad de frase): si lo tecleado iba en minúscula y
        // el motor también ofrece la forma en minúscula, se usa esa (las mayúsculas de inicio de frase son de Samsung).
        if (best != null && !word.isEmpty() && Character.isLowerCase(word.charAt(0)) && Character.isUpperCase(best.text.charAt(0))
                && !innerUpper(best.text)) {
            String low = Character.toLowerCase(best.text.charAt(0)) + best.text.substring(1);
            for (Delight.Cand c : cs) if (c.text.equals(low) && !Float.isNaN(c.score)) { best = c; break; }
        }
        // Palabra que Samsung memorizó (apodos): no se cambia, salvo para ponerle la tilde/ñ que el motor prefiere
        // (Samsung guardó "pelicula" y "cual" sin tilde y en la app de Google ya no se corregían, 17:03).
        if (learnedWord && !(best != null && norm(best.text).equals(nw) && !best.text.equalsIgnoreCase(word)))
            return new Result(null, "aprendida", lit);
        // Una letra suelta no se autocorrige (t -> y al pulsar espacio): Gboard tampoco.
        if (word.codePointCount(0, word.length()) < 2) return new Result(null, "una-letra", lit);
        if (best == null) return new Result(null, "-", lit);
        if (Double.isNaN(lit)) {
            // La misma palabra con su tilde/ñ siempre (martir -> mártir -17.7, cancion -> canción): no es jerga.
            boolean sameWord = norm(best.text).equals(nw) && !best.text.equalsIgnoreCase(word);
            return bestScore > UNKNOWN_BEST_MIN || sameWord ? new Result(best, "motor(desconocida)", lit) : new Result(null, "-", lit);
        }
        if (lit < OOV_LITERAL) {
            return bestScore - lit >= OOV_MARGIN ? new Result(best, "motor(no-palabra)", lit) : new Result(null, "-", lit);
        }
        // Desde 3 letras: las de 2 son casi siempre jerga a propósito (ta, pa, xq).
        if (word.length() >= 3 && lit < RARE_LITERAL && bestScore - lit >= RARE_MARGIN && !norm(best.text).equals(nw)
                && oneEdit(norm(best.text), nw)) {
            return new Result(best, "motor(rara)", lit);
        }
        // Solo tildes/ñ: las mayúsculas de inicio de frase las pone Samsung (evita El/Yo a mitad de frase).
        if (norm(best.text).equals(nw) && !best.text.equalsIgnoreCase(word) && bestScore - lit >= ACCENT_MARGIN) {
            return new Result(best, "contexto", lit);
        }
        // Mayúsculas propias de marca en mitad de la palabra (iPhone, YouTube): nunca es solo inicio de frase.
        // Siglas en mayúsculas no (ps -> PS, xd -> XD, usa -> USA): solo marcas con minúsculas dentro.
        if (best.text.equalsIgnoreCase(word) && innerUpper(best.text) && !best.text.equals(best.text.toUpperCase())
                && !innerUpper(word) && bestScore - lit >= ACCENT_MARGIN) {
            return new Result(best, "marca", lit);
        }
        return new Result(null, "-", lit);
    }
}
