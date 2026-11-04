package gbport;

import android.util.Log;
import com.microsoft.fluency.Point;
import com.microsoft.fluency.Prediction;
import com.microsoft.fluency.Predictions;
import com.microsoft.fluency.Predictor;
import com.microsoft.fluency.ResultsFilter;
import com.microsoft.fluency.Sequence;
import com.microsoft.fluency.Term;
import com.microsoft.fluency.TouchHistory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Puente Samsung Keyboard -> motor Delight5 de Gboard. Samsung llama aquí en vez de Predictor.getPredictions.
// En campos compatibles Delight aporta los candidatos; durante carga o en entradas no compatibles se conserva Fluency.
public final class Bridge {
    static final String TAG = "GboardPort";
    // Prueba de vida: si esto sale en logcat, el parche está activo y la clase se cargó dentro del teclado.
    static { Log.i(TAG, "Bridge cargado (parche activo, odex aceptado)"); }
    static volatile String DIR; // Se configura desde el servicio del perfil actual, nunca desde el perfil 0.
    static final String[][] LMS = {{"es_us.dict", "es", "US"}, {"en_us.dict", "en", "US"}};
    // Modelo espacial: posición real del dedo de cada letra, pasada al marco del layout del motor con el ajuste por
    // filas (teclas reales de Samsung o, si no llegan, la media de tus propios toques). Sin calibración: centros.
    static final boolean SPATIAL = true;
    // Predicción de la siguiente palabra con Gboard (tras espacio). Si Gboard solo devuelve puntuación,
    // se deja la de Samsung.
    static final boolean NEXT_WORD = true;
    // Swipe con el decodificador de gestos de Gboard (si Samsung ya dio las posiciones de las teclas).
    static final boolean SWIPE = true;

    static volatile int state; // 0 sin contexto/iniciar, 1 cargando, 2 listo, -1 esperando reintento
    static volatile long retryAtNanos;
    static final long RETRY_NANOS = 5_000_000_000L;
    static volatile Object input;
    static final java.util.concurrent.atomic.AtomicLong inputGeneration = new java.util.concurrent.atomic.AtomicLong();
    // Campo actual (enganche en HoneyBoardService.onStartInput): servicio para leer el texto real del campo,
    // app y tipo de campo para los metadatos de sesión de Gboard.
    static volatile java.lang.ref.WeakReference<android.inputmethodservice.InputMethodService> svc;
    static volatile String pkg = "";
    static volatile boolean search, secret, email;
    // Texto del campo alrededor del cursor que se le pasa al motor (el motor poda a 40 caracteres de todos modos).
    static final int CTX_BEFORE = 300, CTX_AFTER = 60;
    static long lastFlush;
    // Aprendizaje: palabras confirmadas (con sus 2 previas) pendientes de escribir en el historial.
    static final List<String[]> pendingLearn = new ArrayList<>();
    static volatile String lastLearnKey = "";
    static final java.util.regex.Pattern WORD = java.util.regex.Pattern.compile("[\\p{L}][\\p{L}'’-]*");
    // Decisión de autocorrección de Gboard para la última palabra, que aj.j.k debe respetar.
    static volatile String decWord;
    static volatile String correctionText;
    static volatile int decResult; // 1 = corregir (forzar commit), 0 = mantener lo tecleado

    // Llamado al inicio de aj.j.k: impone la decisión de Gboard. 1 = autocorregir, 0 = no, -1 = que decida Samsung.
    public static int decideAutoReplace() {
        if (state != 2 || secret) return -1;
        try {
            String w = composing();
            if (w != null && w.equals(decWord)) {
                GLog.w("k: comp=[" + w + "] == decWord -> " + decResult);
                if (decResult == 1 && !secret) GLog.w("aplicar: '" + w + "' (Samsung pregunta, corregir=sí)");
                return decResult;
            }
            GLog.w("k: comp=[" + w + "] != decWord=[" + decWord + "], se conserva la entrada si Delight está activo");
            if (decWord != null && !decWord.isEmpty() && !secret) GLog.w("aplicar: composición '" + w + "' != decidida '" + decWord + "', se conserva lo tecleado");
        } catch (Throwable e) {
            // ignorar
        }
        return delightOwns(composing()) ? 0 : -1;
    }

    static int lastType = -1;

