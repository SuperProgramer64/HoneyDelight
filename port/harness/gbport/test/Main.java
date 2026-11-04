package gbport.test;

import gbport.Decide;
import gbport.Delight;
import java.util.List;

// Prueba del motor de Gboard fuera de Gboard: args = carpeta, luego pares "contexto|palabra".
public final class Main {
    static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format("%02x", x));
        return s.length() == 0 ? "-" : s.toString();
    }

    public static void main(String[] a) throws Exception {
        String base = a[0];
        Delight.loadLibrary(base + "/libintegrated_shared_object.so", base + "/crash");
        Delight.createDecoder(new String[][]{{"es", "US"}, {"en", "US"}}, base + "/data");
        Delight.setQwertyLayout();
        System.out.println("LM es=" + Delight.loadMainLm(base + "/lm/datadownloadfile_1788031289653", "es", "US")
                + " en=" + Delight.loadMainLm(base + "/lm/main_en_us_2026080409_2", "en", "US"));
        if (System.getenv("GB_RTM") != null) Delight.rtMask = Integer.parseInt(System.getenv("GB_RTM"));
        if (System.getenv("GB_PERSO") != null) {
            String pd = System.getenv("GB_PERSO") + "/";
            System.out.println("personales: historial=" + Delight.loadPersonalLm(Delight.LM_USER_HISTORY, pd + "UserHistory.dict", null, null, "en", "es")
                    + " pers-es=" + Delight.loadPersonalLm(Delight.LM_PERSONAL, pd + "Personal.es_US.dict", "es", "US")
                    + " pers-en=" + Delight.loadPersonalLm(Delight.LM_PERSONAL, pd + "Personal.en_US.dict", "en", "US")
                    + " contactos=" + Delight.loadPersonalLm(Delight.LM_CONTACTS, pd + "Contacts.dict", null, null)
                    + (System.getenv("GB_BLOCK") == null ? "" : " bloq-es=" + Delight.loadPersonalLm(Delight.LM_BLOCKLIST, pd + "blacklist.es_us.list", "es", "US")
                        + " bloq-en=" + Delight.loadPersonalLm(Delight.LM_BLOCKLIST, pd + "blacklist.en_us.list", "en", "US")
                        + " correo=" + Delight.loadPersonalLm(Delight.LM_EMAIL, pd + "Email.dict", null, null)));
        }
        if (System.getenv("GB_EMOJI") != null) {
            String ed = System.getenv("GB_EMOJI") + "/";
            System.out.println("emoji: lm-es=" + Delight.loadEmojiLm(ed + "es_us", "es", "US") + " lm-en=" + Delight.loadEmojiLm(ed + "en_us", "en", "US")
                    + " atajos=" + Delight.loadEmojiShortcuts(new String[][]{{ed + "es_us.shortcuts", "es"}, {ed + "en_us.shortcuts", "en"}}));
        }
        if (System.getenv("GB_FULL") != null) { Delight.setRuntimeParams(true, System.getenv("GB_FULL"), false); System.out.println("runtime params: completos"); }
        else if (System.getenv("GB_RT") != null) { Delight.setRuntimeParams(!"0".equals(System.getenv("GB_NW"))); System.out.println("runtime params: on"); }
        if (System.getenv("GB_AC") != null) { Delight.axiu10 = Float.parseFloat(System.getenv("GB_AC")); Delight.setRuntimeParams(true); }
        if (System.getenv("GB_CV") != null) Delight.ctxVariant = Integer.parseInt(System.getenv("GB_CV"));
        if (System.getenv("GB_KP") != null) Delight.kpBits = Integer.parseInt(System.getenv("GB_KP"));
        if (System.getenv("GB_TS") != null) Delight.trackState = true;
        if (System.getenv("GB_BS") != null) { Delight.beginSession(); System.out.println("begin session"); }
        if (System.getenv("GB_DM") != null) {
            byte[] r = Delight.setDecodeMode(Integer.parseInt(System.getenv("GB_DM")));
            System.out.println("decode mode -> " + gbport.Pb.dump(r, "").replace("\n", " "));
        }
        if (a.length > 1 && a[1].startsWith("ADAPTREPLAY:")) {
            SpatialReplay.run(a[1].substring(12));
            return;
        }
        if (a.length > 1 && a[1].equals("JOINED")) {
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                String before = t[0], word = t[1], after = t.length > 2 ? t[2] : "";
                float[] prefix = new float[word.length()];
                java.util.Arrays.fill(prefix, Float.NaN);
                List<Delight.Cand> cs = Delight.parseCands(Delight.suggestEchoRaw(before, after, word, 20,
                        null, null, prefix));
                gbport.Split.note(before, word, cs);
                gbport.Split.Result split = gbport.Split.update(before, after, word, System.nanoTime() + 250_000_000L);
                Decide.Result literal = Decide.decide(cs, word, false);
                boolean wins = gbport.Split.wins(split, literal.litScore,
                        literal.cand == null ? literal.litScore : literal.cand.score);
                if (t.length > 3) {
                    boolean expectedSplit = !"-".equals(t[3]);
                    if (wins != expectedSplit || (wins && !t[3].equals(split.text)))
                        throw new AssertionError("Split " + word + " expected=" + t[3]
                                + " actual=" + (wins ? split.text : "-"));
                }
                System.out.println("JOINED " + word + " => " + (split == null ? "-" : split.text)
                        + " prefixes=" + java.util.Arrays.toString(prefix));
                for (Delight.Cand c : cs) System.out.println("CAND " + c.text + " type=" + c.type
                        + " score=" + c.score + " lm=" + c.lm + " spatial=" + c.spatial);
            }
            return;
        }
        if (a.length > 1 && a[1].startsWith("ECHO:")) {
            // Decodificación con eco del estado del campo (protocolo de teclado real).
            if (System.getenv("GB_ET") != null) Delight.echoType = Integer.parseInt(System.getenv("GB_ET"));
            if (System.getenv("GB_ER") != null) Delight.echoReason = Integer.parseInt(System.getenv("GB_ER"));
            Delight.echoSkipLast = System.getenv("GB_ESL") != null;
            for (int i = 1; i < a.length; i++) {
                String[] t = a[i].substring(a[i].startsWith("ECHO:") ? 5 : 0).split("\\|", -1);
                StringBuilder sb = new StringBuilder("ECHO [" + t[0] + "|" + t[1] + "] ");
                for (Delight.Cand c : Delight.parseCands(Delight.suggestEchoRaw(t[0], t[1], 8))) {
                    sb.append(c.text).append(c.auto ? "*" : "").append(String.format("(%.1f) ", c.score));
                }
                System.out.println(sb);
            }
            return;
        }
        if (a.length > 1 && a[1].equals("BLOCK")) {
            byte[] r = com.google.android.keyboard.client.delight5.Decoder.getBlocklistedWordsNative();
            String d = gbport.Pb.dump(r, "");
            System.out.println("BLOCK bytes=" + (r == null ? -1 : r.length) + "\n" + (d.length() > 1500 ? d.substring(0, 1500) : d));
            byte[] f = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(System.getenv("GB_PERSO") + "/blacklist.es_us.list"));
            String fd = gbport.Pb.dump(f, "");
            System.out.println("ARCHIVO\n" + (fd.length() > 1200 ? fd.substring(0, 1200) : fd));
            return;
        }
        if (a.length > 1 && a[1].equals("SPAT")) {
            // Posición real del dedo frente a centro de tecla: "antes|registrado|intención[|temblor]".
            // El toque de cada letra cae entre la tecla querida y la registrada (55 % hacia la registrada si difieren),
            // más un temblor gaussiano (fracción de tecla).
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                String reg = t[1], want = t[2];
                float jit = t.length > 3 ? Float.parseFloat(t[3]) : 0.12f;
                java.util.Random rnd = new java.util.Random(reg.hashCode());
                float[] xs = new float[reg.length()], ys = new float[reg.length()];
                for (int j = 0; j < reg.length(); j++) {
                    float[] cr = Delight.center(reg.charAt(j));
                    float[] cw = j < want.length() ? Delight.center(want.charAt(j)) : null;
                    if (cr == null) { xs[j] = Float.NaN; ys[j] = Float.NaN; continue; }
                    float x = cr[0], y = cr[1];
                    if (cw != null && (cw[0] != cr[0] || cw[1] != cr[1])) { x = cw[0] + 0.55f * (cr[0] - cw[0]); y = cw[1] + 0.55f * (cr[1] - cw[1]); }
                    xs[j] = x + (float) rnd.nextGaussian() * jit * 108; ys[j] = y + (float) rnd.nextGaussian() * jit * 160;
                }
                Decide.Result rc = Decide.decide(Delight.parseCands(Delight.suggestEchoRaw(t[0], "", reg, 20)), reg, false);
                Decide.Result rr = Decide.decide(Delight.parseCands(Delight.suggestEchoRaw(t[0], "", reg, 20, xs, ys, null)), reg, false);
                System.out.println("SPAT [" + t[0] + "] " + reg + " (querías " + want + ")  centro -> " + (rc.cand == null ? reg : rc.cand.text)
                        + " [" + rc.why + "]   real -> " + (rr.cand == null ? reg : rr.cand.text) + " [" + rr.why + "]");
            }
            return;
        }
        if (a.length > 1 && a[1].equals("SPLIT")) {
            // Simula el teclado: decodifica cada prefijo como si se tecleara letra a letra (eso alimenta el corte
            // incremental) y mide el coste extra por tecla del corte. "antes|palabra".
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                String w = t[1];
                long extraMax = 0, baseMax = 0;
                Decide.Result r = null;
                gbport.Split.Result sp = null;
                for (int n = 1; n <= w.length(); n++) {
                    String pre = w.substring(0, n);
                    long t0 = System.nanoTime();
                    java.util.List<Delight.Cand> cs = Delight.parseCands(Delight.suggestEchoRaw(t[0], "", pre, 20));
                    r = Decide.decide(cs, pre, false);
                    gbport.Split.note(t[0], pre, cs);
                    long t1 = System.nanoTime();
                    sp = gbport.Split.update(t[0], "", pre, System.nanoTime() + 200_000_000L);
                    long t2 = System.nanoTime();
                    baseMax = Math.max(baseMax, (t1 - t0) / 1_000_000); extraMax = Math.max(extraMax, (t2 - t1) / 1_000_000);
                }
                double single = r.cand != null ? r.cand.score : r.litScore;
                boolean win = gbport.Split.wins(sp, r.litScore, single);
                String got = win ? sp.text : (r.cand == null ? w : r.cand.text);
                if (gbport.Split.needsSpace(t[0], w)) got = " " + got;
                System.out.println("SPLIT [" + t[0] + "] " + w + " -> [" + got + "] " + (win ? "corte" : r.why)
                        + String.format("  base<=%dms corte+<=%dms", baseMax, extraMax)
                        + (sp == null ? "" : String.format("  (corte %s %.1f vs %.1f lit %.1f)", sp.text, sp.score, single, r.litScore)));
            }
            return;
        }
        if (a.length > 1 && a[1].equals("PRE")) {
            // Puntuación de cada prefijo durante la decodificación con eco: "antes|palabra".
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                float[] pl = Delight.prefixScores(t[0], t[1]);
                StringBuilder sb = new StringBuilder("PRE [" + t[0] + "] ");
                for (int j = 0; j < pl.length; j++) sb.append(t[1], 0, j + 1).append(Float.isNaN(pl[j]) ? "(?) " : String.format("(%.1f) ", pl[j]));
                System.out.println(sb);
            }
            return;
        }
        if (a.length > 1 && a[1].equals("TIME")) {
            // Tiempo por petición (ms) según el largo del contexto: corrección (eco) y siguiente palabra.
            String frase = "Peleas con amigos y familia por cosas pequeñas, la verdad, es que a veces me cansa un poco todo esto. ";
            for (int len : new int[]{40, 120, 300, 1000}) {
                StringBuilder b = new StringBuilder();
                while (b.length() < len) b.append(frase);
                String before = b.substring(b.length() - len);
                long t0 = System.nanoTime();
                for (int k = 0; k < 5; k++) Delight.suggestEchoRaw(before, "", "exmplea", 26);
                long t1 = System.nanoTime();
                for (int k = 0; k < 5; k++) Delight.suggest(before, "", "", 26);
                long t2 = System.nanoTime();
                System.out.println(String.format("TIME ctx=%4d  palabra(7 letras)=%5.1f ms  siguiente=%5.1f ms", len, (t1 - t0) / 5e6, (t2 - t1) / 5e6));
            }
            return;
        }
        if (a.length > 1 && a[1].startsWith("LEARN:")) {
            // LEARN:modo palabra prefijo veces — ¿el motor aprende una palabra nueva confirmándola?
            int mode = Integer.parseInt(a[1].substring(6));
            String w = a[2], pre = a[3]; int times = Integer.parseInt(a[4]);
            java.util.function.Supplier<String> probe = () -> {
                StringBuilder sb = new StringBuilder();
                for (Delight.Cand c : Delight.suggest("hola ", pre, 8)) sb.append(c.text).append(Float.isNaN(c.score) ? "(?) " : String.format("(%.1f) ", c.score));
                sb.append(" | literal: ");
                for (Delight.Cand c : Delight.suggest("hola ", w, 4)) sb.append(c.text).append(Float.isNaN(c.score) ? "(?) " : String.format("(%.1f) ", c.score));
                sb.append(" | sig tras 'ya tengo': ");
                for (Delight.Cand c : Delight.suggest("ya tengo ", "", 6)) if (c.type != 2) sb.append(c.text).append(' ');
                return sb.toString();
            };
            System.out.println("ANTES  " + probe.get());
            if (mode >= 20) {
                String uh = System.getenv("GB_PERSO") + "/UserHistory.dict";
                java.util.List<String[]> ws = java.util.Collections.singletonList(new String[]{"ya tengo", w});
                for (int k = 0; k < times; k++) System.out.println("learn=" + Delight.learn(uh, ws, mode == 21 && k == times - 1));
                System.out.println("DESPUES " + probe.get());
                return;
            }
            int bm = mode % 10;
            if (mode >= 10) Delight.beginSession();
            for (int k = 0; k < times; k++) Delight.commitWord("ya tengo ", w, bm);
            if (mode >= 10) System.out.println("finishSession -> " + gbport.Pb.dump(Delight.finishSession(), "").replace("\n", " ").substring(0, 0));
            System.out.println("DESPUES " + probe.get());
            System.out.println("flush=" + Delight.flushPersonal());
            return;
        }
        if (a.length > 1 && a[1].equals("GEO")) {
            // Swipe en un teclado "real" simulado (1080 de ancho, filas de 150, z indentada 1.5, origen y=1700) con temblor.
            float jit = System.getenv("GB_JIT") == null ? 0.15f : Float.parseFloat(System.getenv("GB_JIT"));
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                System.out.println("GEO [" + t[0] + "] " + t[1] + " -> " + gbport.Geo.testDecode(t[0], t[1], 98f, 150f, 50f, 1700f, 1.5f, jit));
            }
            return;
        }
        if (a.length > 1 && a[1].equals("SWIPESTREAM")) {
            // Trazo por partes (S1: prefijo entero sin levantar; S2: solo los puntos nuevos).
            int mode = System.getenv("GB_SM") == null ? 2 : Integer.parseInt(System.getenv("GB_SM"));
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                float[][] tr = Delight.synthTrace(t[1], 20f);
                int m = tr[0].length, prev = 0;
                StringBuilder sb = new StringBuilder("SWIPESTREAM" + mode + " " + t[1] + ":");
                for (int k = 1; k <= 4; k++) {
                    int c = Math.max(2, m * k / 4);
                    float[] xs = java.util.Arrays.copyOf(tr[0], c), ys = java.util.Arrays.copyOf(tr[1], c);
                    long[] ts = new long[c];
                    for (int j = 0; j < c; j++) ts[j] = (long) tr[2][j];
                    byte[] r = mode == 1 ? Delight.decodeGestureStream(t[0], "", xs, ys, ts, 0, true, k == 4, 16)
                            : Delight.decodeGestureStream(t[0], "", xs, ys, ts, prev, k == 1, k == 4, 8);
                    prev = c;
                    sb.append(" [").append(c).append("]").append(Delight.gestureText(r));
                    if (k == 4) for (Delight.Cand cd : Delight.parseCands(r)) sb.append(" ").append(cd.text);
                    if (k == 4) sb.append(" => ").append(gbport.Swipe.rank(Delight.gestureText(r), Delight.parseCands(r), t[0]));
                }
                System.out.println(sb);
            }
            return;
        }
        if (a.length > 1 && a[1].equals("SWIPEPART")) {
            // Como el teclado durante el trazo: se decodifica el trazo parcial varias veces y al final el completo.
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                float[][] tr = Delight.synthTrace(t[1], 20f);
                int m = tr[0].length;
                StringBuilder sb = new StringBuilder("SWIPEPART " + t[1] + ":");
                for (int k = 1; k <= 4; k++) {
                    int c = Math.max(2, m * k / 4);
                    float[] xs = java.util.Arrays.copyOf(tr[0], c), ys = java.util.Arrays.copyOf(tr[1], c);
                    long[] ts = new long[c];
                    for (int j = 0; j < c; j++) ts[j] = (long) tr[2][j];
                    if (System.getenv("GB_TB") != null) Delight.gestureTimeBase += Long.parseLong(System.getenv("GB_TB"));
                    byte[] r = Delight.decodeGestureRaw(t[0], "", xs, ys, ts, 8);
                    sb.append(" [").append(c).append("]").append(Delight.gestureText(r));
                }
                System.out.println(sb);
            }
            return;
        }
        if (a.length > 1 && a[1].equals("SWIPE")) {
            // Swipe sintético por los centros de las teclas: "antes|palabra".
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                if (System.getenv("GB_MSPT") != null) Delight.synthMsPerPt = Float.parseFloat(System.getenv("GB_MSPT"));
                float[][] tr = Delight.synthTrace(t[1], System.getenv("GB_STEP") == null ? 20f : Float.parseFloat(System.getenv("GB_STEP")));
                long[] ts = new long[tr[2].length];
                for (int j = 0; j < ts.length; j++) ts[j] = (long) tr[2][j];
                byte[] r = Delight.decodeGestureRaw(t[0], "", tr[0], tr[1], ts, 8);
                StringBuilder sb = new StringBuilder("SWIPE [" + t[0] + "] " + t[1] + " (" + ts.length + " pts) => [" + Delight.gestureText(r) + "] ");
                for (Delight.Cand c : Delight.parseCands(r)) sb.append(c.text).append(String.format("(%.1f) ", c.score));
                if (System.getenv("GB_DUMP") != null) sb.append("\n").append(gbport.Pb.dump(r, ""));
                System.out.println(sb);
            }
            return;
        }
        if (a.length > 1 && a[1].equals("CTX")) {
            // Decisión completa con el texto real del campo (puntuación incluida): "antes|palabra|después".
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].replace("\\n", "\n").split("\\|", -1);
                String after = t.length > 2 ? t[2] : "";
                Decide.Result r = Decide.decide(Delight.parseCands(Delight.suggestEchoRaw(t[0], after, t[1], 20)), t[1], false);
                StringBuilder nw = new StringBuilder();
                int k = 0;
                for (Delight.Cand c : Delight.suggest(t[0], after, "", 20)) {
                    if (c.type == 2) continue;
                    nw.append(c.text).append(' ');
                    if (++k == 4) break;
                }
                System.out.println("CTX [" + t[0].replace("\n", "⏎") + "] " + t[1] + " -> " + (r.cand == null ? t[1] : r.cand.text)
                        + " [" + r.why + "] | sig: " + nw);
            }
            return;
        }
        if (a.length > 1 && a[1].equals("MARK")) {
            // Autocorrección que marca el propio motor (campo 3), sin contexto.
            for (int i = 2; i < a.length; i++) {
                String m = "(ninguna)";
                for (Delight.Cand c : Delight.suggest("", a[i], 20)) if (c.auto) { m = c.text; break; }
                System.out.println("MARK " + a[i] + " -> " + m);
            }
            return;
        }
        if (a.length > 1 && a[1].startsWith("TR:")) {
            String[] t = a[1].substring(3).split("\\|", -1);
            for (String s : Delight.trace(t[0], t[1], 6)) System.out.println("TR " + s);
            return;
        }
        if (a.length > 1 && a[1].startsWith("KP:")) {
            // ¿Dónde vienen los candidatos con contexto: en la última pulsación o en decode?
            for (int i = 1; i < a.length; i++) {
                String[] t = a[i].substring(a[i].startsWith("KP:") ? 3 : 0).split("\\|", -1);
                byte[][] r = Delight.suggestBothRaw(t[0], t[1], 8);
                for (int j = 0; j < 2; j++) {
                    StringBuilder sb = new StringBuilder((j == 0 ? "KP  " : "DEC ") + "[" + t[0] + "|" + t[1] + "] ");
                    for (Delight.Cand c : Delight.parseCands(r[j])) {
                        sb.append(c.text).append(String.format("(%.1f)", c.score)).append(' ');
                    }
                    System.out.println(sb);
                }
            }
            return;
        }
        if (a.length > 1 && a[1].startsWith("DUAL:")) {
            String[] t = a[1].substring(5).split("\\|", -1);
            java.util.List<Delight.Cand> c1 = Delight.suggest(t[0], t[1], 6);   // con contexto
            java.util.List<Delight.Cand> c2 = Delight.suggest("", t[1], 6);       // sin contexto (2.ª pasada)
            java.util.List<Delight.Cand> c3 = Delight.suggest(t[0], t[1], 6);   // de nuevo con contexto
            for (String tag : new String[]{"CTX","NOCTX","CTX2"}) {
                java.util.List<Delight.Cand> cc = tag.equals("CTX")?c1:(tag.equals("NOCTX")?c2:c3);
                StringBuilder sb=new StringBuilder(tag+": ");
                for (Delight.Cand c: cc) sb.append(c.text).append('(').append(Float.isNaN(c.score)?"?":String.format("%.1f",c.score)).append(") ");
                System.out.println(sb);
            }
            return;
        }
        if (a.length > 1 && a[1].equals("SEQ")) {
            // Flujo del teclado: tras cada espacio, siguiente palabra con el texto previo; luego corrección
            // de la palabra tecleada CON contexto (eco del campo) y se añade lo que quedaría escrito.
            StringBuilder before = new StringBuilder();
            for (int i = 2; i < a.length; i++) {
                StringBuilder nw = new StringBuilder();
                if (before.length() > 0) {
                    int k = 0;
                    for (Delight.Cand c : Delight.suggest(before.toString(), "", 20)) {
                        if (c.type == 2) continue;
                        nw.append(c.text).append(' ');
                        if (++k == 4) break;
                    }
                }
                String w = a[i];
                Decide.Result r = Decide.decide(Delight.parseCands(Delight.suggestEchoRaw(before.toString(), w, 20)), w, false);
                String got = r.cand == null ? w : r.cand.text;
                System.out.println("SEQ [" + before + "] sugiere: " + (nw.length() == 0 ? "-" : nw) + "| tecleas " + w + " -> " + got + " [" + r.why + "]");
                before.append(got).append(' ');
            }
            return;
        }
        if (a.length > 1 && a[1].startsWith("REPLAY:")) {
            // Medición del dedo: cada palabra registrada (toques.txt) se decide con los centros de las teclas y con los
            // toques reales; se cuenta cuántas veces cada una acaba en lo que confirmaste.
            int n = 0, okC = 0, okR = 0;
            StringBuilder diff = new StringBuilder();
            for (String line : java.nio.file.Files.readAllLines(java.nio.file.Paths.get(a[1].substring(7)), java.nio.charset.StandardCharsets.UTF_8)) {
                String[] t = line.split("\t", -1);
                if (t.length < 4) continue;
                String typed = t[0], comm = t[1], before = t[2].replace('⏎', '\n');
                String[] ps = t[3].split(";");
                if (ps.length != typed.length()) continue;
                float[] xs = new float[ps.length], ys = new float[ps.length];
                for (int i = 0; i < ps.length; i++) {
                    if (ps[i].equals("-")) { xs[i] = Float.NaN; ys[i] = Float.NaN; continue; }
                    String[] xy = ps[i].split(",");
                    xs[i] = Float.parseFloat(xy[0]); ys[i] = Float.parseFloat(xy[1]);
                }
                Decide.Result c = Decide.decide(Delight.parseCands(Delight.suggestEchoRaw(before, "", typed, 20, null, null, null)), typed, false);
                Decide.Result r = Decide.decide(Delight.parseCands(Delight.suggestEchoRaw(before, "", typed, 20, xs, ys, null)), typed, false);
                String gc = c.cand == null ? typed : c.cand.text, gr = r.cand == null ? typed : r.cand.text;
                n++;
                if (gc.equalsIgnoreCase(comm)) okC++;
                if (gr.equalsIgnoreCase(comm)) okR++;
                if (!gc.equals(gr)) diff.append("  ").append(typed).append(" -> centros:").append(gc).append(" dedo:").append(gr).append(" (confirmaste ").append(comm).append(")\n");
            }
            System.out.println("REPLAY palabras=" + n + " aciertos centros=" + okC + " dedo=" + okR);
            System.out.print(diff);
            return;
        }
        if (a.length > 1 && a[1].startsWith("UH")) {
            // Historial: UHDUMP (resumen del volcado), UHGET:ctx|palabra, UHFORGET:palabra|ctx1|ctx2... y luego DEC.
            String uh = System.getenv("GB_PERSO") + "/UserHistory.dict";
            if (a[1].equals("UHDUMP")) {
                byte[] r = Delight.iterateUh(uh);
                System.out.println("iterate bytes=" + (r == null ? -1 : r.length));
                if (r != null) { String d = gbport.Pb.dump(r, ""); System.out.println(d.length() > 3000 ? d.substring(0, 3000) : d); }
                for (int i = 2; i < a.length; i++) if (r != null) System.out.println("contiene " + a[i] + ": " + new String(r, java.nio.charset.StandardCharsets.UTF_8).contains(a[i]));
            } else if (a[1].startsWith("UHGET:")) {
                String[] t = a[1].substring(6).split("\\|", -1);
                byte[] r = Delight.getNgram(uh, t[0], t[1]);
                System.out.println("get " + (r == null ? "null" : gbport.Pb.dump(r, "  ")));
            } else if (a[1].startsWith("UHFORGET:")) {
                String[] t = a[1].substring(9).split("\\|", -1);
                java.util.List<String[]> ng = new java.util.ArrayList<>();
                ng.add(new String[]{"", t[0]});
                for (int k = 1; k < t.length; k++) ng.add(new String[]{t[k], t[0]});
                System.out.println("forget=" + Delight.forget(uh, ng, true));
            }
            for (int i = 2; i < a.length; i++) {
                String w = a[i];
                Decide.Result r = Decide.decide(Delight.parseCands(Delight.suggestEchoRaw("", w, 20)), w, false);
                System.out.println("DEC " + w + " -> " + (r.cand == null ? "(igual)" : r.cand.text) + " [" + r.why + "] lit=" + r.litScore);
            }
            return;
        }
        if (a.length > 1 && a[1].equals("BOTH")) {
            // Mis reglas (Decide) frente a la autocorrección del motor, y la mezcla.
            int diff = 0;
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                Decide.Result r = Decide.decide(Delight.parseCands(Delight.suggestEchoRaw(t[0], "", t[1], 20)), t[1], false);
                String eng = Delight.typeAndSpace(t[0], t[1], true, null, null)[0];
                String mine = r.cand == null ? t[1] : r.cand.text, h = Decide.hybrid(t[1], eng, r);
                boolean d = !mine.equals(h);
                if (d) diff++;
                System.out.println((d ? "BOTH* " : "BOTH  ") + "[" + t[0] + "] " + t[1] + "  reglas=" + mine + "  motor=" + eng.trim() + "  => " + h);
            }
            System.out.println("BOTH cambios=" + diff);
            return;
        }
        if (a.length > 1 && a[1].equals("SPACE2")) {
            boolean dec = System.getenv("GB_DEC") != null;
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                long t0 = System.nanoTime();
                String[] r = Delight.typeAndSpace(t[0], t[1], dec, null, null);
                long t1 = System.nanoTime();
                Delight.suggestEchoRaw(t[0], "", t[1], 20);
                long t2 = System.nanoTime();
                System.out.println("SPACE2 [" + t[0] + "] " + t[1] + " => '" + r[0] + "' (" + (t1 - t0) / 1000000 + " ms; eco " + (t2 - t1) / 1000000 + " ms)");
                if (System.getenv("GB_DUMP") != null) System.out.println(r[1]);
            }
            return;
        }
        if (a.length > 1 && a[1].equals("SPACE")) {
            // Lo que hace Gboard: tras teclear la palabra se manda el espacio al motor y se lee qué confirma.
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                Delight.suggestEchoRaw(t[0], "", t[1], 20);
                byte[] k = Delight.keyPress(32, 540, 160 * 3.5f);
                gbport.Pb.F st = gbport.Pb.one(gbport.Pb.parse(k), 2);
                gbport.Pb.F ed = st == null ? null : gbport.Pb.one(gbport.Pb.parse(st.d), 2);
                String commit = "-", comp = "-";
                if (ed != null) {
                    java.util.List<gbport.Pb.F> ef = gbport.Pb.parse(ed.d);
                    gbport.Pb.F c3 = gbport.Pb.one(ef, 3), c6 = gbport.Pb.one(ef, 6);
                    if (c3 != null) commit = c3.str();
                    if (c6 != null) comp = c6.str();
                }
                System.out.println("SPACE [" + t[0] + "] " + t[1] + " => confirma='" + commit + "' compone='" + comp + "'");
                if (System.getenv("GB_DUMP") != null) System.out.println(gbport.Pb.dump(k, "  "));
            }
            return;
        }
        if (a.length > 1 && a[1].equals("AUTO")) {
            // Marca de autocorrección del propio motor (campo 3) frente a nuestra decisión: "antes|palabra".
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                java.util.List<Delight.Cand> cs = Delight.parseCands(Delight.suggestEchoRaw(t[0], "", t[1], 20));
                Decide.Result r = Decide.decide(cs, t[1], false);
                StringBuilder sb = new StringBuilder("AUTO [" + t[0] + "] " + t[1] + " decide=" + (r.cand == null ? "-" : r.cand.text) + " motor=");
                String m = "-";
                for (Delight.Cand c : cs) if (c.auto) { m = c.text; break; }
                sb.append(m);
                System.out.println(sb);
            }
            return;
        }
        if (a.length > 1 && a[1].equals("DECL")) {
            // Decide con la palabra marcada como aprendida por Samsung.
            for (int i = 2; i < a.length; i++) {
                String[] t = a[i].split("\\|", -1);
                Decide.Result r = Decide.decide(Delight.parseCands(Delight.suggestEchoRaw(t[0], "", t[1], 20)), t[1], true);
                System.out.println("DECL [" + t[0] + "] " + t[1] + " -> " + (r.cand == null ? "(igual)" : r.cand.text) + " [" + r.why + "]");
            }
            return;
        }
        if (a.length > 1 && a[1].equals("CAPS")) {
            // Mayúscula automática (Caps) sobre textos de prueba; "_" marca el cursor al final.
            String[] ts = {"", "¿", "Hola. ¿", "Hola. ¡¿", "Oye, ¿", "Ya. «", "“", "Dijo: \"", "hola ¿", "Fin.\n¿", "(", "Hola. ", "hola", "Hola ¡", "dijo «", "Hola «¿"};
            for (String t : ts) {
                int r = gbport.Caps.caps(t, t.length(), android.text.TextUtils.CAP_MODE_SENTENCES);
                System.out.println("CAPS [" + t.replace("\n", "⏎") + "_] -> " + (r != 0 ? "MAYÚS" : "minús"));
            }
            return;
        }
        if (a.length > 1 && a[1].equals("DEC")) {
            // Política completa de autocorrección (Decide) sobre cada palabra, sin contexto, como en el teclado.
            for (int i = 2; i < a.length; i++) {
                String w = a[i];
                Decide.Result r = Decide.decide(Delight.parseCands(Delight.suggestEchoRaw("", w, 20)), w, false);
                System.out.println("DEC " + w + " -> " + (r.cand == null ? "(igual)" : r.cand.text) + " [" + r.why + "]");
            }
            return;
        }
        if (a.length > 1 && a[1].startsWith("CW:")) {
            // checkWords de Gboard: awzb{1: awza{1,2}, 2: awyi{1,2}} -> awzd{1: [bool], 2: [bool]}
            for (String w : a[1].substring(3).split(",")) {
                byte[] ra = com.google.android.keyboard.client.delight5.Decoder.checkWordsNative(
                        gbport.Pb.n().m(1, gbport.Pb.n().s(1, "").s(2, w)).bytes());
                byte[] rb = com.google.android.keyboard.client.delight5.Decoder.checkWordsNative(
                        gbport.Pb.n().m(1, gbport.Pb.n().s(1, w).s(2, "")).bytes());
                byte[] rc = com.google.android.keyboard.client.delight5.Decoder.checkWordsNative(
                        gbport.Pb.n().m(1, gbport.Pb.n().s(1, w).s(2, "")).m(2, gbport.Pb.n().b(1, true).b(2, true)).bytes());
                System.out.println("CW " + w + " | A=" + hex(ra) + " | B=" + hex(rb) + " | C=" + hex(rc));
            }
            return;
        }
        if (a.length > 1 && a[1].startsWith("NW:")) {
            String ctx = a[1].substring(3);
            // A: contexto como texto + decode con muchos resultados
            StringBuilder sa = new StringBuilder("A(ctx-texto,n=20): ");
            for (Delight.Cand c : Delight.suggest(ctx, "", System.getenv("GB_N") == null ? 20 : Integer.parseInt(System.getenv("GB_N")))) sa.append(c.text).append("[t").append(c.type).append("] ");
            System.out.println(sa);
            // B: teclear el texto con espacios y decodificar
            byte[] r = Delight.typeThenDecodeRaw(ctx, 20);
            StringBuilder sb = new StringBuilder("B(tecleado): ");
            for (Delight.Cand c : Delight.parseCands(r)) sb.append(c.text).append("[t").append(c.type).append("] ");
            System.out.println(sb);
            if (a.length > 2) System.out.println(gbport.Pb.dump(r, ""));
            return;
        }
        if (a.length > 1 && a[1].startsWith("RAW:")) {
            String[] t = a[1].substring(4).split("\\|", -1);
            System.out.println(gbport.Pb.dump(Delight.suggestRaw(t[0], t[1], 4), ""));
            return;
        }
        for (int i = 1; i < a.length; i++) {
            String[] t = a[i].split("\\|", -1);
            long t0 = System.nanoTime();
            List<Delight.Cand> cs = Delight.suggest(t[0], t[1], 6);
            long us = (System.nanoTime() - t0) / 1000;
            StringBuilder sb = new StringBuilder();
            for (Delight.Cand c : cs) sb.append(c.text).append("[t").append(c.type).append(Float.isNaN(c.score) ? "" : String.format(",%.1f", c.score))
                    .append(System.getenv("GB_SPLIT") == null ? "" : String.format(" sp%.1f lm%.1f", c.spatial, c.lm)).append("] ");
            System.out.println("[" + t[0] + "|" + t[1] + "] " + us + "us -> " + sb);
        }
    }
}
