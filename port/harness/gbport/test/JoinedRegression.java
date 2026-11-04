package gbport.test;
import gbport.Decide;
import java.lang.reflect.Method;
public final class JoinedRegression {
    public static void main(String[] args) throws Exception {
        Method context;
        try { context = gbport.Bridge.class.getDeclaredMethod("afterComposition", String.class, String.class, String.class); }
        catch (NoSuchMethodException e) { throw new AssertionError("Composing suffix is duplicated after the cursor", e); }
        context.setAccessible(true);
        for (String[] example : new String[][]{
                {"Hola Y","tú como","Ytú"," como"},
                {"Hola Yt","ú como","Ytú"," como"},
                {"Hola Ytú"," tú como","Ytú"," tú como"},
                {"Hola ","Ytú como","Ytú"," como"},
                {"Hola ","mundo","Ytú","mundo"}})
            if (!example[3].equals(context.invoke(null,example[0],example[1],example[2])))
                throw new AssertionError("Incorrect composing context");
        Method repeated;
        try { repeated = Decide.class.getDeclaredMethod("repeatedExpansion", String.class, String.class); }
        catch (NoSuchMethodException e) { throw new AssertionError("Repeated decoder expansions reach suggestions", e); }
        repeated.setAccessible(true);
        for (String value : new String[]{"Ytytúú", "Ytytútú", "YtúYtútú", "Ytútútú"})
            if (!(Boolean) repeated.invoke(null, "Ytú", value)) throw new AssertionError("Loop accepted: " + value);
        for (String[] pair : new String[][]{{"coco","cocodrilo"},{"progra","programación"},{"Ytú","Y tú"},{"Ytú","Tú"}})
            if ((Boolean) repeated.invoke(null, pair[0],pair[1])) throw new AssertionError("Valid suggestion rejected");
        System.out.println("PASS repeated decoder expansions, valid completions, word separation");
    }
}