    // Igual, con la lista de sugerencias que Samsung usará para reemplazar (aj.j.k p1). Si nuestra decisión va una letra
    // por detrás (29 % de las veces: la sugerencia de la última letra aún no había llegado al pulsar espacio), se decide
    // ya para la palabra real; se corrige solo si la primera sugerencia de Samsung es nuestra corrección, si no se deja.
    public static int decideAutoReplace(List<?> preds) {
        if (state != 2 || secret) return -1;
        try {
            String w = composing();
            if (w == null || w.isEmpty() || w.equals(decWord)) return decideAutoReplace();
            final long generation = inputGeneration.get();
            final String before = fieldBefore(w), after = fieldAfter();
            if (before == null || secret) return decideAutoReplace();
            java.util.concurrent.Future<String> fut = ENGINE.submit(() -> {
                List<Delight.Cand> cs = Delight.parseCands(Delight.suggestEchoRaw(before, after, w, 20));
                Decide.Result dec = Decide.decide(cs, w, false);
                String corr = dec.cand != null ? dec.cand.text : null;
                String q = Decide.interrogative(before, w);
                if (q != null && !q.equals(w)) corr = q;
                String base = corr != null ? corr : w;
                if (Character.isLowerCase(base.charAt(0)) && !before.isEmpty() && Caps.OPENERS.indexOf(before.charAt(before.length() - 1)) >= 0
                        && (lastType & android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0
                        && Caps.openerCaps(before, before.length(), android.text.TextUtils.CAP_MODE_SENTENCES) != 0)
                    corr = Character.toUpperCase(base.charAt(0)) + base.substring(1);
                return corr == null ? "" : corr;
            });
            String corr;
            try { corr = fut.get(120, java.util.concurrent.TimeUnit.MILLISECONDS); }
            catch (java.util.concurrent.TimeoutException e) { fut.cancel(false); GLog.w("aplicar: '" + w + "' sin tiempo, se conserva lo tecleado"); return 0; }
            if (!predictionCurrent(generation, w)) return 0;
            if (corr.isEmpty()) { GLog.w("aplicar: '" + w + "' al momento: se deja"); return 0; }
            Object top = preds == null || preds.isEmpty() ? null : preds.get(0);
            String t = top instanceof Prediction ? ((Prediction) top).getPrediction() : null;
            if (corr.equals(t)) { GLog.w("aplicar: '" + w + "' al momento -> " + corr); return 1; }
            GLog.w("aplicar: '" + w + "' al momento -> " + corr + " pero Samsung tiene '" + t + "': se deja");
            return 0;
        } catch (Throwable e) {
            return delightOwns(composing()) ? 0 : -1;
        }
    }

    private Bridge() {}

    static int inputTypeFor(String app, int type) {
        int variation = type & android.text.InputType.TYPE_MASK_VARIATION;
        if ("com.google.android.googlequicksearchbox".equals(app)
                && (type & android.text.InputType.TYPE_MASK_CLASS) == android.text.InputType.TYPE_CLASS_TEXT
                && variation != 0x80 && variation != 0x90 && variation != 0xe0)
            return (type & ~android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
                    | android.text.InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
        return type;
    }

    // Llamado al inicio de HoneyBoardService.onStartInput: nuevo campo de texto.
    public static void onStartInput(android.inputmethodservice.InputMethodService s, android.view.inputmethod.EditorInfo ei) {
        try {
            resetInputSession();
            if (ei != null) ei.inputType = inputTypeFor(ei.packageName, ei.inputType);
            if (s != null) configureDirectory(s.getFilesDir());
            GLog.enabled = DIR != null && new File(DIR, "debug").isFile();
            svc = new java.lang.ref.WeakReference<>(s);
            localEditorText = null;
            if (!Ai.registered && s != null) Ai.register(s.getApplicationContext());
            else if (Ai.registered) Ai.addStyles();     // Samsung vacía la lista al recargar sus estilos del servidor
            int it = ei == null ? 0 : ei.inputType;
            int cls = it & android.text.InputType.TYPE_MASK_CLASS, var = it & android.text.InputType.TYPE_MASK_VARIATION;
            secret = (cls == android.text.InputType.TYPE_CLASS_TEXT && (var == 0x80 || var == 0x90 || var == 0xe0))
                    || (cls == android.text.InputType.TYPE_CLASS_NUMBER && var == 0x10);
            int action = ei == null ? 0 : ei.imeOptions & android.view.inputmethod.EditorInfo.IME_MASK_ACTION;
            search = action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH || (cls == android.text.InputType.TYPE_CLASS_TEXT && var == 0xb0);
            email = cls == android.text.InputType.TYPE_CLASS_TEXT && (var == 0x20 || var == 0xd0);
            String p = ei == null || ei.packageName == null ? "" : ei.packageName;
            boolean changed = !p.equals(pkg) || it != lastType;
            lastType = it;
            pkg = p;
            if (state == 2) {
                final boolean sr = search, em = email;
                final long session = inputGeneration.get();
                lastTypeMs = System.currentTimeMillis();
                synchronized (pendingLearn) {
                    if (!pendingLearn.isEmpty()) scheduleLearn();
                }
                ENGINE.execute(() -> {
                    if (session != inputGeneration.get()) return;
                    try {
                        Split.clearCache();
                        Delight.setRuntimeParams(NEXT_WORD, p, sr, em);
                        long now = System.currentTimeMillis();
                        if (now - lastFlush > 5 * 60_000L) { lastFlush = now; Delight.flushPersonal(); }
                    } catch (Throwable e) { Log.e(TAG, "parámetros de sesión", e); }
                });
            } else {
                start();
            }
            EditCommit.onInput(s, ei);
            if (changed && !secret) GLog.w("campo app=" + p + " tipo=0x" + Integer.toHexString(it) + " ime=0x"
                    + Integer.toHexString(ei == null ? 0 : ei.imeOptions) + (search ? " busqueda" : ""));
        } catch (Throwable e) {
            // nunca romper el teclado
        }
    }

    // Tras confirmar una palabra (campo termina en separador): la apunta para el historial con sus 2 previas.
    static void noteCommitted(String before) {
        if (secret || before == null || before.isEmpty()) return;
        char last = before.charAt(before.length() - 1);
        if (Character.isLetterOrDigit(last)) return;
        String tail = before.length() > 200 ? before.substring(before.length() - 200) : before;
        // Solo la frase actual: el contexto no cruza puntos ni saltos de línea.
        int cut = Math.max(Math.max(tail.lastIndexOf('.', tail.length() - 2), tail.lastIndexOf('\n', tail.length() - 2)),
                Math.max(tail.lastIndexOf('?', tail.length() - 2), tail.lastIndexOf('!', tail.length() - 2)));
        String sentence = tail.substring(cut + 1);
        java.util.regex.Matcher m = WORD.matcher(sentence);
        List<String> ws = new ArrayList<>();
        while (m.find()) ws.add(m.group());
        if (ws.isEmpty()) return;
        String w = ws.get(ws.size() - 1);
        // Sin URLs, correos ni palabras con cifras (el patrón ya excluye cifras; esto filtra restos de enlaces).
        if (tail.endsWith(w + "@") || tail.contains("://" + w)) return;
        // Clave = dónde acaba la palabra: "segunda," y luego "segunda, " (o borrar y reescribir ahí) es una sola vez.
        int end = before.length();
        while (end > 0 && !Character.isLetter(before.charAt(end - 1))) end--;
        String key = end + "|" + tail.toLowerCase(Locale.ROOT).stripTrailing();
        if (key.equals(lastLearnKey)) return;
        lastLearnKey = key;
        recordTouches(w);
        // Como Gboard: la mayúscula de inicio de frase la pone el teclado, se aprende la palabra (Coño -> coño).
        if (ws.size() == 1 && w.length() > 1 && Character.isUpperCase(w.charAt(0)) && !Decide.innerUpper(w)
                && w.substring(1).equals(w.substring(1).toLowerCase(Locale.ROOT)))
            w = w.toLowerCase(Locale.ROOT);
        if (!learnable(w)) return;
        String ctx = ws.size() >= 3 ? ws.get(ws.size() - 3) + " " + ws.get(ws.size() - 2) : (ws.size() == 2 ? ws.get(0) : "");
        synchronized (pendingLearn) { pendingLearn.add(new String[]{ctx, w}); }
        scheduleLearn();
    }

    // Lo que el motor no conocía al teclearlo (lit NaN o de no-palabra) y se dejó sin corregir: como Gboard, se aprende
    // solo si se vuelve a escribir; si no, una errata (Aracnid, agresivs) quedaría aprendida y ya nunca se corregiría.
    static final Map<String, Double> litSeen = java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String, Double>(256, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, Double> e) { return size() > 400; }
    });
    static final Map<String, String> fixSeen = java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String, String>(64, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, String> e) { return size() > 300; }
    });
    static final Map<String, Integer> oovUses = java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String, Integer>(64, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, Integer> e) { return size() > 300; }
    });

    // Última palabra tecleada con toques reales {antes, tecleada, xs, ys}; al confirmarla se guarda con lo confirmado.
    static volatile Object[] lastTouch;

    static void recordTouches(String committed) {
        Object[] t = lastTouch;
        lastTouch = null;
        if (t == null || secret) return;
        String typed = (String) t[1];
        // Solo si lo confirmado sale de lo tecleado (misma palabra o su corrección), no de una sugerencia sin relación.
        if (typed.length() < 2 || Math.abs(typed.length() - committed.length()) > 2) return;
        float[] xs = (float[]) t[2], ys = (float[]) t[3];
        StringBuilder sb = new StringBuilder();
        sb.append(typed).append('\t').append(committed).append('\t').append(((String) t[0]).replace('\t', ' ').replace('\n', '⏎'));
        sb.append('\t');
        for (int i = 0; i < xs.length; i++) sb.append(i == 0 ? "" : ";").append(Float.isNaN(xs[i]) ? "-" : String.format(Locale.US, "%.0f,%.0f", xs[i], ys[i]));
        GLog.touches(sb.toString());
    }

    static boolean learnable(String w) {
        int n = w.codePointCount(0, w.length());
        // Letras sueltas que no son palabra (p, t, s) no.
        if (n == 1) return "ayoeuAYOEU".indexOf(w.charAt(0)) >= 0;
        // Alargamientos (Profeeeee, nooooo): Gboard no los mete en las sugerencias.
        for (int i = 2; i < w.length(); i++)
            if (Character.toLowerCase(w.charAt(i)) == Character.toLowerCase(w.charAt(i - 1))
                    && Character.toLowerCase(w.charAt(i)) == Character.toLowerCase(w.charAt(i - 2))) return false;
        Double lit = litSeen.get(w);
        if (lit == null) { String c = Character.toUpperCase(w.charAt(0)) + w.substring(1); lit = litSeen.get(c); }
        String previousCorrection = fixSeen.get(w.toLowerCase(Locale.ROOT));
        if (previousCorrection != null && previousCorrection.contains(" ")) return false;
        if (lit == null) return false;
        if (lit.isNaN() || lit < Decide.OOV_LITERAL) {
            String k = w.toLowerCase(Locale.ROOT);
            Integer u = oovUses.get(k);
            int uses = u == null ? 1 : u + 1;
            oovUses.put(k, uses);
            // Si el motor tenía una corrección para ella (errata probable), hace falta escribirla 3 veces; si no (nombres,
            // jerga), 2.
            int need = fixSeen.containsKey(k) ? 3 : 2;
            if (uses < need) { GLog.w("aprendizaje: '" + w + "' no es palabra para el motor; se aprende si la escribes " + need + " veces (" + uses + ")"); return false; }
        }
        return true;
    }

    // Aprender recarga el historial en el hilo del motor (~0,3 s): se hace cuando dejas de escribir 2 s, no a mitad
    // de frase (cada "aprendido" iba seguido de "motor lento, uso Samsung").
    static volatile long lastTypeMs;
    static volatile boolean learningNow;
    // Pausa sin teclear antes de aprender: con 2 s aún salían "motor lento" (17 en 25 min) al retomar la escritura.
    static final long LEARN_IDLE_MS = 4000;
    static final java.util.concurrent.ScheduledExecutorService LEARN_TIMER = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "GboardPort-learn");
        t.setDaemon(true);
        return t;
    });
    static final java.util.concurrent.atomic.AtomicBoolean learnScheduled = new java.util.concurrent.atomic.AtomicBoolean();

    static void scheduleLearn() {
        if (!learnScheduled.compareAndSet(false, true)) return;
        LEARN_TIMER.schedule(Bridge::learnWhenIdle, 2, java.util.concurrent.TimeUnit.SECONDS);
    }

    static void learnWhenIdle() {
        long idle = System.currentTimeMillis() - lastTypeMs;
        if (idle < LEARN_IDLE_MS) { LEARN_TIMER.schedule(Bridge::learnWhenIdle, LEARN_IDLE_MS - idle + 50, java.util.concurrent.TimeUnit.MILLISECONDS); return; }
        learnScheduled.set(false);
        flushLearn();
    }

    // Escribe lo pendiente en el historial (hilo aparte) y recarga el modelo.
    static void flushLearn() {
        final List<String[]> batch;
        synchronized (pendingLearn) {
            if (pendingLearn.isEmpty()) return;
            batch = new ArrayList<>(pendingLearn);
            pendingLearn.clear();
        }
        ENGINE.execute(() -> {
            if (System.currentTimeMillis() - lastTypeMs < LEARN_IDLE_MS) {
                synchronized (pendingLearn) { pendingLearn.addAll(0, batch); }
                scheduleLearn();
                return;
            }
            try {
                long t0 = android.os.SystemClock.uptimeMillis();
                learningNow = true;
                boolean ok;
                try { ok = Delight.learn(DIR + "/personal/UserHistory.dict", batch, true); if (ok) Split.clearCache(); } finally { learningNow = false; }
                StringBuilder sb = new StringBuilder();
                for (String[] b : batch) sb.append(sb.length() == 0 ? "" : ", ").append(b[1]);
                GLog.w("aprendido(" + ok + ", " + (android.os.SystemClock.uptimeMillis() - t0) + " ms): " + sb);
            } catch (Throwable e) { Log.e(TAG, "aprendizaje", e); }
        });
    }

    // Palabras a olvidar del historial (erratas aprendidas): files/gbport/olvidar.txt, una por línea. Se procesa al
    // arrancar el motor, antes de cargar el historial, y el fichero pasa a olvidar.hecho.
    static void forgetPending(String uh) {
        File f = new File(DIR, "olvidar.txt");
        if (!f.exists()) return;
        try {
            List<String[]> ng = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            for (String line : java.nio.file.Files.readAllLines(f.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                String w = line.trim();
                if (w.isEmpty() || w.startsWith("#")) continue;
                ng.add(new String[]{"", w});
                sb.append(sb.length() == 0 ? "" : ", ").append(w);
            }
            boolean ok = !ng.isEmpty() && Delight.forget(uh, ng, false);
            GLog.w("olvidadas(" + ok + "): " + sb);
            f.renameTo(new File(DIR, "olvidar.hecho"));
        } catch (Throwable e) { Log.e(TAG, "olvidar", e); }
    }

    // ---- Deshacer/rehacer con dos dedos (Cp.f, gestos de Samsung/Keys Cafe) ----
    // Samsung los detecta con GestureDetector.onFling: un deslizamiento lento de dos dedos no llega a la velocidad mínima
    // de "fling" y no hace nada. Se baja esa velocidad a un tercio (llamado al final del constructor de Cp.f).
    public static void tuneGestures(Object f) {
        try {
            java.lang.reflect.Field fg = f.getClass().getDeclaredField("g");
            fg.setAccessible(true);
            Object d = fg.get(f);
            java.lang.reflect.Field fb = d.getClass().getDeclaredField("b");
            fb.setAccessible(true);
            Object det = fb.get(d);
            if (!(det instanceof android.view.GestureDetector)) return;
            java.lang.reflect.Field mv = android.view.GestureDetector.class.getDeclaredField("mMinimumFlingVelocity");
            mv.setAccessible(true);
            int old = mv.getInt(det), nv = Math.max(40, old / 3);
            mv.setInt(det, nv);
            if (!gestureLogged) { gestureLogged = true; GLog.w("gestos dos dedos: velocidad mínima " + old + " -> " + nv); }
        } catch (Throwable e) {
            if (!gestureLogged) { gestureLogged = true; GLog.w("gestos dos dedos: no se pudo ajustar (" + e + ")"); }
        }
    }

    static volatile boolean gestureLogged;

    // Tras "¿" Samsung no recalcula el Shift (tras "¡" sí: el registro mostró "mayúscula tras '¡'" pero nada tras "¿").
    // Si el texto termina en un signo de apertura que pide mayúscula (Caps) y el Shift está apagado, se enciende como lo
    // haría Samsung (n9.b.l(1)), en el hilo de la interfaz. Solo en campos que piden mayúsculas.
    static int shiftPos = -1;

    // Repinta la tecla Shift y las letras como hace Samsung tras cambiar el Shift: evento Wp.b.b
    // ("keyboard_view_update_keyboard_shift_view") en su bus Wp.a. Sin esto el valor cambia pero se sigue viendo y
    // escribiendo en minúscula (16:42:29).
    static void refreshShiftView() {
        try {
            Object bus = Class.forName("vj.h").getMethod("f", Class.class).invoke(null, Class.forName("Wp.a"));
            java.lang.reflect.Field ev = Class.forName("Wp.b").getDeclaredField("b");
            ev.setAccessible(true);
            bus.getClass().getMethod("b", ev.getType(), Object.class).invoke(bus, ev.get(null), 10);
        } catch (Throwable e) {
            Log.w(TAG, "repintar shift", e);
        }
    }

    static void shiftAfterOpener(String before) {
        try {
            if (before == null || before.isEmpty() || secret) return;
            char last = before.charAt(before.length() - 1);
            if (Caps.OPENERS.indexOf(last) < 0) return;
            if ((lastType & (android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS)) == 0) return;
            if (Caps.openerCaps(before, before.length(), android.text.TextUtils.CAP_MODE_SENTENCES) == 0) return;
            // Se mira ya y otra vez tras volver de la página de símbolos: Samsung calcula bien la mayúscula al volver
            // (16:40:33 "mayúscula tras '¿'") pero al redibujar el teclado de letras la apaga (l(0) en n9.b.s()).
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            for (long delay : new long[]{0, 150, 400, 800, 1300}) {
                h.postDelayed(() -> {
                    try {
                        String b = fieldBefore(null);
                        if (b == null || b.isEmpty() || b.charAt(b.length() - 1) != last) return;
                        if (shiftState() != 0) return;
                        shiftObj.getClass().getMethod("l", int.class).invoke(shiftObj, 1);
                        refreshShiftView();
                        GLog.w("shift tras '" + last + "' (" + delay + " ms)");
                    } catch (Throwable e) { Log.w(TAG, "shift", e); }
                }, delay);
            }
        } catch (Throwable e) {
            // nunca romper el teclado
        }
    }

    // Estado de Shift de Samsung (n9.b.d(), el mismo que lee Lo.b): 0 normal, 1 mayúscula siguiente, 2 bloqueo. -1 si no.
    static java.lang.reflect.Method shiftGet;
    static Object shiftObj;

    static int shiftState() {
        try {
            if (shiftGet == null) {
                java.lang.reflect.Field fb = Class.forName("Lo.b").getDeclaredField("b");
                fb.setAccessible(true);
                shiftObj = fb.get(null);
                shiftGet = shiftObj.getClass().getMethod("d");
            }
            return ((Number) shiftGet.invoke(shiftObj)).intValue();
        } catch (Throwable e) {
            return -1;
        }
    }

    // ---- Comillas latinas al mantener < y > (como Gboard) ----
    // KeyVO.getNormalBubbles() (caracteres de la pulsación larga) pasa por aquí: a < se le añade « y a > », primeras.
    static final java.util.Map<Object, List<?>> bubbleCache = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    static java.lang.reflect.Method mNormalKey, mKeyCodeLabel, mKeyCode, mLabelSize, mLabel;
    static java.lang.reflect.Constructor<?> cKcl;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List bubbles(Object key, List list) {
        try {
            if (key == null || list == null) return list;
            List<?> cached = bubbleCache.get(key);
            if (cached != null) return cached;
            if (mNormalKey == null) {
                mNormalKey = key.getClass().getMethod("getNormalKey");
                Class<?> lk = mNormalKey.getReturnType();
                mKeyCodeLabel = lk.getMethod("getKeyCodeLabel");
                Class<?> kcl = mKeyCodeLabel.getReturnType();
                mKeyCode = kcl.getMethod("getKeyCode");
                mLabelSize = kcl.getMethod("getKeyLabelSize");
                mLabel = kcl.getMethod("getKeyLabel");
                cKcl = kcl.getConstructor(String.class, List.class, float.class);
            }
            Object nk = mNormalKey.invoke(key);
            Object kcl = nk == null ? null : mKeyCodeLabel.invoke(nk);
            int code = kcl == null ? 0 : (Integer) mKeyCode.invoke(kcl);
            String add = code == '<' ? "«" : code == '>' ? "»" : null;
            List out = list;
            if (add != null) {
                boolean has = false;
                for (Object b : list) if (add.equals(mLabel.invoke(b))) { has = true; break; }
                if (!has) {
                    float size = list.isEmpty() ? (Float) mLabelSize.invoke(kcl) : (Float) mLabelSize.invoke(list.get(0));
                    out = new ArrayList(list.size() + 1);
                    out.add(cKcl.newInstance(add, java.util.Collections.singletonList((int) add.charAt(0)), size));
                    out.addAll(list);
                    GLog.w("pulsación larga de '" + (char) code + "': añadida " + add);
                }
            }
            bubbleCache.put(key, out);
            return out;
        } catch (Throwable e) {
            return list;
        }
    }

    // ---- Mayúscula automática tras signos de apertura ----
    // Samsung decide el shift con TextUtils.getCapsMode (V7.a.getCursorCapsMode), que solo salta comillas rectas y
    // paréntesis. Como Gboard, al inicio de frase también cuentan ¿ ¡ « “ ‘ ‹ „: "¿hola" -> "¿Hola", "Ya. «buenas" ->
    // "Ya. «Buenas". A mitad de frase no ("Oye, ¿qué haces?" va en minúscula en español).
    // Sustituye a TextUtils.getCapsMode en el búfer local de Samsung (sin IPC).
    public static int caps(CharSequence cs, int off, int req) {
        return Caps.caps(cs, off, req);
    }

    // Cuando Samsung pregunta a la app (búfer local no válido): se mira el texto del campo.
    public static int capsIc(int r, int req) {
        if (r != 0 || (req & android.text.TextUtils.CAP_MODE_SENTENCES) == 0) return r;
        try {
            String t = fieldBefore(null);
            return t == null ? r : Caps.openerCaps(t, t.length(), req);
        } catch (Throwable e) { return r; }
    }

    // Texto real del campo antes del cursor, sin la palabra en curso (con puntuación, mayúsculas y saltos de línea).
    // null si no hay campo; entonces se usa la secuencia de Samsung.
    static volatile LocalEditorText localEditorText;

    // Called before Samsung enqueues its builder and starts its UI wait loop.
    public static void captureLocalEditor() {
        if (!"com.samsung.android.honeyboard".equals(pkg)) return;
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) localEditorText = Ai.localText();
    }

    static LocalEditorText builderText() {
        LocalEditorText text = localEditorText;
        return "com.samsung.android.honeyboard".equals(pkg)
                && "EngineBuilderThread".equals(Thread.currentThread().getName())
                && text != null && System.nanoTime() - text.capturedAt < 1_000_000_000L ? text : null;
    }

    public static CharSequence localTextBefore(int count, int flags) {
        LocalEditorText text = builderText();
        return text == null ? null : text.read(true, count, flags);
    }

    public static CharSequence localTextAfter(int count, int flags) {
        LocalEditorText text = builderText();
        return text == null ? null : text.read(false, count, flags);
    }

    static EditorContext localContext(String word) {
        LocalEditorText text = builderText();
        return text == null ? Ai.localContext(word) : text.context(word);
    }

    static String fieldBefore(String word) {
        if ("com.samsung.android.honeyboard".equals(pkg)) {
            EditorContext local = localContext(word);
            return local == null ? null : local.before;
        }
        EditorContext fields = readContext(word);
        return fields == null ? null : fields.before;
    }

    static EditorContext readContext(String word) {
        try {
            android.inputmethodservice.InputMethodService service = svc == null ? null : svc.get();
            android.view.inputmethod.InputConnection connection = service == null ? null : service.getCurrentInputConnection();
            if (connection == null) return null;
            CharSequence before = connection.getTextBeforeCursor(CTX_BEFORE, 0);
            CharSequence after = connection.getTextAfterCursor(CTX_AFTER, 0);
            if (before == null) return null;
            return EditorContext.fromRaw(before.toString(), after == null ? "" : after.toString(), word);
        } catch (Throwable e) { return null; }
    }

    static EditorContext captureContext(Sequence ctx, String word) {
        // Capture on the requesting thread, before it waits for ENGINE. Never pass an InputConnection to ENGINE.
        EditorContext fields = "com.samsung.android.honeyboard".equals(pkg) ? localContext(word) : readContext(word);
        return fields == null ? new EditorContext(contextText(ctx), "") : fields;
    }

    static String afterComposition(String before, String after, String word) {
        if (word == null || word.isEmpty()) return after;
        int max = Math.min(before.length(), word.length());
        for (int prefix = max; prefix >= 0; prefix--) {
            String rest = word.substring(prefix);
            if (before.endsWith(word.substring(0, prefix)) && after.startsWith(rest))
                return after.substring(rest.length());
        }
        return after;
    }

    static String fieldAfter() { return fieldAfter(composing()); }

    static String fieldAfter(String word) {
        EditorContext fields = "com.samsung.android.honeyboard".equals(pkg) ? localContext(word) : readContext(word);
        return fields == null ? "" : fields.after;
    }

    static String shorten(String t) {
        String x = t.replace('\n', '⏎');
        return x.length() > 120 ? "…" + x.substring(x.length() - 120) : x;
    }

    // Llamado al inicio de aj.j.b(): guarda el Qi.c con la palabra en composición.
    public static void setInput(Object o) {
        input = o;
    }

    static synchronized void resetInputSession() {
        inputGeneration.incrementAndGet();
        input = null;
        decWord = null;
        correctionText = null;
        decResult = 0;
        localEditorText = null;
        lastTouch = null;
        lastLearnKey = "";
    }

    static boolean predictionCurrent(long generation, String word) {
        return !secret && generation == inputGeneration.get()
                && java.util.Objects.equals(word, composing());
    }

    static synchronized void configureDirectory(File filesDir) {
        if (filesDir == null || state == 1 || state == 2) return;
        String directory = new File(filesDir, "gbport").getAbsolutePath();
        if (directory.equals(DIR)) return;
        DIR = directory;
        retryAtNanos = 0;
        state = 0;
    }

    static void engineUnavailable() {
        retryAtNanos = System.nanoTime() + RETRY_NANOS;
        decWord = null;
        correctionText = null;
        decResult = 0;
        state = -1;
    }

    static void start() {
        synchronized (Bridge.class) {
            if (DIR == null || state == 1 || state == 2
                    || (state == -1 && System.nanoTime() < retryAtNanos)) return;
            boolean available = false;
            for (String[] lm : LMS) {
                File file = new File(DIR, lm[0]);
                if (file.isFile() && file.canRead() && file.length() > 0) available = true;
            }
            if (!available) {
                engineUnavailable();
                Log.w(TAG, "modelos base no disponibles en el perfil; se reintentará");
                return;
            }
            state = 1;
        }
        // Arranque en el hilo del motor: todas las llamadas nativas van por el mismo hilo.
        ENGINE.execute(() -> {
            try {
                Delight.loadLibrary("integrated_shared_object", DIR + "/crash");
                Delight.createDecoder(new String[][]{{"es", "US"}, {"en", "US"}}, DIR + "/data");
                Split.clearCache();
                Delight.setQwertyLayout();
                int ok = 0;
                for (String[] lm : LMS) {
                    File f = new File(DIR, lm[0]);
                    if (f.exists() && Delight.loadMainLm(f.getPath(), lm[1], lm[2])) ok++;
                }
                // Lo que Gboard aprendió de ti (copiado de Gboard al instalar): historial bilingüe, diccionario
                // personal y contactos. Se cargan como en Gboard (Delight5Facilitator / lzg).
                String pd = DIR + "/personal/";
                forgetPending(pd + "UserHistory.dict");
                String perso = (Delight.loadPersonalLm(Delight.LM_USER_HISTORY, pd + "UserHistory.dict", null, null, "en", "es") ? "historial " : "")
                        + (Delight.loadPersonalLm(Delight.LM_PERSONAL, pd + "Personal.es_US.dict", "es", "US") ? "personal-es " : "")
                        + (Delight.loadPersonalLm(Delight.LM_PERSONAL, pd + "Personal.en_US.dict", "en", "US") ? "personal-en " : "")
                        + (Delight.loadPersonalLm(Delight.LM_CONTACTS, pd + "Contacts.dict", null, null) ? "contactos " : "")
                        // Palabras que quitaste de las sugerencias en Gboard y el diccionario de correos (Gboard: lvq, mba).
                        + (Delight.loadPersonalLm(Delight.LM_BLOCKLIST, pd + "blacklist.es_us.list", "es", "US") ? "bloqueadas-es " : "")
                        + (Delight.loadPersonalLm(Delight.LM_BLOCKLIST, pd + "blacklist.en_us.list", "en", "US") ? "bloqueadas-en " : "")
                        + (Delight.loadPersonalLm(Delight.LM_EMAIL, pd + "Email.dict", null, null) ? "correo" : "");
                // Emoji de Gboard (atajos palabra -> emoji, copiados de Gboard al instalar).
                String ed = DIR + "/emoji/";
                int emo = Delight.loadEmojiShortcuts(new String[][]{{ed + "es_us.shortcuts", "es"}, {ed + "en_us.shortcuts", "en"}});
                Delight.emoji = emo > 0;
                GLog.w("emoji: " + emo + " atajos");
                // Parámetros de sesión como los de Gboard (siguiente palabra, app del campo, aprendizaje).
                Delight.setRuntimeParams(NEXT_WORD, pkg, search, email);
                GLog.w("personales: " + (perso.isEmpty() ? "ninguno" : perso));
                if (ok > 0) { retryAtNanos = 0; state = 2; }
                else engineUnavailable();
                Log.i(TAG, "motor Gboard listo, modelos=" + ok);
            } catch (Throwable e) {
                engineUnavailable();
                Log.e(TAG, "no se pudo iniciar el motor Gboard; se reintentará", e);
            }
        });
    }

    static String composing() {
        Object o = input;
        if (!(o instanceof Qi.c)) return null;
        try {
            return ((Qi.c) o).g();
        } catch (Throwable e) {
            return null;
        }
    }

    static String contextText(Sequence s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        int n = s.size();
        for (int i = 0; i < n; i++) {
            Term t = s.get(i);
            String w = t == null ? null : t.getTerm();
            if (w == null || w.isEmpty()) continue;
            sb.append(w).append(' ');
        }
        return sb.toString();
    }

    // ---- Modelo espacial: geometría real del teclado de Samsung ----
    // Centros reales de cada tecla (minúscula base, 0..127) en píxeles de pantalla, aprendidos del toque real.
    static final float[] cx = new float[128], cy = new float[128];
    static final int[] cn = new int[128];
    // Samsung reuses history entries across predictions. Each physical tap trains once.
    static final java.util.Map<Object, Boolean> calibratedTouches = new java.util.WeakHashMap<>();
    static final String[] GROWS = {"qwertyuiop", "asdfghjklñ", "zxcvbnm"};
    static final float[] GINDENT = {0f, 0f, 1.5f};
    static final int MINSAMP = 2;

    // Campos reflejados de Samsung: Qi.a.e -> Qi.k ; Qi.k.d -> List<Qi.j> ; Qi.j.a -> Point, .b -> int.
    static java.lang.reflect.Field fK, fList, fPt, fCode;
    static boolean reflectFail;

    static void resolveReflect(Object in) {
        if (fK != null || reflectFail) return;
        try {
            java.lang.reflect.Field kf = null;
            for (Class<?> k = in.getClass(); k != null && kf == null; k = k.getSuperclass())
                for (java.lang.reflect.Field fd : k.getDeclaredFields())
                    if ("Qi.k".equals(fd.getType().getName())) { kf = fd; break; }
            if (kf == null) { reflectFail = true; return; }
            kf.setAccessible(true);
            java.lang.reflect.Field lf = kf.getType().getDeclaredField("d");
            lf.setAccessible(true);
            fK = kf; fList = lf;
        } catch (Throwable e) { reflectFail = true; }
    }

    static int grow(char c) { for (int r = 0; r < 3; r++) if (GROWS[r].indexOf(c) >= 0) return r; return -1; }
    static char bl(char c) { return Delight.base(Character.toLowerCase(c)); }

    static float dispW() {
        try { return android.content.res.Resources.getSystem().getDisplayMetrics().widthPixels; }
        catch (Throwable e) { return 1080f; }
    }

    // Lee la lista de toques reales de la palabra en curso, la alinea por posición con la palabra,
    // actualiza la calibración y devuelve {x[], y[], has[]} por letra (píxeles reales); null si no hay datos.
    static float[][] realXY(Object in, String word) {
        resolveReflect(in);
        if (fK == null) return null;
        try {
            Object k = fK.get(in);
            if (k == null) return null;
            List<?> list = (List<?>) fList.get(k);
            if (list == null || list.isEmpty()) return null;
            if (fPt == null) {
                Class<?> jc = list.get(0).getClass();
                fPt = jc.getDeclaredField("a"); fPt.setAccessible(true);
                fCode = jc.getDeclaredField("b"); fCode.setAccessible(true);
            }
            int m = list.size(), wl = word.length();
            float[] px = new float[wl], py = new float[wl], hs = new float[wl];
            int startList = m - wl; // Alinear ambas colas, también si faltan toques iniciales.
            for (int i = 0; i < wl; i++) {
                int li = startList + i;
                if (li < 0 || li >= m) continue;
                Object p = fPt.get(list.get(li));
                if (!(p instanceof Point)) continue;
                float x = ((Point) p).getX(), y = ((Point) p).getY();
                if (!Float.isFinite(x) || !Float.isFinite(y)) continue;
                int code = fCode.getInt(list.get(li));
                if (code != 0 && bl((char) code) != bl(word.charAt(i))) continue;
                px[i] = x; py[i] = y; hs[i] = 1;
                char ch = bl(word.charAt(i));
                if (ch < 128 && grow(ch) >= 0
                        && calibratedTouches.put(list.get(li), Boolean.TRUE) == null) {
                    int n = cn[ch];
                    if (n == 0) { cx[ch] = x; cy[ch] = y; }
                    else { cx[ch] = (cx[ch] * n + x) / (n + 1); cy[ch] = (cy[ch] * n + y) / (n + 1); }
                    if (cn[ch] < 1000) cn[ch]++;
                    tapSamples++;
                }
            }
            return new float[][]{px, py, hs};
        } catch (Throwable e) { return null; }
    }

    // Ajuste por filas para la posición real: teclas de Samsung (Ui.b.h) si llegaron; si no, la media de tus toques
    // (se rehace cada 25 toques nuevos). null = sin calibración todavía.
    static volatile float[][] tapFit;
    static int spatialWords;
    static int tapSamples, tapFitAt;

    static float[][] calibFit() {
        float[][] rf = rowFit();
        if (rf != null) return rf;
        if (tapFit == null || tapSamples - tapFitAt >= 25) {
            java.util.Map<Character, float[]> m = new java.util.HashMap<>();
            for (int r = 0; r < 3; r++) for (int ci = 0; ci < GROWS[r].length(); ci++) {
                char ch = GROWS[r].charAt(ci);
                if (ch < 128 && cn[ch] >= 3) m.put(ch, new float[]{cx[ch], cy[ch]});
            }
            float[][] f = Geo.rowFit(m);
            if (f != null) { tapFit = f; tapFitAt = tapSamples; }
        }
        return tapFit;
    }

    // Toques reales de la palabra en curso, ya en el marco del motor ({xs, ys}, NaN = centro). null = sin datos.
    static float[][] realPoints(String word) {
        float[][] r = realXY(input, word);
        if (r == null) return null;
        float[][] rf = calibFit();
        if (rf == null) return null;
        int n = word.length();
        float[] xs = new float[n], ys = new float[n];
        int used = 0;
        for (int i = 0; i < n; i++) {
            if (r[2][i] == 0) { xs[i] = Float.NaN; ys[i] = Float.NaN; continue; }
            float[] e = Geo.toEngine(r[0][i], r[1][i], rf);
            // Un toque a más de una tecla y media del layout es un desajuste de alineación: centro.
            float[] c = Delight.center(word.charAt(i));
            if (c == null || Math.abs(e[0] - c[0]) > Delight.KW * 1.5f || Math.abs(e[1] - c[1]) > Delight.KH * 1.5f) {
                xs[i] = Float.NaN; ys[i] = Float.NaN; continue;
            }
            xs[i] = e[0]; ys[i] = e[1]; used++;
        }
        if ((++spatialWords % 40) == 1) GLog.w("dedo: calibración=" + (rowFit() != null ? "teclas" : "toques") + " toques usados " + used + "/" + n + " en '" + (secret ? "*" : word) + "'");
        return used == 0 ? null : new float[][]{xs, ys};
    }

    // Ajuste lineal de la rejilla desde las teclas calibradas: realX≈a·gx+b, realY≈c·fila+d. {a,b,c,d} o null.
    static float[] gridFit() {
        double nX = 0, sgx = 0, sx = 0, sgx2 = 0, sgxx = 0;
        double nY = 0, sgy = 0, sy = 0, sgy2 = 0, sgyy = 0;
        Set<Integer> gxset = new HashSet<>(), gyset = new HashSet<>();
        int learned = 0;
        for (int r = 0; r < 3; r++) for (int ci = 0; ci < GROWS[r].length(); ci++) {
            char ch = GROWS[r].charAt(ci);
            if (ch >= 128 || cn[ch] < MINSAMP) continue;
            learned++;
            double gx = GINDENT[r] + ci, gy = r;
            nX++; sgx += gx; sx += cx[ch]; sgx2 += gx * gx; sgxx += gx * cx[ch]; gxset.add((int) Math.round(gx * 2));
            nY++; sgy += gy; sy += cy[ch]; sgy2 += gy * gy; sgyy += gy * cy[ch]; gyset.add((int) gy);
        }
        if (learned < 8 || gxset.size() < 2 || gyset.size() < 2) return null;
        double dx = nX * sgx2 - sgx * sgx, dy = nY * sgy2 - sgy * sgy;
        if (dx == 0 || dy == 0) return null;
        double a = (nX * sgxx - sgx * sx) / dx, b = (sx - a * sgx) / nX;
        double c = (nY * sgyy - sgy * sy) / dy, d = (sy - c * sgy) / nY;
        if (a <= 0 || c <= 0) return null;
        return new float[]{(float) a, (float) b, (float) c, (float) d};
    }

    // ---- Swipe: trazo de Samsung decodificado por el decodificador de gestos de Gboard ----
    // Trazo actual (hj.b.h1: Samsung lo manda creciente con marca 1 mientras deslizas y -1 al soltar).
    static volatile boolean traceEnd;
    static volatile float[] trX = new float[0], trY = new float[0];
    static volatile long[] trT = new long[0];
    static volatile boolean traceOn;
    static volatile long traceAt;
    // Centros y tamaño reales de las teclas de letra (Ui.b.h: Samsung se los da a su modelo de pulsación).
    static final java.util.Map<Character, float[]> keys = new java.util.concurrent.ConcurrentHashMap<>();
    static volatile float[][] rowFit;     // por fila: {pendiente x por columna, x de la columna 0, y media}
    static java.lang.reflect.Field fAspect, fScale;

    // Llamado al inicio de Ui.b.h(KeyShape, alternativas).
    public static void onKeyShape(com.microsoft.fluency.KeyShape ks, String[] alts) {
        try {
            if (ks == null || alts == null || alts.length == 0 || alts[0] == null || alts[0].length() != 1) return;
            char c = bl(alts[0].charAt(0));
            if (grow(c) < 0) return;
            Point[] pts = ks.getPoints();
            if (pts == null || pts.length == 0 || pts[0] == null) return;
            float w = 0, h = 0;
            try {
                if (fAspect == null) {
                    fAspect = ks.getClass().getDeclaredField("aspectRatio"); fAspect.setAccessible(true);
                    fScale = ks.getClass().getDeclaredField("initialScaleMultiplier"); fScale.setAccessible(true);
                }
                float ar = fAspect.getFloat(ks), sc = fScale.getFloat(ks);
                if (ar >= 1) { h = sc; w = sc * ar; } else { w = sc; h = sc / ar; }
            } catch (Throwable e) {
                // sin tamaño: basta el centro
            }
            float[] prev = keys.put(c, new float[]{pts[0].getX(), pts[0].getY(), w, h});
            if (prev == null || prev[0] != pts[0].getX() || prev[1] != pts[0].getY()) {
                rowFit = null;
                if (keys.size() == 27) GLog.w("teclas reales: " + keys.size() + " (a=" + fmt(keys.get('a')) + " p=" + fmt(keys.get('p')) + " m=" + fmt(keys.get('m')) + ")");
            }
        } catch (Throwable e) {
            // nunca romper el teclado
        }
    }

    // Llamado al inicio de hj.b.Y0/Y1(R6.b): Samsung carga el teclado. R6.b.m = lista de R6.c (teclas) con
    // c = códigos, d/e = x/y, f/g = ancho/alto, h = código principal: la geometría exacta, en el mismo marco que los
    // toques y el trazo del swipe.
    static java.lang.reflect.Field fBoardKeys, fKc, fKx, fKy, fKw, fKh, fKp;

    public static void onBoard(Object board) {
        try {
            if (board == null) return;
            // Tras ¿ con pulsación larga Samsung vuelve sola a las letras (~30 ms) y en ese cambio apaga el Shift.
            if (state == 2) {
                String fb = fieldBefore(null);
                if (fb != null) shiftAfterOpener(fb);
            }
            if (fBoardKeys == null) { fBoardKeys = board.getClass().getDeclaredField("m"); fBoardKeys.setAccessible(true); }
            Object lo = fBoardKeys.get(board);
            if (!(lo instanceof List)) return;
            int n = 0;
            java.util.Map<Character, float[]> got = new java.util.HashMap<>();
            for (Object k : (List<?>) lo) {
                if (k == null) continue;
                if (fKx == null) {
                    Class<?> kc = k.getClass();
                    fKc = kc.getDeclaredField("c"); fKx = kc.getDeclaredField("d"); fKy = kc.getDeclaredField("e");
                    fKw = kc.getDeclaredField("f"); fKh = kc.getDeclaredField("g"); fKp = kc.getDeclaredField("h");
                    for (java.lang.reflect.Field fd : new java.lang.reflect.Field[]{fKc, fKx, fKy, fKw, fKh, fKp}) fd.setAccessible(true);
                }
                int code = fKp.getInt(k);
                if (code <= 0) { int[] cs = (int[]) fKc.get(k); if (cs != null && cs.length > 0) code = cs[0]; }
                if (code <= 0 || code > 0xFFFF) continue;
                char c = bl((char) code);
                if (grow(c) < 0) continue;
                float x = fKx.getInt(k), y = fKy.getInt(k), w = fKw.getInt(k), h = fKh.getInt(k);
                if (w <= 0 || h <= 0) continue;
                got.put(c, new float[]{x + w / 2f, y + h / 2f, w, h});
                n++;
            }
            if (n < 20) return;      // no es un teclado de letras (números, símbolos)
            boolean same = got.size() == keys.size();
            if (same) for (Map.Entry<Character, float[]> e : got.entrySet()) {
                float[] o = keys.get(e.getKey());
                if (o == null || !java.util.Arrays.equals(o, e.getValue())) { same = false; break; }
            }
            if (same) return;       // la misma geometría (Samsung la manda en cada apertura, dos veces)
            keys.clear();
            keys.putAll(got);
            rowFit = null;
            GLog.w("teclas reales (teclado): " + n + " a=" + fmt(keys.get('a')) + " p=" + fmt(keys.get('p')) + " m=" + fmt(keys.get('m')));
        } catch (Throwable e) {
            Log.w(TAG, "geometría del teclado no disponible", e);
        }
    }

    static String fmt(float[] k) { return k == null ? "-" : String.format(Locale.US, "%.0f,%.0f %.0fx%.0f", k[0], k[1], k[2], k[3]); }

    // Llamado al inicio de hj.b.h1(puntos, n, tiempos, marca).
    public static void onTrace(android.graphics.PointF[] pts, int n, long[] times, byte mark) {
        try {
            if (pts == null || times == null) return;
            int m = Math.min(n, Math.min(pts.length, times.length));
            if (m < 2) return;
            float[] x = new float[m], y = new float[m];
            long[] t = new long[m];
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (int i = 0; i < m; i++) {
                if (pts[i] == null) return;
                x[i] = pts[i].x; y[i] = pts[i].y; t[i] = times[i];
                minX = Math.min(minX, x[i]); maxX = Math.max(maxX, x[i]); minY = Math.min(minY, y[i]); maxY = Math.max(maxY, y[i]);
            }
            // Un toque con temblor no es swipe: el trazo debe recorrer al menos media tecla.
            float[] ka = keys.get('a');
            float minMove = ka != null && ka[2] > 0 ? ka[2] * 0.5f : 40f;
            if (Math.max(maxX - minX, maxY - minY) < minMove) return;
            trX = x; trY = y; trT = t;
            traceEnd = mark < 0;        // 1 = deslizando, -1 = el dedo se levantó
            traceAt = android.os.SystemClock.uptimeMillis();
            traceOn = true;
        } catch (Throwable e) {
            // nunca romper el teclado
        }
    }

    // Llamado al inicio de Qi.a.j (pulsación normal de tecla): se sale del modo swipe.
    public static void onTap() {
        traceOn = false;
    }

    static float[][] rowFit() {
        float[][] rf = rowFit;
        if (rf == null) rowFit = rf = Geo.rowFit(keys);
        return rf;
    }

    // Sugerencias del swipe con el decodificador de gestos de Gboard. null = que siga Samsung (Fluency).
    static Predictions gesture(ResultsFilter f, EditorContext fields) {
        float[][] rf = rowFit();
        float[] x = trX, y = trY;
        long[] t = trT;
        if (rf == null || x.length < 2) return null;
        float[] ex = new float[x.length], ey = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            float[] e = Geo.toEngine(x[i], y[i], rf);
            ex[i] = e[0]; ey[i] = e[1];
        }
        int total = Math.max(3, f == null ? 6 : f.getTotal());
        String before = fields.before;
        // El trazo entero cada vez, pero "dedo arriba" solo al levantarlo: si cada vista previa lo levantaba, el motor
        // encadenaba los gestos (hi -> hiJo -> hiJopoo, 15:57) y el trazo final salía basura.
        boolean end = traceEnd;
        // 16+ candidatos: así entran las variantes con tilde aunque el historial meta alargadas (queeeee).
        byte[] r = Delight.decodeGestureStream(before, fields.after, ex, ey, t, 0, true, end, Math.max(16, total + 6));
        // Mientras se desliza el motor no escribe la palabra (g vacío): orden por puntuación de sus candidatos.
        List<String> ws = Swipe.rank(Delight.gestureText(r), Delight.parseCands(r), before);
        // Mayúsculas como las pondría Samsung al escribir: su estado de Shift (0 nada, 1 la siguiente letra, que incluye
        // el inicio de frase automático, 2 bloqueo). Solo afecta a lo deslizado; lo tecleado lo sigue gestionando Samsung.
        int sh = shiftState();
        if (sh > 0) {
            List<String> cased = new ArrayList<>();
            for (String w : ws) cased.add(sh == 2 ? w.toUpperCase(Locale.ROOT) : (w.isEmpty() ? w : w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1)));
            ws = cased;
        }
        List<Prediction> out = new ArrayList<>();
        for (int i = 0; i < ws.size() && out.size() < total + 2; i++) out.add(new Prediction(ws.get(i), i == 0 ? 0.995 : 0.25 / (i + 1)));
        if (out.isEmpty()) return null;
        if (!secret) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(5, out.size()); i++) sb.append(i == 0 ? "" : ", ").append(out.get(i).getPrediction());
 if (end) GLog.w("swipe pts=" + x.length + " shift=" + sh + " -> " + sb + " app=" + pkg + " ctx=[" + shorten(before) + "]");
        }
        return new Predictions(out);
    }

    static String correctedSearchTail(String before, String word, String correction) {
        if (before == null || word == null || word.isEmpty() || correction == null
                || correction.equals(word) || !before.endsWith(word + " ")) return null;
        int start = before.length() - word.length() - 1;
        if (start > 0 && Character.isLetterOrDigit(before.charAt(start - 1))) return null;
        return correction + " ";
    }

    static boolean needsCommitRefresh(String word) {
        if (state != 2 || secret || word == null || word.isEmpty()) return false;
        if (!word.equals(decWord)) return true;
        if (decResult == 1 && correctionText != null && correctionText.contains(" ")) return false;
        Double literal = litSeen.get(word);
        boolean uncertain = literal == null || literal.isNaN() || literal < Split.TRY_BELOW;
        return uncertain && word.length() >= 3 && (decResult == 0 || word.length() >= 6);
    }

    public static void beforeGoogleCommit(CharSequence text) {
        if (!" ".contentEquals(text == null ? "" : text) || secret
                || !"com.google.android.googlequicksearchbox".equals(pkg)) return;
        android.inputmethodservice.InputMethodService service = svc == null ? null : svc.get();
        android.view.inputmethod.InputConnection connection = service == null ? null : service.getCurrentInputConnection();
        if (connection == null || state != 2) return;
        try {
            CharSequence preceding = connection.getTextBeforeCursor(CTX_BEFORE, 0);
            if (preceding == null || preceding.length() == 0) return;
            String snapshot = preceding.toString();
            java.util.regex.Matcher words = WORD.matcher(snapshot);
            String word = null;
            while (words.find()) if (words.end() == snapshot.length()) word = words.group();
            if (word == null || word.length() > 64 || !Delight.typeable(word)) return;
            final long generation = inputGeneration.get();
            String correction = word.equals(decWord) && decResult == 1 ? correctionText : null;
            if (needsCommitRefresh(word)) {
                final String completeWord = word;
                final EditorContext fields = captureContext(null, completeWord);
                final long deadline = System.nanoTime() + 90_000_000L;
                java.util.concurrent.Future<Out> decision = ENGINE.submit(() -> {
                    if (generation != inputGeneration.get() || secret) return new Out();
                    return compute(null, null, completeWord, false, deadline, null, fields);
                });
                try {
                    Out finalDecision = decision.get(120, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (finalDecision.correction != null) correction = finalDecision.correction;
                } catch (java.util.concurrent.TimeoutException e) {
                    decision.cancel(false);
                    GLog.w("Google: decisión final sin tiempo; se conserva la corrección comprobada");
                }
            }
            if (generation != inputGeneration.get() || secret) return;
            if (correction == null || correctedSearchTail(snapshot + " ", word, correction) == null) return;
            CharSequence unchanged = connection.getTextBeforeCursor(CTX_BEFORE, 0);
            if (unchanged == null || !snapshot.contentEquals(unchanged)) return;
            android.view.inputmethod.ExtractedText extracted = connection.getExtractedText(
                    new android.view.inputmethod.ExtractedTextRequest(), 0);
            if (extracted == null || extracted.selectionStart != extracted.selectionEnd) return;
            int cursor = extracted.startOffset + extracted.selectionEnd;
            int start = cursor - word.length();
            if (start < extracted.startOffset) return;
            boolean applied = false;
            connection.beginBatchEdit();
            try {
                connection.finishComposingText();
                if (connection.setSelection(start, cursor)) applied = connection.commitText(correction, 1);
                if (!applied) connection.setSelection(cursor, cursor);
            } finally {
                connection.endBatchEdit();
            }
            GLog.w("Google: corrección durante espacio aplicada=" + applied);
        } catch (Throwable e) {
            GLog.w("Google: no se pudo aplicar la corrección al espacio");
        }
    }

    // Tras espacio (sin palabra en curso): siguiente palabra según Gboard a partir del texto previo.
    // Si Gboard no da palabras (solo puntuación), se queda la predicción de Samsung.
    static Predictions nextWord(ResultsFilter f, Predictions fl, EditorContext fields) {
        try {
            String before = fields.before;
            if (!secret) {
                final String b = before;
                java.lang.ref.WeakReference<android.inputmethodservice.InputMethodService> r = svc;
                android.inputmethodservice.InputMethodService is = r == null ? null : r.get();
                if (b.endsWith("//") && is != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> Ai.maybeCommand(is, b));
                    return fl;
                }
            }
            noteCommitted(before);
            shiftAfterOpener(before);
            if (before.trim().isEmpty()) return fl;
            int total = Math.max(3, f == null ? 6 : f.getTotal());
            List<Delight.Cand> cs = Delight.suggest(before, fields.after, "", Math.max(20, total + 6));
            List<Prediction> out = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            double max = Double.NEGATIVE_INFINITY;
            for (Delight.Cand c : cs) if (c.type != 2 && !Float.isNaN(c.score)) max = Math.max(max, c.score);
            for (Delight.Cand c : cs) {
                if (c.type == 2 || c.text.trim().isEmpty() || !seen.add(c.text.toLowerCase())) continue;  // 2 = puntuación
                double pr = Float.isNaN(c.score) ? 0.01 : 0.5 * Math.exp(c.score - max);
                out.add(new Prediction(c.text, pr));
                if (out.size() >= total) break;
            }
            if (out.isEmpty()) return fl;
            if (!secret) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(4, out.size()); i++) sb.append(i == 0 ? "" : ", ").append(out.get(i).getPrediction());
                GLog.w("sig ctx=[" + shorten(before) + "] -> " + sb);
            }
            return new Predictions(out);
        } catch (Throwable e) {
            Log.e(TAG, "fallo en siguiente palabra, uso Samsung", e);
            return fl;
        }
    }

    // ---- Hilo del motor ----
    // Samsung pide sugerencias desde el hilo de la interfaz (al tocar y al borrar). El motor trabaja en su propio
    // hilo con un límite de tiempo: si no llega, se usan las de Samsung y el teclado nunca se traba (ANR 11:52).
    static final java.util.concurrent.ExecutorService ENGINE = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "GboardPort-engine");
        t.setDaemon(true);
        return t;
    });
    static final java.util.concurrent.atomic.AtomicBoolean busy = new java.util.concurrent.atomic.AtomicBoolean();
    // Presupuesto de espera: hilo de la interfaz / otros hilos de Samsung.
    static final long UI_BUDGET_MS = 80, BG_BUDGET_MS = 250;
    static volatile int slow;
    static volatile long busySince;
    static volatile boolean hangLogged;

    // Resultado del motor: sugerencias (null = las de Samsung) y decisión de autocorrección para aj.j.k.
    static final class Out {
        Predictions p;
        String dw;
        String correction;
        int dr;
        List<String> emojis;     // emoji de Gboard para la fila de emoji de Samsung (aj.j.p)
    }

    // Objeto de predicción de Samsung (aj.j), enganchado al inicio de aj.j.b. Su lista p es la que la interfaz pinta
    // como emoji sugeridos (Hj.b.O1); Samsung solo la llena si su modelo de SwiftKey trae la etiqueta "emoji".
    static volatile Object owner;
    static java.lang.reflect.Field fEmojiList;

    public static void setOwner(Object o) {
        owner = o;
    }

    // Emoji de Gboard para la palabra en curso (último cálculo) y cuándo se calcularon.
    static volatile List<String> lastEmojis = new ArrayList<>();
    static volatile long lastEmojisAt;

    // Llamado en Sg.g0.a justo antes de pintar la fila de emoji (sea de EmojiCore de Samsung o de SwiftKey): pone
    // delante los emoji de Gboard de la palabra en curso (sus -> 😳, spider -> 🕸️, araña -> 🕷️).
    @SuppressWarnings("unchecked")
    public static void mergeEmojis(List list) {
        try {
            if (list == null || state != 2) return;
            List<String> em = lastEmojis;
            if (em.isEmpty() || android.os.SystemClock.uptimeMillis() - lastEmojisAt > 5000) return;
            java.util.Set<String> have = new HashSet<>();
            for (Object o : list) if (o != null) have.add(o.toString());
            int at = 0;
            for (String e : em) {
                if (have.contains(e)) { list.remove(e); }
                list.add(at++, e);
            }
            while (list.size() > 9) list.remove(list.size() - 1);
        } catch (Throwable e) {
            // la fila de Samsung queda como estaba
        }
    }

    @SuppressWarnings("unchecked")
    static void putEmojis(List<String> emojis) {
        try {
            Object o = owner;
            if (o == null) return;
            if (fEmojiList == null) { fEmojiList = o.getClass().getDeclaredField("p"); fEmojiList.setAccessible(true); }
            Object l = fEmojiList.get(o);
            if (!(l instanceof List)) return;
            List<Object> list = (List<Object>) l;
            list.clear();
            double pr = 0.9;
            for (String e : emojis) { list.add(new Prediction(e, pr)); pr -= 0.05; }
        } catch (Throwable e) {
            // sin fila de emoji
        }
    }

    static boolean delightOwns(String word) {
        return state == 2 && !secret && (word == null || word.isEmpty() || Delight.typeable(word));
    }

    static Predictions neutralPredictions(String word) {
        List<Prediction> out = new ArrayList<>();
        if (word != null && !word.isEmpty()) out.add(new Prediction(word, 0.90));
        return new Predictions(out);
    }

    public static Predictions getPredictions(Predictor p, Sequence ctx, TouchHistory th, ResultsFilter f) {
        final long requestedAt = System.nanoTime();
        if (secret) return p.getPredictions(ctx, th, f);
        final long generation = inputGeneration.get();
        String current = composing();
        Predictions fl = delightOwns(current) ? neutralPredictions(current) : p.getPredictions(ctx, th, f);
        lastTypeMs = System.currentTimeMillis();
        if (state != 2) {
            start();
            return fl;
        }
        // Motor ocupado: no se encola nada; se conserva la entrada neutral en campos de Delight.
        if (!busy.compareAndSet(false, true)) {
            long since = android.os.SystemClock.uptimeMillis() - busySince;
            if (since > 5000 && !hangLogged) { hangLogged = true; GLog.w("motor sin responder " + since + " ms; se conserva la entrada sin corregir"); }
            decWord = null;
            return fl;
        }
        busySince = android.os.SystemClock.uptimeMillis();
        hangLogged = false;
        boolean ui = android.os.Looper.myLooper() == android.os.Looper.getMainLooper();
        final String word;
        final boolean swipeNow;
        final float[][] touch;
        java.util.concurrent.Future<Out> fut;
        try {
            word = composing();
            boolean sw = false;
            try { sw = SWIPE && traceOn && th != null && android.os.SystemClock.uptimeMillis() - traceAt < 3000 && th.size() > 0; }
            catch (Throwable e) { /* sin swipe */ }
            swipeNow = sw;
            float[][] tp = null;
            if (SPATIAL && !sw && word != null && !word.isEmpty()) {
                try { tp = realPoints(word); } catch (Throwable e) { tp = null; }
            }
            touch = tp;
            final EditorContext fields = captureContext(ctx, word);
            // Plazo para el trabajo opcional (corte de palabras pegadas): lo que sobre del presupuesto de quien espera.
            final long deadline = System.nanoTime() + ((ui ? UI_BUDGET_MS : BG_BUDGET_MS) - 25) * 1_000_000L;
            fut = ENGINE.submit(() -> {
                try {
                    if (generation != inputGeneration.get() || secret) return new Out();
                    return compute(f, fl, word, swipeNow, deadline, touch, fields);
                }
                finally { busy.set(false); }
            });
        } catch (Throwable e) {
            busy.set(false);
            decWord = null;
            return fl;
        }
        try {
            Out o = fut.get(ui ? UI_BUDGET_MS : BG_BUDGET_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
            synchronized (Bridge.class) {
                if (!predictionCurrent(generation, word)) return neutralPredictions(composing());
                decWord = o.dw;
                decResult = o.dr;
                correctionText = o.correction;
                if (o.emojis != null) {
                    putEmojis(o.emojis);
                    lastEmojis = o.emojis;
                    lastEmojisAt = android.os.SystemClock.uptimeMillis();
                }
            }
            if ("com.samsung.android.honeyboard".equals(pkg)) {
                long ms = (System.nanoTime() - requestedAt) / 1_000_000L;
                GLog.w("editor Delight: " + ms + " ms, hilo=" + (ui ? "UI" : "fondo"));
            }
            return o.p != null ? o.p : fl;
        } catch (java.util.concurrent.TimeoutException e) {
            decWord = null;
            if ((++slow & 15) == 1) GLog.w("motor lento (>" + (ui ? UI_BUDGET_MS : BG_BUDGET_MS) + " ms), uso entrada sin corregir; veces=" + slow
                    + (learningNow ? " (aprendiendo)" : " palabra='" + (secret ? "*" : word) + "'"));
            return fl;
        } catch (Throwable e) {
            decWord = null;
            return fl;
        }
    }

    static Out compute(ResultsFilter f, Predictions fl, String word, boolean swipeNow, long deadline, float[][] touch, EditorContext fields) {
        Out o = new Out();
        // Swipe en curso o recién soltado: trazo vivo y el historial de Samsung no está vacío.
        if (swipeNow) {
            try {
                Predictions gp = gesture(f, fields);
                if (gp != null) { o.p = gp; return o; }
            } catch (Throwable e) {
                Log.e(TAG, "fallo en swipe, uso Samsung", e);
            }
        }
        if (NEXT_WORD && (word == null || word.isEmpty())) { o.p = nextWord(f, fl, fields); o.emojis = new ArrayList<>(); return o; }
        if (word == null || !Delight.typeable(word)) return o;
        try {
            int total = Math.max(3, f == null ? 6 : f.getTotal());
            // Decodificación CON el contexto de la frase, como un teclado real: tras cada tecla se le devuelve al motor
            // el estado del campo (Delight.suggestEchoRaw). Con posición real del dedo si hay calibración (touch).
            // 20+ candidatos para que entren las variantes con ñ.
            String before = fields.before;
            List<Delight.Cand> cs = Delight.parseCands(Delight.suggestEchoRaw(before, fields.after, word, Math.max(20, total + 6),
                    touch == null ? null : touch[0], touch == null ? null : touch[1], null));
            cs = Decide.rankCandidates(cs, word);


            Decide.Result dec = Decide.decide(cs, word, false);
            if (touch != null && !secret) lastTouch = new Object[]{before, word, touch[0], touch[1]};
            // Lo tecleado con su puntuación real, también si se corrigió: si deshaces la corrección (sobrevivieram) no
            // se aprende a la primera.
            litSeen.put(word, dec.litScore);
            if (dec.cand != null) fixSeen.put(word.toLowerCase(Locale.ROOT), dec.cand.text);
            Delight.Cand bestCorr = dec.cand;
            boolean doCorrect = bestCorr != null;
            String corrText = doCorrect ? bestCorr.text : word;
            String why = dec.why;
            String interr = Decide.interrogative(before, word);
            if (interr != null && !interr.equals(word)) { doCorrect = true; corrText = interr; why = "interrogativa"; }
            // Red de seguridad: si aun así la palabra tras ¿ ¡ (o « " al empezar frase) llega en minúscula, al confirmarla
            // se pone con mayúscula inicial.
            if (!corrText.isEmpty() && Character.isLowerCase(corrText.charAt(0)) && !before.isEmpty()
                    && Caps.OPENERS.indexOf(before.charAt(before.length() - 1)) >= 0
                    && (lastType & android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0
                    && Caps.openerCaps(before, before.length(), android.text.TextUtils.CAP_MODE_SENTENCES) != 0) {
                corrText = Character.toUpperCase(corrText.charAt(0)) + corrText.substring(1);
                doCorrect = true;
                why = why + "+mayúscula";
            }
            // Lo tecleado no es palabra y Decide no lo corrige: decide la autocorrección del propio motor, como Gboard al
            // pulsar espacio (realid -> realidad). Al final, porque deja el estado del motor tras un espacio.
            if ((!doCorrect || why.startsWith("motor(")) && Decide.needsEngine(word, dec)
                    && System.nanoTime() < deadline) {
                try {
                    String eng = Delight.typeAndSpace(before, word, true, touch == null ? null : touch[0], touch == null ? null : touch[1])[0];
                    String h = Decide.hybrid(word, eng, dec);
                    if (!h.equals(word)) { doCorrect = true; corrText = h; why = "motor-gboard"; }
                } catch (Throwable e) { /* sin cambio */ }
            }

            // Palabras pegadas: cada tecla apunta su prefijo; con la palabra completa se prueba el corte en el tiempo que quede.
            Split.note(before, word, cs);
            Split.Result sp = null;
            if (!dec.why.equals("aprendida")) {
                try { sp = Split.update(before, fields.after, word, deadline); } catch (Throwable e) { sp = null; }
            }
            double single = bestCorr != null ? bestCorr.score : dec.litScore;
            if (Split.wins(sp, dec.litScore, single)) { doCorrect = true; corrText = sp.text; why = "corte"; }
            // Palabra pegada al signo anterior (hola,como): se antepone un espacio.
            if (Split.needsSpace(before, word)) { doCorrect = true; corrText = " " + corrText; why = why + "+espacio"; }

            // Se guarda la decisión de Gboard para imponerla en aj.j.k (saltando el filtro del diccionario de Samsung).
            o.dw = word;
            if (doCorrect) fixSeen.put(word.toLowerCase(Locale.ROOT), corrText);
            o.dr = doCorrect ? 1 : 0;
            o.correction = doCorrect ? corrText : null;

            if (!secret) {
                StringBuilder top = new StringBuilder();
                int k = 0;
                for (Delight.Cand c : cs) {
                    if (Float.isNaN(c.score)) continue;
                    top.append(k == 0 ? "" : " ").append(c.text).append(String.format(Locale.US, "(%.1f)", c.score));
                    if (++k == 4) break;
                }
                GLog.w("w=" + word + " -> " + (doCorrect ? "[" + corrText + "]" : "(sin corregir)") + " [" + why + "]"
                        + " lit=" + String.format(Locale.US, "%.1f", dec.litScore) + " app=" + pkg + " ctx=[" + shorten(before) + "] cands: " + top);
            }

            List<Prediction> out = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            if (doCorrect) {
                if (bestCorr != null && corrText.equals(bestCorr.text))
                    litSeen.put(corrText, (double) bestCorr.score);
                // Probabilidad alta para que Samsung aplique la corrección de Gboard al pulsar espacio.
                // NO se incluye lo tecleado: si aparece como candidato, Samsung lo toma por válido y no autocorrige.
                out.add(new Prediction(corrText, 0.995));
                seen.add(corrText.toLowerCase());
                seen.add(word.toLowerCase());
            } else {
                // No se corrige: se respeta lo tecleado (nombres, jerga, letras repetidas).
                out.add(new Prediction(word, 0.90));
                seen.add(word.toLowerCase());
            }
            // Emoji de Gboard (candidatos tipo 3, p. ej. pizza -> 🍕, sus -> 😳): en la barra detrás de la primera y en la
            // fila de emoji de Samsung (que es donde Samsung los muestra; de la barra los quita).
            int emojis = 0;
            o.emojis = new ArrayList<>();
            for (Delight.Cand c : cs) {
                if (c.type != 3 || emojis >= 3 || !seen.add(c.text)) continue;
                out.add(new Prediction(c.text, 0.5 - 0.01 * emojis));
                o.emojis.add(c.text);
                emojis++;
            }
            // Resto de sugerencias de Gboard (incluidas completaciones) por puntuación.
            double max = Double.NEGATIVE_INFINITY;
            for (Delight.Cand c : cs) if (c.type != 3 && !Float.isNaN(c.score)) max = Math.max(max, c.score);
            for (Delight.Cand c : cs) {
                if (!Decide.displayAlternative(word, doCorrect ? corrText : null, c.text)) continue;
                if (c.type == 3 || Float.isNaN(c.score) || !seen.add(c.text.toLowerCase())) continue;
                out.add(new Prediction(c.text, 0.25 * Math.exp(c.score - max)));
                if (out.size() >= total + 2 + emojis) break;
            }
            o.p = new Predictions(out);
            return o;
        } catch (Throwable e) {
            Log.e(TAG, "fallo del motor Gboard, vuelvo a Fluency", e);
            engineUnavailable();
            return o;
        }
    }
}
