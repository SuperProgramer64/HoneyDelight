package gbport;

import com.google.android.keyboard.client.delight5.Decoder;
import com.google.android.keyboard.client.delight5.JniUtil;
import java.io.File;
import java.nio.charset.StandardCharsets;

// Envoltorio del decodificador Delight5 de Gboard (campos según Gboard 18.5.6).
public final class Delight {
    private static boolean loaded;

    public static synchronized void loadLibrary(String soPath, String crashDir) {
        if (loaded) return;
        if (soPath.indexOf('/') >= 0) System.load(soPath); else System.loadLibrary(soPath);
        new File(crashDir).mkdirs();
        JniUtil.init(crashDir.getBytes(StandardCharsets.UTF_8), false, false);
        loaded = true;
    }

    // axhs: parámetros por idioma (valores por defecto de los recursos de Gboard).
    static Pb langParams(String lang, String country) {
        Pb p = Pb.n().s(1, lang);
        if (country != null && !country.isEmpty()) p.s(2, country);
        p.s(3, "!?;,:\"()'-/@_")
         .s(4, "([{&«„“")
         .s(5, "।॥.,;:!?‽)]}%&»”")
         .s(6, "")
         .s(7, "،؛؟।॥ \t\n\r ​()[]{}*&<>+=|.,;:!?‽/\"¡¿、：；（）〜…。，？！")
         .s(8, "'-")
         .s(9, "#@&._*")
         .s(10, " ")
         .s(11, "؟।॥.?‽!\n\r。？！")
         .s(17, "¡¿\n\r")
         .b(12, false)
         .b(21, false)
         .b(16, true)
         .s(22, ". ")
         .s(23, ".")
         .s(27, ",-:;")
         .s(29, ",:;");
        if ("en".equals(lang)) for (String a : "aka inc vs esp etc".split(" ")) p.s(13, a);
        return p;
    }

    public static void createDecoder(String[][] langs, String dataDir) {
        ctxVersion = 0;
        decodeState = 0;
        echoBefore = null;
        echoAfter = null;
        Pb params = Pb.n();
        for (String[] l : langs) params.m(1, langParams(l[0], l[1]));
        params.s(3, dataDir);
        params.m(11, Pb.n().b(1, true).i(2, 0));
        new File(dataDir).mkdirs();
        Decoder.createOrResetDecoderNative(Pb.n().m(1, params).i(2, System.currentTimeMillis()).bytes());
    }

    // axjc{axja{...}, tiempo}: parámetros de sesión, como los pone LatinIme de Gboard al abrir un campo.
    // axja: 1 sugerencias activas, 57 teclado en pantalla, 39 (siempre true en Gboard),
    // 55 axje{1 next_word_prediction (preferencia "Sugerencias de la siguiente palabra"), 2}.
    // Máscara experimental de qué campos de axja se envían: 1 = campo 1, 2 = campo 57, 4 = campo 39.
    public static int rtMask = 7;
    public static float axiu10 = 2.0f;

    public static void setRuntimeParams(boolean nextWord) {
        setRuntimeParams(nextWord, null, false);
    }

    // Igual que LatinIme.x(EditorInfo) de Gboard 18.5.6, con los metadatos del campo (app y si es búsqueda).
    public static volatile boolean emoji = true;

    public static void setRuntimeParams(boolean nextWord, String pkg, boolean search) {
        setRuntimeParams(nextWord, pkg, search, false);
    }

    // email: campo de correo -> como Gboard (LatinIme.x), 25 awtj{4 awth{1 awtl{1 EMAIL(20), 2 peso 1.0, 3 0.0}}} y 29 = 2.
    public static void setRuntimeParams(boolean nextWord, String pkg, boolean search, boolean email) {
        Pb rp = Pb.n();
        if ((rtMask & 1) != 0) rp.b(1, true);
        if ((rtMask & 2) != 0) rp.b(57, true);
        if ((rtMask & 4) != 0) rp.b(39, true);
        // 4 block_offensive_words: apagado (sin censura). 6 enable_auto_capitalization, 5 doble espacio = punto y
        // 35 espacio tras puntuación: apagados, ya los hace Samsung.
        rp.b(4, false).b(6, false).b(5, false).b(35, false);
        rp.m(55, Pb.n().b(1, nextWord).b(2, true));
        // Fijos de Gboard: 26/14/38 siempre true, 22 = 20, 19 = 2; poda del contexto alrededor del cursor
        // (18 máx. caracteres a leer 40, 16 caracteres que disparan la poda 120, 17 profundidad 15).
        rp.b(26, true).b(14, true).b(38, true).i(22, 20).i(19, 2).i(18, 40).i(16, 120).i(17, 15);
        // 56 modo de dispositivo: DM_VIRTUAL_KEYBOARD.
        rp.i(56, 1);
        // 9 axiu: 9 true, 10 2.0, 25 user_history_learning_strategies = 1 (aprende de lo que escribes).
        // 6 = pref_key_enable_emoji_suggestion (sugerencias de emoji).
        rp.m(9, Pb.n().b(6, emoji).b(9, true).f(10, axiu10).i(25, 1));
        // 32 axiq: hora, desfase horario, app del campo, "search" si es un buscador.
        long now = System.currentTimeMillis();
        Pb q = Pb.n().i(1, now).i(2, java.util.TimeZone.getDefault().getOffset(now));
        if (pkg != null && !pkg.isEmpty()) q.s(3, pkg);
        if (search) q.s(4, "search");
        rp.m(32, q);
        if (email) rp.m(25, Pb.n().m(4, Pb.n().m(1, Pb.n().i(1, LM_EMAIL).f(2, 1.0f).f(3, 0.0f)))).i(29, 2);
        else rp.m(25, Pb.n()).i(29, 0);
        Decoder.setRuntimeParamsNative(Pb.n().m(1, rp).i(2, now).bytes());
    }

