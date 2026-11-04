package gbport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

// Puente con la IA de escritura de Samsung (Galaxy AI) que ya trae el teclado: el gestor oi.s (Ha.c) crea el redactor
// aiwriter.scs.a (Ha.d) con b(WritingComposerPromptParam) = redactar/reescribir (tono, formato y "characteristics": la
// descripción libre del estilo que usa "Mi estilo") y p(context, texto, PromptParam) = funciones (Proofreading, Emojinally,
// tonos, traducción). Para experimentar sin interfaz: un receptor que solo acepta órdenes con la clave de
// files/gbport/ai_token (legible solo con root) y deja la respuesta en el registro.
public final class Ai {
    static final String ACTION = "gbport.AI";
    static volatile boolean registered;
    static String token;

    private Ai() {}

    static Object writer() throws Exception {
        Object hc = Class.forName("vj.h").getMethod("f", Class.class).invoke(null, Class.forName("Ha.c"));
        hc.getClass().getMethod("u").invoke(hc);
        java.lang.reflect.Field f = hc.getClass().getDeclaredField("f");
        f.setAccessible(true);
        return f.get(hc);
    }

    static Method method(Object o, String name, int params) {
        for (Method m : o.getClass().getMethods()) if (m.getName().equals(name) && m.getParameterTypes().length == params) return m;
        return null;
    }

    static boolean serviceError(String value) {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("response error:") || normalized.startsWith("[error")
                || normalized.startsWith("invalid authentication") || normalized.equals("error");
    }

    static String text(Object g) throws Exception {
        String value = g == null ? null : (String) g.getClass().getMethod("a").invoke(g);
        if (serviceError(value)) {
            GLog.w("IA servicio: respuesta de error descartada");
            return null;
        }
        return value;
    }

    // Reescribir con el redactor: tone Professional/Casual/Polite/"My style"; format Standard/Email/Social media/Comment.
    static String compose(String text, String tone, String format, String characteristics, String special, String pkg) throws Exception {
        Object w = writer();
        Class<?> pc = Class.forName("com.samsung.android.honeyboard.common.aiwriter.data.WritingComposerPromptParam");
        Constructor<?> c = pc.getConstructor(String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, byte[].class, String.class, String.class);
        Object p = c.newInstance(characteristics == null ? "" : characteristics, format == null ? "Standard" : format,
                tone == null ? "Casual" : tone, "", text, "", special == null ? "None" : special, null, "", pkg == null ? "" : pkg);
        Method b = null;
        for (Method m : w.getClass().getMethods())
            if (m.getName().equals("b") && m.getParameterTypes().length == 2 && m.getParameterTypes()[0] == pc) b = m;
        return text(b.invoke(w, p, null));
    }

    // Funciones: feature Proofreading / Emojinally / Professional / Casual / Polite / Social post / Original.
    static String prompt(Context ctx, String text, String feature, String lang) throws Exception {
        Object w = writer();
        Class<?> pc = Class.forName("com.samsung.android.honeyboard.common.aiwriter.data.PromptParam");
        Object p = pc.getConstructor(String.class, String.class, String.class, String.class, String.class, boolean.class)
                .newInstance(text, "", lang, feature, lang, false);
        Method m = null;
        for (Method x : w.getClass().getMethods())
            if (x.getName().equals("p") && x.getParameterTypes().length == 4 && x.getParameterTypes()[2] == pc) m = x;
        return text(m.invoke(w, ctx, text, p, null));
    }

