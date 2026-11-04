package gbport.test;
import gbport.EditCommit;
public final class EditRegression {
    public static void main(String[] args) throws Exception {
        java.lang.reflect.Method matches = EditCommit.class.getDeclaredMethod(
                "matches", String.class, int.class, CharSequence.class, int.class);
        matches.setAccessible(true);
        if (!(Boolean) matches.invoke(null, "antes y después", 0, "antes y después", 0))
            throw new AssertionError("Unchanged target rejected");
        if ((Boolean) matches.invoke(null, "antes y después", 0, "texto cambiado", 0))
            throw new AssertionError("User changes would be overwritten");
        if ((Boolean) matches.invoke(null, "texto", 0, "texto", 5))
            throw new AssertionError("Different extracted range accepted");
        if ((Boolean) matches.invoke(null, "texto", 0, null, 0))
            throw new AssertionError("Missing field accepted");
        System.out.println("PASS unchanged field, user changes, extracted offset, missing text");
    }
}