    // axjn{axjl{tipo, ruta, offset, largo, idioma, país}}
    public static boolean loadMainLm(String path, String lang, String country) {
        long len = new File(path).length();
        Pb d = Pb.n().i(1, 1).s(4, path).i(5, 0).i(6, len).s(7, lang).s(8, country);
        return Decoder.loadLanguageModelNative(Pb.n().m(1, d).bytes());
    }

    // Tipos de modelo (axjk): 2 contactos, 3 historial del usuario, 4 diccionario personal.
    public static final int LM_CONTACTS = 2, LM_USER_HISTORY = 3, LM_PERSONAL = 4, LM_BLOCKLIST = 7, LM_EMAIL = 20;

    // Modelos personales de Gboard (lzg.e / lzg.f): un idioma (7/8) o varios (13, historial bilingüe).
    public static boolean loadPersonalLm(int type, String path, String lang, String country, String... langs) {
        File f = new File(path);
        if (!f.exists()) return false;
        Pb d = Pb.n().i(1, type).s(4, path).i(5, 0).i(6, f.length());
        if (lang != null) d.s(7, lang);
        if (country != null) d.s(8, country);
        for (String l : langs) d.s(13, l);
        return Decoder.loadLanguageModelNative(Pb.n().m(1, d).bytes());
    }

    public static final int LM_EMOJI_ANNOTATION = 15;

    // Emoji de Gboard: modelo de anotaciones (tipo 15) por idioma.
    public static boolean loadEmojiLm(String path, String lang, String country) {
        return loadPersonalLm(LM_EMOJI_ANNOTATION, path, lang, country);
    }

    // Atajos de emoji (EmojiShortcutsLoader de Gboard): el archivo .shortcuts es axah{1 axaf{1 emoji, 2 [claves], 3 bool}};
    // se manda axfb{1 axll{1 [axlj{1 [clave], 2 [emoji], 3 [true], 4 [bool]}], 2 [idioma]}, 2 ms}.
    // files: pares {ruta, idioma}. Devuelve cuántas claves se cargaron.
    public static synchronized int loadEmojiShortcuts(String[][] files) {
        Pb ll = Pb.n();
        int total = 0;
        for (String[] fl : files) {
            File f = new File(fl[0]);
            if (!f.exists()) continue;
            try {
                byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
                int fileTotal = 0;
                Pb lj = Pb.n();
                java.util.Map<String, Integer> perKey = new java.util.HashMap<>();
                for (Pb.F af : Pb.all(Pb.parse(data), 1)) {
                    java.util.List<Pb.F> ff = Pb.parse(af.d);
                    Pb.F em = Pb.one(ff, 1), fl3 = Pb.one(ff, 3);
                    if (em == null) continue;
                    boolean flag = fl3 != null && fl3.v != 0;
                    for (Pb.F k : Pb.all(ff, 2)) {
                        String key = k.str().toLowerCase(java.util.Locale.ROOT);
                        int c = perKey.getOrDefault(key, 0);
                        if (c >= 5) continue;                       // como lxx.p: pocas por clave
                        perKey.put(key, c + 1);
                        lj.s(1, key).s(2, em.str()).b(3, true).b(4, flag);
                        fileTotal++;
                    }
                }
                ll.m(1, lj).s(2, fl[1]);
                total += fileTotal;
            } catch (RuntimeException | java.io.IOException e) {
                // Un recurso opcional dañado no debe impedir cargar los idiomas principales.
            }
        }
        if (total == 0) return 0;
        boolean ok = Decoder.loadEmojiShortcutMapNative(Pb.n().m(1, ll).i(2, System.currentTimeMillis()).bytes());
        return ok ? total : -total;
    }

    // Guarda en disco lo aprendido (historial del usuario), como lwt.E() de Gboard.
    public static synchronized boolean flushPersonal() {
        return Decoder.flushPersonalizedDataToDiskNative(System.currentTimeMillis());
    }

    // awxz{awxx{ancho tecla, alto tecla, ancho teclado, alto teclado, teclas[]}}
    public static void setLayout(float keyW, float keyH, float kbW, float kbH, int[] cps, float[] x, float[] y, float[] w, float[] h) {
        Pb lay = Pb.n().f(1, keyW).f(2, keyH).f(3, kbW).f(4, kbH);
        for (int k = 0; k < cps.length; k++) {
            lay.m(5, Pb.n().i(1, cps[k]).f(2, x[k]).f(3, y[k]).f(4, w[k]).f(5, h[k]));
        }
        Decoder.setKeyboardLayoutNative(Pb.n().m(1, lay).bytes());
    }

    // awzf.c (campo 2.1 de la respuesta) trae la versión de contexto del decodificador.
    static byte[] track(byte[] r) { return track(r, 2); }

    static byte[] track(byte[] r, int stateField) {
        Pb.F st = Pb.one(Pb.parse(r), stateField);
        if (st != null) {
            Pb.F v = Pb.one(Pb.parse(st.d), 1);
            if (v != null) ctxVersion = (int) v.v;
        }
        return r;
    }

