package gbport.test;
import java.lang.reflect.*;
public final class EditorContextRegression {
    static Method capture;
    static Field before, after;
    static Object snapshot(CharSequence text, int start, int end, String word) throws Exception {
        return capture.invoke(null, text, start, end, word);
    }
    static void check(CharSequence text, int start, int end, String word, String b, String a) throws Exception {
        Object s = snapshot(text, start, end, word);
        if (s == null || !b.equals(before.get(s)) || !a.equals(after.get(s)))
            throw new AssertionError("Wrong local editor context for " + text);
    }
    public static void main(String[] args) throws Exception {
        Class<?> type;
        try { type = Class.forName("gbport.EditorContext"); }
        catch (ClassNotFoundException e) { throw new AssertionError("Local editor still needs synchronous InputConnection reads", e); }
        capture = type.getDeclaredMethod("fromSelection", CharSequence.class, int.class, int.class, String.class);
        capture.setAccessible(true);
        before = type.getDeclaredField("before"); before.setAccessible(true);
        after = type.getDeclaredField("after"); after.setAccessible(true);
        check("Hola Ytú como", 6, 6, "Ytú", "Hola ", " como");
        check("Más largo y narrativo", 20, 20, "narrativo", "Más largo y ", "");
        check("hola mundo", 0, 4, "", "", " mundo");
        check("hola mundo", 4, 0, "", "", " mundo");
        check("hola mundo", 5, 5, "", "hola ", "mundo");
        StringBuilder mutable = new StringBuilder("Hola qiero");
        Object frozen = snapshot(mutable, 10, 10, "qiero"); mutable.setLength(0);
        if (!"Hola ".equals(before.get(frozen))) throw new AssertionError("Worker context changed after capture");
        if (snapshot("hola", -1, 0, "") != null || snapshot("hola", 0, 5, "") != null)
            throw new AssertionError("Invalid selection used as context");
        System.out.println("PASS local editor composition, selection, immutable snapshot, invalid bounds");
    }
}
