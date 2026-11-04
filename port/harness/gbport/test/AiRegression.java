package gbport.test;
import gbport.Ai;
public final class AiRegression {
    public static void main(String[] args) throws Exception {
        java.lang.reflect.Method check;
        try { check = Ai.class.getDeclaredMethod("serviceError", String.class); }
        catch (NoSuchMethodException e) { throw new AssertionError("Service errors are accepted as rewritten text", e); }
        check.setAccessible(true);
        for (String value : new String[]{"response error: Violent Content", " Invalid authentication parameter ", "[ERROR: timeout]", "error"})
            if (!(Boolean) check.invoke(null, value)) throw new AssertionError("Service error accepted: " + value);
        for (String value : new String[]{"Corrige este error.", "El contenido violento aparece en el informe.", "Texto válido"})
            if ((Boolean) check.invoke(null, value)) throw new AssertionError("Valid text rejected");
        java.lang.reflect.Method run = Ai.class.getDeclaredMethod("run", String.class, String.class, String.class);
        run.setAccessible(true);
        Object original = run.invoke(null, "gb_estilo", "Izuku vuelve a casa.", "instrucción anterior");
        if (!"Izuku vuelve a casa.".equals(original))
            throw new AssertionError("Personal style must initially show the original text");
        java.lang.reflect.Method inline = Ai.class.getDeclaredMethod("inlineStyle", String.class);
        inline.setAccessible(true);
        String[] closed = (String[]) inline.invoke(null, "Hola // más claro //");
        if (closed == null || !"Hola".equals(closed[0]) || !"más claro".equals(closed[1]))
            throw new AssertionError("Closed style instruction lost");
        if (inline.invoke(null, "Mira https://example.com/ruta") != null)
            throw new AssertionError("URL parsed as a style");
        String[] open = (String[]) inline.invoke(null, "Hola // con emojis");
        if (open == null || !"con emojis".equals(open[1]))
            throw new AssertionError("Open instruction lost");
        java.lang.reflect.Field input = Ai.class.getDeclaredField("prefetchText");
        java.lang.reflect.Field attempt = Ai.class.getDeclaredField("prefetchAttempt");
        java.lang.reflect.Field pendingField = Ai.class.getDeclaredField("pending");
        java.lang.reflect.Field resultsField = Ai.class.getDeclaredField("results");
        input.setAccessible(true); attempt.setAccessible(true);
        pendingField.setAccessible(true); resultsField.setAccessible(true);
        input.set(null, "mensaje de prueba");
        java.lang.reflect.Field style = Ai.class.getDeclaredField("prefetchStyle");
        style.setAccessible(true);
        style.set(null, "");
        attempt.setLong(null, System.nanoTime() - 6_000_000_000L);
        java.util.Map pending = (java.util.Map) pendingField.get(null);
        java.util.Map results = (java.util.Map) resultsField.get(null);
        java.util.concurrent.CompletableFuture<String> failed =
                java.util.concurrent.CompletableFuture.completedFuture("");
        pending.put("gb_emoji", failed);
        results.put("gb_emoji", "");
        java.util.concurrent.CompletableFuture<String> successful =
                java.util.concurrent.CompletableFuture.completedFuture("texto válido");
        pending.put("gb_corto", successful);
        results.put("gb_corto", "texto válido");
        java.lang.reflect.Method prefetch = Ai.class.getDeclaredMethod("prefetch", String.class);
        prefetch.setAccessible(true);
        prefetch.invoke(null, "mensaje de prueba");
        Object retry = pending.get("gb_emoji");
        if (retry == failed) throw new AssertionError("Empty cached response never retries");
        if (pending.get("gb_corto") != successful) throw new AssertionError("Successful result was discarded");
        prefetch.invoke(null, "mensaje de prueba");
        if (pending.get("gb_emoji") != retry) throw new AssertionError("Repeated calls duplicated the retry");
        style.set(null, "instrucción anterior");
        prefetch.invoke(null, "mensaje de prueba");
        if (pending.get("gb_corto") == successful)
            throw new AssertionError("Changed personal style reused the previous generation");
        System.out.println("PASS service errors, inline commands, URLs, retry cooldown, changed personal style");
    }
}