    // abortComposing responde awyf{1 awzf}; decode/onKeyPress responden con awzf en el campo 2.
    static byte[] abortComposing(long id) {
        return track(Decoder.abortComposingNative(Pb.n().m(1, meta(id)).i(2, id).bytes()), 1);
    }

    // Candidato de Gboard: texto, tipo (1 = lo tecleado), puntuación total (campo 4 = 5 + 6), coste espacial
    // (campo 5) y probabilidad del modelo de idioma (campo 6). NaN si el motor no los da.
    // auto: el propio motor marca este candidato como su autocorrección (campo 3 = 1).
    public static final class Cand {
        public final String text; public final int type; public final float score, spatial, lm;
        public final boolean auto;
        Cand(String text, int type, float score, float spatial, float lm, boolean auto) {
            this.text = text; this.type = type; this.score = score; this.spatial = spatial; this.lm = lm;
            this.auto = auto;
        }
    }

    public static java.util.List<Cand> parseCands(byte[] r) {
        java.util.List<Cand> out = new java.util.ArrayList<>();
        Pb.F st = Pb.one(Pb.parse(r), 2);
        if (st == null) return out;
        Pb.F cs = Pb.one(Pb.parse(st.d), 3);
        if (cs == null) return out;
        for (Pb.F c : Pb.all(Pb.parse(cs.d), 1)) {
            java.util.List<Pb.F> f = Pb.parse(c.d);
            Pb.F t = Pb.one(f, 2), ty = Pb.one(f, 1), sc = Pb.one(f, 4), sp = Pb.one(f, 5), lm = Pb.one(f, 6);
            Pb.F au = Pb.one(f, 3);
            if (t == null || t.d.length == 0) continue;
            out.add(new Cand(t.str(), ty == null ? 0 : (int) ty.v, sc == null ? Float.NaN : sc.flt(),
                    sp == null ? Float.NaN : sp.flt(), lm == null ? Float.NaN : lm.flt(), au != null && au.v == 1));
        }
        return out;
    }

    // Teclado QWERTY español sintético (las coordenadas solo sirven al modelo espacial de Gboard).
    static final String[] ROWS = {"qwertyuiop", "asdfghjklñ", "zxcvbnm"};
    static final float KW = 108, KH = 160;
    static final float[] OFF = {0, 0, 162};

    static int row(char c) { for (int r = 0; r < 3; r++) if (ROWS[r].indexOf(c) >= 0) return r; return -1; }

    static char base(char c) {
        String from = "áàäâãéèëêíìïîóòöôõúùüûç", to = "aaaaaeeeeiiiiooooouuuuc";
        int i = from.indexOf(c);
        return i >= 0 ? to.charAt(i) : c;
    }

    public static void setQwertyLayout() {
        String all = ROWS[0] + ROWS[1] + ROWS[2];
        int n = all.length() + 1;
        int[] cps = new int[n]; float[] x = new float[n], y = new float[n], w = new float[n], h = new float[n];
        for (int i = 0; i < all.length(); i++) {
            char c = all.charAt(i); int r = row(c);
            cps[i] = c; x[i] = OFF[r] + KW * ROWS[r].indexOf(c) + KW / 2; y[i] = KH * r + KH / 2; w[i] = KW; h[i] = KH;
        }
        cps[n - 1] = ' '; x[n - 1] = 540; y[n - 1] = KH * 3.5f; w[n - 1] = 540; h[n - 1] = KH;
        setLayout(KW, KH, 1080, KH * 4, cps, x, y, w, h);
    }

    // Centro de una tecla en el marco del layout del motor ({x, y}); null si no está en el teclado.
    public static float[] center(char ch) {
        char k = base(Character.toLowerCase(ch));
        int r = row(k);
        if (r < 0) return null;
        return new float[]{OFF[r] + KW * ROWS[r].indexOf(k) + KW / 2, KH * r + KH / 2};
    }

    // true si la palabra se puede teclear en el layout (letras, acentos, apóstrofo).
    public static boolean typeable(String word) {
        for (int i = 0; i < word.length(); i++) {
            char c = Character.toLowerCase(word.charAt(i));
            if (row(base(c)) < 0 && c != '\'') return false;
        }
        return !word.isEmpty();
    }

    // Decodifica la palabra en curso con el texto previo como contexto.
    public static synchronized java.util.List<Cand> suggest(String before, String word, int n) {
        return suggest(before, "", word, n);
    }