    // ---- Estilos propios en el menú de IA de Samsung ("Estilo de escritura") ----
    // Samsung admite estilos dinámicos (writing_assist_dynamic_style.json del servidor): nombres en Ja.i.e, etiquetas en
    // Ja.i.f y Ia.b.h nombre -> etiqueta; Bs.c los añade a la lista y al elegir uno el redactor llama a
    // aiwriter.scs.a.p(..., PromptParam(feature = nombre)). Se añaden los nuestros y p() se intercepta (intercept()).
    static final String[][] STYLES = {
            {"gb_emoji", "😀 Emojis+"}, {"gb_pop", "🔥 Pop y memes"}, {"gb_corto", "✂️ Más corto"}, {"gb_estilo", "✍️ A tu estilo"}};

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void addStyles() {
        try {
            Class<?> ji = Class.forName("Ja.i"), ib = Class.forName("Ia.b");
            java.lang.reflect.Field fe = ji.getDeclaredField("e"), ff = ji.getDeclaredField("f"), fh = ib.getDeclaredField("h");
            fe.setAccessible(true); ff.setAccessible(true); fh.setAccessible(true);
            java.util.List names = (java.util.List) fe.get(null), labels = (java.util.List) ff.get(null);
            java.util.Map map = (java.util.Map) fh.get(null);
            int added = 0;
            for (String[] st : STYLES) {
                if (!names.contains(st[0])) { names.add(st[0]); labels.add(st[1]); added++; }
                map.put(st[0], st[1]);
            }
            if (added > 0) GLog.w("IA: " + added + " estilos propios en el menú de Samsung");
        } catch (Throwable e) {
            GLog.w("IA: no se pudieron añadir estilos (" + e + ")");
        }
    }