    public static synchronized java.util.List<Cand> suggest(String before, String after, String word, int n) {
        long id = ++reqId;
        abortComposing(id);
        setContext(before, "", after, 0);
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            char k = base(Character.toLowerCase(c));
            int r = row(k);
            float x = r < 0 ? 540 : OFF[r] + KW * ROWS[r].indexOf(k) + KW / 2;
            float y = r < 0 ? KH * 3.5f : KH * r + KH / 2;
            keyPress(c, x, y);
        }
        return parseCands(decode(n));
    }

    // Como Gboard al pulsar espacio: teclea la palabra con onKeyPress (centros o toques reales), opcionalmente decode,
    // y manda el espacio; devuelve {lo que el motor confirma (axfs.3), su respuesta}. El motor decide la corrección.
    public static synchronized String[] typeAndSpace(String before, String word, boolean decodeFirst, float[] xs, float[] ys) {
        long id = ++reqId;
        abortComposing(id);
        setContext(before, "", "", 0);
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            float x, y;
            if (xs != null && i < xs.length && !Float.isNaN(xs[i])) { x = xs[i]; y = ys[i]; }
            else {
                char k = base(Character.toLowerCase(c));
                int r = row(k);
                x = r < 0 ? 540 : OFF[r] + KW * ROWS[r].indexOf(k) + KW / 2;
                y = r < 0 ? KH * 3.5f : KH * r + KH / 2;
            }
            keyPress(c, x, y);
        }
        if (decodeFirst) decode(8);
        byte[] k = keyPress(' ', 540, KH * 3.5f);
        String commit = "";
        Pb.F st = Pb.one(Pb.parse(k), 2);
        Pb.F ed = st == null ? null : Pb.one(Pb.parse(st.d), 2);
        if (ed != null) { Pb.F c3 = Pb.one(Pb.parse(ed.d), 3); if (c3 != null) commit = c3.str(); }
        return new String[]{commit, Pb.dump(k, "  ")};
    }

    // Teclea sin eco y devuelve, por prefijo, la puntuación del literal que trae cada respuesta de onKeyPress (NaN si no).
    public static synchronized float[] prefixScores(String before, String word) {
        long id = ++reqId;
        abortComposing(id);
        setContext(before, "", "", 0);
        float[] out = new float[word.length()];
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            char k = base(Character.toLowerCase(c));
            int r = row(k);
            float x = r < 0 ? 540 : OFF[r] + KW * ROWS[r].indexOf(k) + KW / 2;
            float y = r < 0 ? KH * 3.5f : KH * r + KH / 2;
            byte[] resp = keyPress(c, x, y);
            String pre = word.substring(0, i + 1);
            out[i] = Float.NaN;
            for (Cand cd : parseCands(resp)) if (cd.text.equals(pre) && !Float.isNaN(cd.score)) out[i] = cd.score;
        }
        return out;
    }

    // Decodifica la palabra en curso usando coordenadas de toque REALES por letra (x[i],y[i] en el
    // mismo marco de píxeles que el layout). Es el camino fiel: el modelo espacial de Gboard corre
    // con la posición real del dedo, no con el centro de la tecla.
    public static synchronized java.util.List<Cand> suggestXY(String before, String word, int n, float[] x, float[] y) {
        long id = ++reqId;
        abortComposing(id);
        setContext(before, "", "", 0);
        for (int i = 0; i < word.length(); i++) keyPress(word.charAt(i), x[i], y[i]);
        return parseCands(decode(n));
    }

    // Experimento siguiente-palabra: teclea el texto entero (espacios incluidos) como lo haría el usuario y
    // luego decodifica sin palabra en curso, igual que el fetchSuggestions de Gboard tras pulsar espacio.
    public static synchronized byte[] typeThenDecodeRaw(String text, int n) {
        long id = ++reqId;
        abortComposing(id);
        setContext("", "", "", 0);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char k = base(Character.toLowerCase(c));
            int r = row(k);
            float x = r < 0 ? 540 : OFF[r] + KW * ROWS[r].indexOf(k) + KW / 2;
            float y = r < 0 ? KH * 3.5f : KH * r + KH / 2;
            keyPress(c, x, y);
        }
        return decode(n);
    }

    // Como suggest, pero devuelve también la respuesta de la última pulsación (Gboard recibe los candidatos
    // en cada onKeyPress; decode puede devolver solo "sin cambios"). [0] = última pulsación, [1] = decode.
    public static synchronized byte[][] suggestBothRaw(String before, String word, int n) {
        long id = ++reqId;
        abortComposing(id);
        setContext(before, "", "", 0);
        byte[] last = new byte[0];
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            char k = base(Character.toLowerCase(c));
            int r = row(k);
            float x = r < 0 ? 540 : OFF[r] + KW * ROWS[r].indexOf(k) + KW / 2;
            float y = r < 0 ? KH * 3.5f : KH * r + KH / 2;
            last = keyPress(c, x, y);
        }
        return new byte[][]{last, decode(n)};
    }

    // Depuración: traza de cada respuesta (abort, contexto, cada tecla, decode) con la versión de contexto usada.
    public static synchronized java.util.List<String> trace(String before, String word, int n) {
        java.util.List<String> out = new java.util.ArrayList<>();
        long id = ++reqId;
        byte[] r = abortComposing(id);
        out.add("abort -> v=" + ctxVersion + " " + Pb.dump(r, "").replace("\n", " "));
        r = setContext(before, "", "", 0);
        out.add("ctx[" + before + "] -> v=" + ctxVersion + " " + Pb.dump(r, "").replace("\n", " "));
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            char k = base(Character.toLowerCase(c));
            int rw = row(k);
            float x = rw < 0 ? 540 : OFF[rw] + KW * ROWS[rw].indexOf(k) + KW / 2;
            float y = rw < 0 ? KH * 3.5f : KH * rw + KH / 2;
            int vSent = ctxVersion;
            r = keyPress(c, x, y);
            String d = Pb.dump(r, "").replace("\n", " ");
            out.add("key " + c + " (v enviada " + vSent + ") -> v=" + ctxVersion + " " + (d.length() > 160 ? d.substring(0, 160) + "…" : d));
        }
        r = decode(n);
        String d = Pb.dump(r, "").replace("\n", " ");
        out.add("decode -> v=" + ctxVersion + " " + (d.length() > 200 ? d.substring(0, 200) + "…" : d));
        return out;
    }

    // Igual que suggest pero devuelve la respuesta cruda (para depurar).
    public static synchronized byte[] suggestRaw(String before, String word, int n) {
        long id = ++reqId;
        abortComposing(id);
        setContext(before, "", "", 0);
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            char k = base(Character.toLowerCase(c));
            int r = row(k);
            float x = r < 0 ? 540 : OFF[r] + KW * ROWS[r].indexOf(k) + KW / 2;
            float y = r < 0 ? KH * 3.5f : KH * r + KH / 2;
            keyPress(c, x, y);
        }
        return decode(n);
    }

    // Candidatos de una respuesta decode/onKeyPress: awzf(2).axfj(3).awzw(1)[texto=2, tipo=1, rango=14].
    public static java.util.List<String> candidates(byte[] r) {
        java.util.List<String> out = new java.util.ArrayList<>();
        Pb.F st = Pb.one(Pb.parse(r), 2);
        if (st == null) return out;
        Pb.F cs = Pb.one(Pb.parse(st.d), 3);
        if (cs == null) return out;
        for (Pb.F c : Pb.all(Pb.parse(cs.d), 1)) {
            java.util.List<Pb.F> f = Pb.parse(c.d);
            Pb.F t = Pb.one(f, 2), ty = Pb.one(f, 1), sc = Pb.one(f, 4);
            out.add((t == null ? "" : t.str()) + "[t" + (ty == null ? 0 : ty.v) + (sc == null ? "" : String.format(",%.1f", sc.flt())) + "]");
        }
        Pb.F auto = Pb.one(Pb.parse(cs.d), 2);
        out.add("ac=" + (auto == null ? "-" : auto.v));
        return out;
    }

    // axml: evento táctil (acción 0, x, y, código, etiqueta, tiempo).
    // Experimento I+D: bits de booleanos de la pulsación. 1 = axcn.4, 2 = axml.12, 4 = axml.22 (u), 8 = axml.23 (v).
    public static int kpBits = 0;

    static Pb touch(int cp, float x, float y, long t) {
        Pb p = Pb.n().i(1, 0).f(3, x).f(4, y).i(5, (int) t).i(6, cp).s(7, new String(Character.toChars(cp))).i(25, t);
        if ((kpBits & 2) != 0) p.b(12, true);
        if ((kpBits & 4) != 0) p.b(22, true);
        if ((kpBits & 8) != 0) p.b(23, true);
        return p;
    }

    static long reqId = 1;
    static int ctxVersion = 0;
    // Estado de decodificación que devuelve el motor (axex.e de setDecodeMode, axdr.e de parseInputContext);
    // Gboard lo reenvía en el campo 1 de los metadatos de cada petición.
    static int decodeState = 0;

    // axab: metadatos de la petición (estado de decodificación, versión de contexto, id).
    static Pb meta(long id) {
        return Pb.n().i(1, decodeState).i(2, ctxVersion).i(3, id);
    }

    // awyk{1 id, 2 marca de tiempo}: Gboard lo llama al empezar una sesión de escritura (abrir un campo).
    public static synchronized void beginSession() {
        long id = ++reqId;
        Decoder.beginSessionNative(Pb.n().i(1, id).i(2, System.currentTimeMillis()).bytes());
    }

    // axev{1 metadatos, 2 modo de dispositivo} -> axex{1 error, 2 versión, 3 estado}. Gboard lo llama al abrir
    // la sesión con DM_VIRTUAL_KEYBOARD (1); sin esto el motor queda en DM_UNSPECIFIED.
    public static synchronized byte[] setDecodeMode(int deviceMode) {
        long id = ++reqId;
        byte[] r = Decoder.setDecodeModeNative(Pb.n().m(1, meta(id)).i(2, deviceMode).bytes());
        java.util.List<Pb.F> f = Pb.parse(r);
        Pb.F st = Pb.one(f, 3);
        if (st != null) decodeState = (int) st.v;
        return r;
    }

    // Eco del campo: tras cada tecla se reenvía el estado del campo (texto confirmado + palabra en curso). Sin él,
    // con los ajustes de sesión el motor cierra la palabra tras un separador y no decodifica con contexto.
    // echoType = tipo del span axao (0, 5 o 6; -1 = sin span; -2 = sin eco); echoReason = motivo (0 funciona).
    public static int echoType = -1, echoReason = 0;
    public static boolean echoSkipLast = false;
    static String echoBefore, echoAfter;

    // Contexto con spans: axdn.11 = repeated axao{1 inicio, 2 fin (puntos de código), 3 tipo, 4 texto}.
    static byte[] setContextSpan(String before, String after, int spanStart, int spanEnd, int type, String spanText, int reason) {
        long id = ++reqId;
        int cps = before.codePointCount(0, before.length());
        Pb p = Pb.n().s(1, before).s(2, "").s(3, after).b(4, false).b(5, false).b(6, false)
                .m(7, meta(id)).i(9, id).i(10, reason).i(12, cps).i(13, cps);
        if (type >= 0 && spanEnd > spanStart) p.m(11, Pb.n().i(1, spanStart).i(2, spanEnd).i(3, type).s(4, spanText));
        byte[] r = Decoder.parseInputContextNative(p.bytes());
        Pb.F v = Pb.one(Pb.parse(r), 5);
        if (v != null) ctxVersion = (int) v.v;
        return r;
    }

    // Decodifica como un teclado real: tras cada tecla aplica la edición que devuelve el motor (axfs: 3 texto
    // confirmado, 6 palabra en curso) a su copia del campo y se la devuelve con la palabra en curso marcada.
    public static synchronized byte[] suggestEchoRaw(String before, String word, int n) {
        return suggestEchoRaw(before, "", word, n);
    }

    // after: texto del campo tras el cursor (Gboard lo manda en axdn.3).
    public static synchronized byte[] suggestEchoRaw(String before, String after, String word, int n) {
        return suggestEchoRaw(before, after, word, n, null, null, null);
    }

    // xs/ys: posición real del dedo por letra en el marco del layout (NaN o null = centro de la tecla).
    // prefixLit: si no es null, recibe la puntuación del motor para cada prefijo tecleado (lo tecleado hasta la letra i,
    // NaN si el motor no lo conoce); sirve para encontrar cortes de palabras pegadas sin decodificar de más.
    public static synchronized byte[] suggestEchoRaw(String before, String after, String word, int n,
                                                     float[] xs, float[] ys, float[] prefixLit) {
        long id = ++reqId;
        abortComposing(id);
        setContext(before, "", after, 0);
        boolean changedContext = !before.equals(echoBefore) || !after.equals(echoAfter);
        String committed = before, composing = "";
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            char k = base(Character.toLowerCase(c));
            int rw = row(k);
            float x = rw < 0 ? 540 : OFF[rw] + KW * ROWS[rw].indexOf(k) + KW / 2;
            float y = rw < 0 ? KH * 3.5f : KH * rw + KH / 2;
            if (xs != null && i < xs.length && !Float.isNaN(xs[i])) { x = xs[i]; y = ys[i]; }
            byte[] r = keyPress(c, x, y);
            if (prefixLit != null && i < prefixLit.length) {
                String pre = word.substring(0, i + 1);
                float best = Float.NaN;
                for (Cand cd : parseCands(r)) if (cd.text.equals(pre) && !Float.isNaN(cd.score)) best = cd.score;
                prefixLit[i] = best;
            }
            Pb.F st = Pb.one(Pb.parse(r), 2);
            Pb.F ed = st == null ? null : Pb.one(Pb.parse(st.d), 2);
            if (ed != null) {
                java.util.List<Pb.F> ef = Pb.parse(ed.d);
                Pb.F ins = Pb.one(ef, 3), comp = Pb.one(ef, 6);
                String insS = ins == null ? "" : ins.str(), compS = comp == null ? "" : comp.str();
                if (!insS.isEmpty()) { committed = committed + insS; composing = ""; }
                if (!compS.isEmpty() || insS.isEmpty()) composing = compS;
            }
            if (echoType >= -1 && !(echoSkipLast && i == word.length() - 1)) {
                int s = committed.codePointCount(0, committed.length());
                setContextSpan(committed + composing, after, s, s + composing.codePointCount(0, composing.length()), echoType, composing, echoReason);
            }
            // Con otro contexto, actualizar el primer prefijo como en el tecleo incremental de Gboard.
            // Si solo se decodifica la palabra final, Delight puede reutilizar su puntuación del contexto anterior.
            if (changedContext && i == 0 && word.length() > 1) decode(n);
        }
        byte[] result = decode(n);
        echoBefore = before;
        echoAfter = after;
        return result;
    }

    // Variante de construcción del contexto (experimento I+D; 0 = la de siempre).
    public static int ctxVariant = 0;
    // Experimento: tomar el estado de decodificación de la respuesta de parseInputContext.
    public static boolean trackState = false;

    // axdn: 1 texto antes del cursor, 2 seleccionado, 3 después, 4/5/6 indicadores, 7 metadatos, 9 id,
    // 10 motivo, 12/13 inicio/fin de selección en puntos de código (Gboard no los manda con motivo 0 ni 5).
    public static byte[] setContext(String before, String sel, String after, int reason) {
        long id = ++reqId;
        int cps = before.codePointCount(0, before.length());
        int cv = ctxVariant;
        int rsn = cv >= 2 && cv <= 6 ? cv - 1 : reason;          // 2..6 -> motivo 1..5
        if (cv >= 10) rsn = cv % 10;                             // 1N: motivo N con selección; 2N: sin selección
        Pb p = Pb.n().s(1, before).s(2, sel).s(3, after)
                .b(4, cv == 7).b(5, cv == 8).b(6, cv == 9)
                .m(7, meta(id)).i(9, id).i(10, rsn);
        if (cv != 1 && cv < 20) p.i(12, cps).i(13, cps);         // 1 y 2N = sin selección
        byte[] r = Decoder.parseInputContextNative(p.bytes());
        java.util.List<Pb.F> rf = Pb.parse(r);
        Pb.F v = Pb.one(rf, 5);
        if (v != null) ctxVersion = (int) v.v;
        if (trackState) { Pb.F st = Pb.one(rf, 3); decodeState = st == null ? 0 : (int) st.v; }  // axdr.e
        return r;
    }

    public static byte[] keyPress(int cp, float x, float y) {
        long id = ++reqId;
        long t = System.currentTimeMillis();
        return track(Decoder.onKeyPressNative(Pb.n().m(1, Pb.n().b(2, false).m(1, touch(cp, x, y, t))).m(2, meta(id)).i(3, id).b(4, (kpBits & 1) != 0).bytes()));
    }

    // Swipe como lwt.b() de Gboard: decode con axct.2 = axmh{1 puntos del trazo, 2 true (es gesto)}.
    // Cada punto axml{1 acción (0 bajar, 2 mover, 1 subir), 2 dedo, 3 x, 4 y, 5 ms desde el inicio, 25 ms}.
    // Coordenadas en el mismo marco que el layout. before: texto previo (contexto de la frase).
    public static long gestureTimeBase = 0;

    // Trazo por partes, como Gboard mientras se desliza: start = primer envío del gesto (reinicia el campo y manda el
    // "dedo abajo"); end = el dedo se levanta. Solo se mandan los puntos desde 'from'.
    public static synchronized byte[] decodeGestureStream(String before, String after, float[] x, float[] y, long[] t,
                                                          int from, boolean start, boolean end, int n) {
        long id;
        if (start) {
            id = ++reqId;
            abortComposing(id);
            setContext(before, "", after, 0);
        }
        Pb trace = Pb.n();
        for (int i = from; i < x.length; i++) {
            int act = (start && i == 0) ? 0 : (end && i == x.length - 1 ? 1 : 2);
            long dt = t[i] - t[0];
            trace.m(1, Pb.n().i(1, act).i(2, 0).f(3, x[i]).f(4, y[i]).i(5, (int) dt).i(25, dt));
        }
        trace.b(2, true);
        id = ++reqId;
        return track(Decoder.decodeNative(Pb.n().i(1, n).m(2, trace).m(7, meta(id)).i(8, id).bytes()));
    }

    public static synchronized byte[] decodeGestureRaw(String before, String after, float[] x, float[] y, long[] t, int n) {
        long id = ++reqId;
        abortComposing(id);
        setContext(before, "", after, 0);
        Pb trace = Pb.n();
        for (int i = 0; i < x.length; i++) {
            int act = i == 0 ? 0 : (i == x.length - 1 ? 1 : 2);
            long dt = t[i] - t[0] + gestureTimeBase;
            trace.m(1, Pb.n().i(1, act).i(2, 0).f(3, x[i]).f(4, y[i]).i(5, (int) dt).i(25, dt));
        }
        trace.b(2, true);
        id = ++reqId;
        return track(Decoder.decodeNative(Pb.n().i(1, n).m(2, trace).m(7, meta(id)).i(8, id).bytes()));
    }

    // Palabra que el motor escribe con el gesto (edición axfs.6 = texto en composición de la respuesta).
    public static String gestureText(byte[] r) {
        Pb.F st = Pb.one(Pb.parse(r), 2);
        Pb.F ed = st == null ? null : Pb.one(Pb.parse(st.d), 2);
        if (ed == null) return "";
        Pb.F comp = Pb.one(Pb.parse(ed.d), 6);
        return comp == null ? "" : comp.str();
    }

    // Trazo sintético por los centros de las teclas de una palabra (pruebas): un punto cada ~step px, 60 px/ms·10.
    public static float synthMsPerPt = 8;

    public static float[][] synthTrace(String word, float step) {
        java.util.List<float[]> pts = new java.util.ArrayList<>();
        float px = Float.NaN, py = Float.NaN;
        for (int i = 0; i < word.length(); i++) {
            char k = base(Character.toLowerCase(word.charAt(i)));
            int r = row(k);
            if (r < 0) continue;
            float cx = OFF[r] + KW * ROWS[r].indexOf(k) + KW / 2, cy = KH * r + KH / 2;
            if (!Float.isNaN(px)) {
                float d = (float) Math.hypot(cx - px, cy - py);
                int s = Math.max(1, (int) (d / step));
                for (int j = 1; j <= s; j++) pts.add(new float[]{px + (cx - px) * j / s, py + (cy - py) * j / s});
            } else {
                pts.add(new float[]{cx, cy});
            }
            px = cx; py = cy;
        }
        float[] xs = new float[pts.size()], ys = new float[pts.size()], ts = new float[pts.size()];
        for (int i = 0; i < pts.size(); i++) { xs[i] = pts.get(i)[0]; ys[i] = pts.get(i)[1]; ts[i] = i * synthMsPerPt; }
        return new float[][]{xs, ys, ts};
    }

    // Confirma texto como Gboard (lwt.I -> replaceTextNative): axej{1 meta, 2 inicio, 3 fin (puntos de código),
    // 4 texto, 5 motivo-1, 6 ms}. Gboard usa motivo 5 al confirmar palabras.
    public static synchronized byte[] replaceText(int start, int end, String text, int reason) {
        long id = ++reqId;
        return Decoder.replaceTextNative(Pb.n().m(1, meta(id)).i(2, start).i(3, end).s(4, text).i(5, reason - 1)
                .i(6, System.currentTimeMillis()).bytes());
    }

    // Experimento de aprendizaje: teclea la palabra con eco, pulsa espacio y deja el campo con la palabra confirmada.
    // mode 1 = espacio por onKeyPress; 2 = además replaceText motivo 5; 3 = solo contexto confirmado.
    public static synchronized void commitWord(String before, String word, int mode) {
        if (mode != 3) suggestEchoRaw(before, "", word, 4);
        int s = before.codePointCount(0, before.length());
        if (mode == 1 || mode == 2) keyPress(' ', 540, KH * 3.5f);
        if (mode == 2) replaceText(s, s + word.codePointCount(0, word.length()), word, 5);
        setContext(before + word + " ", "", "", 0);
    }

    // ---- Aprendizaje (como mas/lyb de Gboard): n-gramas en el historial con DynamicLm ----
    // axjl del historial bilingüe (tipo 3, campo 13 = idiomas).
    static Pb uhDesc(String path) {
        Pb d = Pb.n().i(1, LM_USER_HISTORY).s(4, path).i(5, 0).i(6, new File(path).length());
        d.s(13, "en").s(13, "es");
        return d;
    }

    // Suma n-gramas al historial: cada elemento {contexto (hasta 2 palabras previas), palabra}
    // (axgy{1 axhm{1 palabra, 2 contexto, 3 cuenta, 4 s}, 2 cuenta, 3 axjl}). reload: recarga el modelo en el
    // decodificador para que lo aprendido se note ya (como Delight5Facilitator.G tras escribir el historial).
    public static synchronized boolean learn(String path, java.util.List<String[]> ngrams, boolean reload) {
        if (!new File(path).exists() || ngrams.isEmpty()) return false;
        byte[] desc = uhDesc(path).bytes();
        if (!com.google.android.keyboard.client.delight5.DynamicLm.openDynamicLmNative(desc)) return false;
        int now = (int) (System.currentTimeMillis() / 1000);
        for (String[] ng : ngrams) {
            int cps = ng[1].codePointCount(0, ng[1].length());
            if (cps < 2 || cps > 48) continue;
            Pb h = Pb.n().s(1, ng[1]).s(2, ng[0]).i(3, 1).i(4, now);
            com.google.android.keyboard.client.delight5.DynamicLm.incrementNgramInDynamicLmNative(
                    Pb.n().m(1, h).i(2, 1).y(3, desc).bytes());
        }
        com.google.android.keyboard.client.delight5.DynamicLm.flushDynamicLmNative(desc);
        com.google.android.keyboard.client.delight5.DynamicLm.closeDynamicLmNative(desc);
        if (reload) {
            Decoder.unloadLanguageModelNative(Pb.n().y(1, uhDesc(path).bytes()).bytes());
            loadPersonalLm(LM_USER_HISTORY, path, null, null, "en", "es");
        }
        return true;
    }

    // Olvida n-gramas del historial (DynamicLm.setNgramInDynamicLm con cuenta 0): axhi{1 axhm{1 palabra, 2 contexto,
    // 3 0, 4 s}, 2 axjl}. Cada elemento {contexto, palabra}; "" = la palabra suelta.
    public static synchronized boolean forget(String path, java.util.List<String[]> ngrams, boolean reload) {
        if (!new File(path).exists() || ngrams.isEmpty()) return false;
        byte[] desc = uhDesc(path).bytes();
        if (!com.google.android.keyboard.client.delight5.DynamicLm.openDynamicLmNative(desc)) return false;
        int now = (int) (System.currentTimeMillis() / 1000);
        for (String[] ng : ngrams) {
            Pb h = Pb.n().s(1, ng[1]).s(2, ng[0]).i(3, 0).i(4, now);
            com.google.android.keyboard.client.delight5.DynamicLm.setNgramInDynamicLmNative(Pb.n().m(1, h).y(2, desc).bytes());
        }
        com.google.android.keyboard.client.delight5.DynamicLm.flushDynamicLmNative(desc);
        com.google.android.keyboard.client.delight5.DynamicLm.closeDynamicLmNative(desc);
        if (reload) {
            Decoder.unloadLanguageModelNative(Pb.n().y(1, uhDesc(path).bytes()).bytes());
            loadPersonalLm(LM_USER_HISTORY, path, null, null, "en", "es");
        }
        return true;
    }

    // Contenido del historial (DynamicLm.iterateOverDynamicLm, axhc{1 axjl}): respuesta en bruto.
    public static synchronized byte[] iterateUh(String path) {
        byte[] desc = uhDesc(path).bytes();
        if (!com.google.android.keyboard.client.delight5.DynamicLm.openDynamicLmNative(desc)) return null;
        try {
            return com.google.android.keyboard.client.delight5.DynamicLm.iterateOverDynamicLmNative(Pb.n().y(1, desc).bytes());
        } finally {
            com.google.android.keyboard.client.delight5.DynamicLm.closeDynamicLmNative(desc);
        }
    }

    // Una entrada del historial (getNgramFromDynamicLm, axgu{4 axjl}... sin el n-grama en esta versión): en bruto.
    public static synchronized byte[] getNgram(String path, String ctx, String word) {
        byte[] desc = uhDesc(path).bytes();
        if (!com.google.android.keyboard.client.delight5.DynamicLm.openDynamicLmNative(desc)) return null;
        try {
            Pb h = Pb.n().s(1, word).s(2, ctx);
            return com.google.android.keyboard.client.delight5.DynamicLm.getNgramFromDynamicLmNative(Pb.n().m(1, h).y(4, desc).bytes());
        } finally {
            com.google.android.keyboard.client.delight5.DynamicLm.closeDynamicLmNative(desc);
        }
    }

    // Cierre de sesión de escritura (LatinIme.onDeactivate de Gboard): axau{3 id}.
    public static synchronized byte[] finishSession() {
        long id = ++reqId;
        return Decoder.finishSessionNative(Pb.n().i(3, id).bytes());
    }

    public static byte[] decode(int n) {
        long id = ++reqId;
        return track(Decoder.decodeNative(Pb.n().i(1, n).m(7, meta(id)).i(8, id).bytes()));
    }
}