    static String savedStyle() {
        try {
            File f = new File(Bridge.DIR, "estilo.txt");
            if (f.exists()) return new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (Throwable e) { /* sin estilo */ }
        return "";
    }

    // Resultados de nuestros estilos para la pantalla: Samsung los busca en oi.u.a(estilo), donde solo guarda los suyos
    // (los dinámicos los espera de su servidor). Se calculan en paralelo cuando Samsung pide los suyos para ese texto.
    static final java.util.Map<String, String> results = new java.util.concurrent.ConcurrentHashMap<>();
    static final java.util.Map<String, java.util.concurrent.Future<?>> pending = new java.util.concurrent.ConcurrentHashMap<>();
    static volatile String prefetchText;
    static String prefetchStyle;
    static long prefetchGeneration, prefetchAttempt;
    static final java.util.concurrent.ExecutorService POOL = java.util.concurrent.Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "GboardPort-ai-style");
        t.setDaemon(true);
        return t;
    });

    static String nameOf(String feat) {
        if (feat == null) return null;
        for (String[] st : STYLES) if (feat.equals(st[0]) || feat.equals(st[1])) return st[0];
        return feat;
    }

    static String styleText(String name) {
        switch (name) {
            case "gb_emoji": case "Emojinally": return styleFor("emoji");
            case "gb_pop": return styleFor("pop");
            case "gb_corto": return styleFor("corto");
            case "gb_estilo": { String st = savedStyle(); return styleFor(st.isEmpty() ? "natural y con buena ortografía" : st); }
            case "gb_estilo_inline": return null;
            case "Professional": return BASE + "Tono profesional, claro y correcto, sin exagerar. Corrige ortografía y tildes.";
            case "Casual": return styleFor("informal");
            case "Polite": return BASE + "Tono educado y amable, sin sonar falso. Corrige ortografía y tildes.";
            case "Social post": return "Convierte el mensaje en una publicación para redes sociales en el mismo idioma, con gancho, natural, "
                    + "1 a 3 emojis y como mucho 2 hashtags si encajan. No lo respondas.";
            default: return null;
        }
    }

    // "A tu estilo": si el texto termina en "// cómo lo quieres" (tecleado o dictado), esa es la instrucción, se guarda
    // como tu estilo y se quita del mensaje; si no, se usa el estilo guardado.
    static String[] inlineStyle(String text) {
        String source = text.trim();
        if (source.endsWith("//")) source = source.substring(0, source.length() - 2).trim();
        int i = source.lastIndexOf("//");
        if (i < 1 || !Character.isWhitespace(source.charAt(i - 1))) return null;
        String ins = source.substring(i + 2).trim(), msg = source.substring(0, i).trim();
        if (ins.isEmpty() || msg.isEmpty() || ins.length() > 300 || ins.contains("\n")) return null;
        return new String[]{msg, ins};
    }

    static String run(String name, String text, String instruction) throws Exception {
        if ("gb_estilo".equals(name)) return text;
        String style = "gb_estilo".equals(name)
                ? styleFor(instruction.isEmpty() ? "natural y con buena ortografía" : instruction)
                : styleText(name);
        if ("gb_estilo".equals(name)) {
            String[] in = inlineStyle(text);
            if (in != null) {
                text = in[0];
                style = styleFor(in[1]);

            }
        } else {
            String[] in = inlineStyle(text);
            if (in != null) text = in[0];            // la instrucción no forma parte del mensaje en los demás estilos
        }
        String out = compose(text, "My style", "Standard", style, null, Bridge.pkg);
        if (out == null) return "";
        out = out.trim();
        // Errores del servicio que llegan como texto ("Invalid authentication parameter", "[ERROR:...]").
        if (out.startsWith("[ERROR") || out.startsWith("Invalid ") || out.equalsIgnoreCase("error")) return "";
        return "Social post".equals(name) ? out : out.replaceAll("(?m)\\s*#\\p{L}[\\p{L}\\d_]*", "").trim();
    }

    static synchronized void prefetch(String text) {
        if (text == null) return;
        long now = System.nanoTime();
        String[] inline = inlineStyle(text);
        if (inline != null) {
            try { java.nio.file.Files.write(new File(Bridge.DIR, "estilo.txt").toPath(),
                    inline[1].getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
            catch (Throwable e) { GLog.w("IA: no se pudo guardar la instrucción"); }
        }
        String instruction = savedStyle();
        boolean same = text.equals(prefetchText) && java.util.Objects.equals(instruction, prefetchStyle);
        if (same && now - prefetchAttempt < 5_000_000_000L) return;
        if (!same) {
            ++prefetchGeneration;
            for (java.util.concurrent.Future<?> task : pending.values()) task.cancel(false);
            results.clear();
            pending.clear();
            prefetchText = text;
            prefetchStyle = instruction;
        }
        prefetchAttempt = now;
        final long generation = prefetchGeneration;
        java.util.List<String> modes = new java.util.ArrayList<>();
        for (String[] style : STYLES) modes.add(style[0]);
        java.util.Collections.addAll(modes, "Professional", "Casual", "Polite", "Social post", "Emojinally");
        for (String name : modes) {
            java.util.concurrent.Future<?> task = pending.get(name);
            if (same && ((task != null && !task.isDone())
                    || !results.getOrDefault(name, "").isEmpty())) continue;
            pending.put(name, POOL.submit(() -> {
                long start = System.currentTimeMillis();
                String result = "";
                try {
                    result = run(name, text, instruction);
                    GLog.w("IA menú " + name + " (" + (System.currentTimeMillis() - start)
                            + " ms): " + result.length() + " caracteres");
                } catch (Throwable e) {
                    GLog.w("IA menú " + name + " error: "
                            + (e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e));
                }
                synchronized (Ai.class) {
                    if (generation == prefetchGeneration) results.put(name, result);
                }
                return result;
            }));
        }
    }

    static void awaitPrefetch(long maxMs) {
        long end = System.currentTimeMillis() + maxMs;
        for (java.util.concurrent.Future<?> f : pending.values()) {
            long left = end - System.currentTimeMillis();
            if (left <= 0) return;
            try { f.get(left, java.util.concurrent.TimeUnit.MILLISECONDS); } catch (Throwable e) { /* sigue */ }
        }
    }

    // Llamado al inicio de oi.u.a(estilo): resultado de nuestros estilos (null = el de Samsung).
    public static String cached(String feature) {
        String n = nameOf(feature);
        if (n == null || !n.startsWith("gb_")) return null;
        String r = results.get(n);
        return r == null ? "" : r;
    }

    // Llamado al inicio de aiwriter.scs.a.p(): devuelve un Ja.g con el resultado si el estilo es nuestro, si no null.
    public static Object intercept(String text, Object param) {
        try {
            String feat = nameOf((String) param.getClass().getMethod("getFeature").invoke(param));
            if (feat == null || text == null) return null;
            if (styleText(feat) == null) {
                if (!"Original".equals(feat) && !"Proofreading".equals(feat)) GLog.w("IA menú: estilo no reconocido '" + feat + "'");
                return null;
            }
            // Mientras Samsung pide los suyos se calculan también los nuestros, y se esperan antes de devolver el suyo para
            // que la pantalla los encuentre al mostrar los resultados.
            java.util.concurrent.Future<?> task;
            synchronized (Ai.class) {
                prefetch(text);
                task = pending.get(feat);
            }
            String res = null;
            if (task != null) {
                try { res = (String) task.get(15, java.util.concurrent.TimeUnit.SECONDS); }
                catch (java.util.concurrent.TimeoutException e) { GLog.w("IA menú: modo sin respuesta a tiempo"); }
            }
            if (!feat.startsWith("gb_")) awaitPrefetch(8000);
            if (res == null || res.isEmpty()) return null;     // sin respuesta: que lo haga Samsung
            Class<?> hc = Class.forName("Ja.h");
            Object h = hc.getConstructor(java.util.List.class, java.util.List.class, boolean.class, java.util.List.class)
                    .newInstance(new java.util.ArrayList<>(), new java.util.ArrayList<>(), false, new java.util.ArrayList<>());
            return Class.forName("Ja.g").getConstructor(String.class, hc, boolean.class).newInstance(res, h, false);
        } catch (Throwable e) {
            GLog.w("IA menú error: " + (e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e));
            return null;
        }
    }


    // ---- Pantalla "Editar" de Estilo de escritura: cápsula "¿Cómo lo quieres?" + botón Aplicar ----
    // ToolkitBaseResultEditFragment.s() construye la pantalla (una actividad normal: el teclado escribe en sus EditText).
    // Al final se llama a decorate(): encima de Cancelar/Realizado se añade una fila con la cápsula y "✨ Aplicar"
    // (gris si está vacía). Aplicar reescribe el texto de arriba con la instrucción; mantener pulsado deshace.
    static final int ID_RESULT_TEXT = 0x7f0a0bdd, ID_ACTION = 0x7f0a0bd6, ID_DONE = 0x7f0a0ba6;

    static volatile java.lang.ref.WeakReference<android.view.View> editWindow;

    static android.widget.EditText focusedLocalEditor() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) return null;
        android.view.View win = editWindow == null ? null : editWindow.get();
        if (win == null || !win.isAttachedToWindow()) return null;
        android.view.View focus = win.findFocus();
        if (!(focus instanceof android.widget.EditText)
                || (focus.getId() != ID_RESULT_TEXT && !"gbport-instruction".equals(focus.getTag()))) return null;
        return (android.widget.EditText) focus;
    }

    static LocalEditorText localText() {
        android.widget.EditText field = focusedLocalEditor();
        return field == null ? null : LocalEditorText.capture(field.getText(), field.getSelectionStart(), field.getSelectionEnd());
    }

    static EditorContext localContext(String word) {
        LocalEditorText text = localText();
        return text == null ? null : text.context(word);
    }

    public static void decorate(Object fragment, android.view.View root) {
        if (root == null) return;
        tryBuild(root, 0);
    }

    // La barra Cancelar/Realizado no cuelga del root del fragmento (Samsung la añade aparte): se busca en toda la ventana
    // y, si aún no está, se reintenta unas veces.
    static void tryBuild(android.view.View root, int attempt) {
        root.postDelayed(() -> {
            try {
                if (!build(root, attempt >= 6) && attempt < 6) tryBuild(root, attempt + 1);
            } catch (Throwable e) { GLog.w("IA editar: no se pudo añadir la cápsula (" + e + ")"); }
        }, attempt == 0 ? 0 : 200);
    }

    static int rid(Context c, String name) {
        return c.getResources().getIdentifier(name, "id", c.getPackageName());
    }

    static boolean build(android.view.View root, boolean last) {
        android.view.View win = root.getRootView();
        if (win.findViewWithTag("gbport-capsula") != null) return true;
        Context c0 = root.getContext();
        // Estructura real (uiautomator): writing_toolkit_result_effect_view = [ScrollView con el texto] +
        // writing_toolkit_result_edit_action_layout (Cancelar y Realizado a la derecha, la izquierda vacía).
        android.view.View action = win.findViewById(rid(c0, "writing_toolkit_result_edit_action_layout"));
        android.view.View cancel = win.findViewById(rid(c0, "writing_toolkit_cancel"));
        android.view.View ev = win.findViewById(ID_RESULT_TEXT);
        android.widget.EditText editor = ev instanceof android.widget.EditText ? (android.widget.EditText) ev : null;
        if (!(action instanceof android.view.ViewGroup) || editor == null || cancel == null || cancel.getLeft() <= 0) {
            if (last) GLog.w("IA editar: vistas no encontradas (barra=" + (action != null) + ", texto=" + (editor != null) + ", cancelar=" + (cancel != null) + ")");
            return false;
        }
        final android.view.View rootv = win;
        final android.view.ViewGroup bar = (android.view.ViewGroup) action;
        EditCommit.attach(editor);
        editWindow = new java.lang.ref.WeakReference<>(win);
        Context c = rootv.getContext();
        float dp = c.getResources().getDisplayMetrics().density;
        android.widget.LinearLayout row = new android.widget.LinearLayout(c);
        row.setTag("gbport-capsula");
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding((int) (16 * dp), (int) (6 * dp), (int) (12 * dp), (int) (2 * dp));

        android.widget.EditText cap = new android.widget.EditText(c);
        cap.setTag("gbport-instruction");
        cap.setHint("¿Cómo lo quieres?");
        cap.setSingleLine(true);
        cap.setTextSize(14);
        cap.setText("");
        cap.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        cap.setPrivateImeOptions("disableImage=true;disableSticker=true;disableSetting=true;disableWritingToolkit=true;disableCustomPaste=true;disableDirectWriting=true;");
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(22 * dp);
        bg.setColor(0x1F808080);
        cap.setBackground(bg);
        cap.setPadding((int) (14 * dp), (int) (8 * dp), (int) (14 * dp), (int) (8 * dp));
        row.addView(cap, new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Botón con el mismo icono de IA del encabezado de Samsung (writing_toolkit_activity_header_title_icon); si no
        // está, un ✨ de texto. En gris cuando la cápsula está vacía.
        android.widget.TextView apply = new android.widget.TextView(c);
        android.view.View hdr = rootv.findViewById(rid(c, "writing_toolkit_activity_header_title_icon"));
        android.graphics.drawable.Drawable icon = null;
        if (hdr instanceof android.widget.ImageView && ((android.widget.ImageView) hdr).getDrawable() != null) {
            android.graphics.drawable.Drawable.ConstantState cs = ((android.widget.ImageView) hdr).getDrawable().getConstantState();
            icon = cs != null ? cs.newDrawable(c.getResources()).mutate() : null;
        }
        if (icon != null) {
            int sz = (int) (30 * dp);
            icon.setBounds(0, 0, sz, sz);
            apply.setCompoundDrawables(icon, null, null, null);
        } else {
            apply.setText("✨");
            apply.setTextSize(20);
        }
        apply.setPadding((int) (9 * dp), (int) (9 * dp), (int) (9 * dp), (int) (9 * dp));
        apply.setMinWidth((int) (48 * dp));
        apply.setMinHeight((int) (48 * dp));
        apply.setContentDescription("Aplicar con Galaxy AI");
        row.addView(apply, new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        final android.graphics.drawable.Drawable ic = icon;
        android.graphics.ColorMatrix gm = new android.graphics.ColorMatrix();
        gm.setSaturation(0f);
        final android.graphics.ColorMatrixColorFilter grey = new android.graphics.ColorMatrixColorFilter(gm);

        final boolean[] editing = {false};
        Runnable refresh = () -> {
            boolean on = cap.getText().toString().trim().length() > 0 && !editing[0];
            apply.setEnabled(on);
            apply.setAlpha(on ? 1f : 0.4f);
            if (ic != null) ic.setColorFilter(on ? null : grey);
        };
        refresh.run();
        cap.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            public void onTextChanged(CharSequence s, int a, int b, int d) {}
            public void afterTextChanged(android.text.Editable e) { refresh.run(); }
        });
        final String[] previous = {null};
        apply.setOnClickListener(v -> {
            final String ins = cap.getText().toString().trim(), src = editor.getText().toString();
            if (ins.isEmpty() || src.trim().isEmpty() || editing[0]) return;
            editing[0] = true;
            if (ic == null) apply.setText("⏳"); else apply.animate().alpha(0.5f).setDuration(400);
            refresh.run();
            try { java.nio.file.Files.write(new File(Bridge.DIR, "estilo.txt").toPath(), ins.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
            catch (Throwable e) { /* ignorar */ }
            new Thread(() -> {
                String res = null;
                long t0 = System.currentTimeMillis();
                try {
                    String out = compose(src, "My style", "Standard", styleFor(ins), null, Bridge.pkg);
                    if (out != null) {
                        out = out.trim();
                        if (!(out.startsWith("[ERROR") || out.startsWith("Invalid "))) res = out.replaceAll("(?m)\\s*#\\p{L}[\\p{L}\\d_]*", "").trim();
                    }
                } catch (Throwable e) {
                    GLog.w("IA editar error: " + (e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e));
                }
                final String r = res;
                GLog.w("IA editar '" + ins + "' (" + (System.currentTimeMillis() - t0) + " ms): " + (r == null ? "sin respuesta" : r.length() + " caracteres"));
                rootv.post(() -> {
                    editing[0] = false;
                    if (ic == null) apply.setText("✨"); else apply.animate().cancel();
                    refresh.run();
                    if (!editor.isAttachedToWindow()) return;
                    if (r == null || r.isEmpty()) {
                        android.widget.Toast.makeText(c, "Galaxy AI no respondió", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!src.contentEquals(editor.getText())) {
                        android.widget.Toast.makeText(c, "El texto cambió durante la reescritura. Vuelve a aplicar.",
                                android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    EditCommit.rewritten(editor);
                    previous[0] = src;
                    editor.setText(r);
                    editor.setSelection(r.length());
                });
            }, "GboardPort-ai-edit").start();
        });
        apply.setOnLongClickListener(v -> {
            if (previous[0] == null) return false;
            editor.setText(previous[0]);
            editor.setSelection(previous[0].length());
            previous[0] = null;
            android.widget.Toast.makeText(c, "Deshecho", android.widget.Toast.LENGTH_SHORT).show();
            return true;
        });

        android.view.View done = rootv.findViewById(rid(c, "writing_toolkit_done"));
        if (done != null && cancel.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams buttons =
                    (android.view.ViewGroup.MarginLayoutParams) cancel.getLayoutParams();
            buttons.setMarginEnd((int) (10 * dp));
            buttons.rightMargin = (int) (10 * dp);
            cancel.setLayoutParams(buttons);
        }
        // A la izquierda de Cancelar, en la misma barra: ancho hasta Cancelar, alto de la barra.
        int w = cancel.getLeft() - (int) (22 * dp), h = bar.getHeight() > 0 ? bar.getHeight() : (int) (52 * dp);
        row.setPadding((int) (4 * dp), 0, 0, 0);
        android.view.ViewGroup.MarginLayoutParams lp = new android.view.ViewGroup.MarginLayoutParams(w, h);
        if (bar instanceof android.widget.LinearLayout) bar.addView(row, 0, lp); else bar.addView(row, lp);
        cancel.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int width = Math.max(0, l - (int) (12 * dp));
            if (row.getLayoutParams().width != width) {
                android.view.ViewGroup.LayoutParams size = row.getLayoutParams();
                size.width = width;
                row.setLayoutParams(size);
            }
        });
        GLog.w("IA editar: cápsula añadida en la barra (" + bar.getClass().getSimpleName() + ", " + w + "x" + h + ")");
        return true;
    }

    // ---- Órdenes en el propio texto: "mensaje //orden//" ----
    // Al cerrar la orden con // el mensaje (todo lo escrito antes del cursor) se reescribe con la IA de Samsung y se
    // sustituye. Órdenes: corrige, emoji, pop, corto, formal, deshacer; cualquier otro texto es una instrucción libre
    // ("A tu estilo"). "Mi estilo" obedece instrucciones libres pero añade hashtags y no traduce: se le pide que no.
    static final String BASE = "Reescribe el mismo mensaje conservando el narrador, las personas gramaticales, nombres y hechos. No lo respondas, no lo continúes ni inventes información. Devuelve solo la reescritura, "
            + "en el mismo idioma y sin hashtags. ";
    static volatile boolean busy;
    static volatile String lastOriginal, lastResult;

    static String styleFor(String cmd) {
        switch (cmd) {
            case "emoji": case "emojis": case "e":
                return BASE + "Corrige ortografía y tildes y llénalo de emojis: al menos uno después de cada frase o idea, "
                        + "también en medio del texto junto a las palabras que representan (casa 🏠, amor ❤️, cansado 😮‍💨), "
                        + "y dos o tres al final. Muchos emojis, variados y que encajen.";
            case "pop": case "meme": case "memes":
                return BASE + "Hazlo muy divertido, en español latino de internet, metiendo 2 a 4 referencias a memes y cultura "
                        + "pop actuales que encajen con lo que dice, por ejemplo: aura (+1000 aura), 67, rizz, delulu, red flag, "
                        + "es un ick, POV, main character, NPC, en su prime, le falta calle, chill guy, brainrot, tralalero "
                        + "tralalá, modo sigma, lowkey, cringe, slay. Natural, sin forzarlo, sin ofender y corto.";
            case "corto": case "breve":
                return BASE + "Más corto y directo. Corrige ortografía y tildes.";
            case "formal":
                return BASE + "Tono formal y educado. Corrige ortografía y tildes.";
            case "informal": case "casual":
                return BASE + "Tono informal y cercano, natural. Corrige ortografía y tildes.";
            default:
                return BASE + "Corrige ortografía y tildes. Instrucción: " + cmd;
        }
    }

    // Llamado con el texto antes del cursor tras cada tecla. true si era una orden (se encarga de todo).
    static boolean maybeCommand(android.inputmethodservice.InputMethodService s, String before) {
        if (before == null || !before.endsWith("//") || busy || s == null) return false;
        int open = before.lastIndexOf("//", before.length() - 3);
        if (open < 0) return false;
        String cmd = before.substring(open + 2, before.length() - 2).trim();
        if (cmd.isEmpty() || cmd.length() > 200 || cmd.contains("\n")) return false;
        android.view.inputmethod.InputConnection ic = s.getCurrentInputConnection();
        if (ic == null) return false;
        CharSequence full = ic.getTextBeforeCursor(3000, 0);
        if (full == null) return false;
        String all = full.toString();
        if (all.length() >= 3000 || !all.endsWith(before.substring(open))) return false;
        final int cmdLen = before.length() - open;
        final String msg = all.substring(0, all.length() - cmdLen).replaceAll("\\s+$", "");
        final String lc = cmd.toLowerCase(java.util.Locale.ROOT);
        final Context ctx = s.getApplicationContext();
        android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        if (lc.startsWith("estilo:")) {
            String st = cmd.substring(7).trim();
            try { java.nio.file.Files.write(new File(Bridge.DIR, "estilo.txt").toPath(), st.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
            catch (Throwable e) { /* ignorar */ }
            main.post(() -> {
                android.view.inputmethod.InputConnection c = s.getCurrentInputConnection();
                if (c != null) { c.finishComposingText(); c.deleteSurroundingText(cmdLen, 0); }
                android.widget.Toast.makeText(ctx, "✍️ Estilo guardado: " + st, android.widget.Toast.LENGTH_SHORT).show();
            });
            return true;
        }
        if (lc.equals("estilo")) {
            final String cur = savedStyle();
            main.post(() -> android.widget.Toast.makeText(ctx, cur.isEmpty() ? "Sin estilo guardado" : "✍️ " + cur, android.widget.Toast.LENGTH_LONG).show());
            main.post(() -> { android.view.inputmethod.InputConnection c = s.getCurrentInputConnection(); if (c != null) c.deleteSurroundingText(cmdLen, 0); });
            return true;
        }
        if (lc.equals("deshacer") || lc.equals("undo")) {
            main.post(() -> {
                android.view.inputmethod.InputConnection c = s.getCurrentInputConnection();
                if (c == null) return;
                c.beginBatchEdit();
                c.finishComposingText();
                String rest = all.substring(0, all.length() - cmdLen);
                if (lastOriginal != null && lastResult != null && rest.trim().endsWith(lastResult)) {
                    c.deleteSurroundingText(all.length(), 0);
                    String keep = rest.substring(0, rest.lastIndexOf(lastResult));
                    c.commitText(keep + lastOriginal, 1);
                    lastResult = null;
                } else {
                    c.deleteSurroundingText(cmdLen, 0);
                }
                c.endBatchEdit();
            });
            return true;
        }
        if (msg.trim().isEmpty()) return false;
        busy = true;
        main.post(() -> android.widget.Toast.makeText(ctx, "✨ Galaxy AI: " + cmd + "…", android.widget.Toast.LENGTH_SHORT).show());
        new Thread(() -> {
            String out = null;
            long t0 = System.currentTimeMillis();
            try {
                out = lc.equals("corrige") || lc.equals("corregir") || lc.equals("c")
                        ? prompt(ctx, msg, "Proofreading", "es")
                        : compose(msg, "My style", "Standard", styleFor(lc.equals("estilo+") || lc.equals("mio") ? savedStyle() : lc), null, Bridge.pkg);
            } catch (Throwable e) {
                GLog.w("IA orden error: " + (e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e));
            }
            final String res = out == null ? null : out.replaceAll("(?m)\\s*#\\p{L}[\\p{L}\\d_]*", "").trim();
            GLog.w("IA orden '" + cmd + "' (" + (System.currentTimeMillis() - t0) + " ms): " + (res == null ? "sin respuesta" : res.length() + " caracteres"));
            main.post(() -> {
                try {
                    android.view.inputmethod.InputConnection c = s.getCurrentInputConnection();
                    if (c == null) return;
                    CharSequence now = c.getTextBeforeCursor(3000, 0);
                    // Solo si el campo sigue igual (no has seguido escribiendo ni cambiado de chat).
                    if (now == null || !now.toString().equals(all)) {
                        android.widget.Toast.makeText(ctx, "Galaxy AI: el texto cambió, no se aplicó", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res == null || res.isEmpty()) {
                        android.widget.Toast.makeText(ctx, "Galaxy AI no respondió", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    c.beginBatchEdit();
                    c.finishComposingText();
                    c.deleteSurroundingText(all.length(), 0);
                    c.commitText(res, 1);
                    c.endBatchEdit();
                    lastOriginal = msg;
                    lastResult = res;
                } finally {
                    busy = false;
                }
            });
        }, "GboardPort-ai-cmd").start();
        return true;
    }

    static void register(Context app) {
        if (registered || app == null) return;
        try {
            File tf = new File(Bridge.DIR, "ai_token");
            if (!tf.exists()) {
                String t = Long.toHexString(new java.security.SecureRandom().nextLong());
                java.nio.file.Files.write(tf.toPath(), t.getBytes());
            }
            token = new String(java.nio.file.Files.readAllBytes(tf.toPath())).trim();
            BroadcastReceiver r = new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) {
                    if (token == null || !token.equals(i.getStringExtra("token"))) return;
                    final String mode = i.getStringExtra("mode"), text = i.getStringExtra("text");
                    final String tone = i.getStringExtra("tone"), format = i.getStringExtra("format");
                    final String ch = i.getStringExtra("style"), sp = i.getStringExtra("special");
                    final String feat = i.getStringExtra("feature"), lang = i.getStringExtra("lang");
                    new Thread(() -> {
                        long t0 = System.currentTimeMillis();
                        try {
                            String out = "prompt".equals(mode) ? prompt(c, text, feat, lang == null ? "es" : lang)
                                    : compose(text, tone, format, ch, sp, Bridge.pkg);
                            GLog.w("IA " + ("prompt".equals(mode) ? feat : tone + "/" + format + (ch == null ? "" : " estilo=[" + ch + "]"))
                                    + " (" + (System.currentTimeMillis() - t0) + " ms): [" + text + "] -> [" + out + "]");
                        } catch (Throwable e) {
                            Throwable x = e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e;
                            GLog.w("IA error: " + x);
                        }
                    }, "GboardPort-ai").start();
                }
            };
            if (android.os.Build.VERSION.SDK_INT >= 33) app.registerReceiver(r, new IntentFilter(ACTION), Context.RECEIVER_EXPORTED);
            else app.registerReceiver(r, new IntentFilter(ACTION));
            registered = true;
            GLog.w("IA: receptor de pruebas listo");
            addStyles();
        } catch (Throwable e) {
            GLog.w("IA: receptor no disponible (" + e + ")");
        }
    }
}
