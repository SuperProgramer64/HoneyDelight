package gbport.test;
import gbport.Bridge;
import java.lang.reflect.*;
import java.util.*;
public final class PredictionRegression {
    public static void main(String[] args) throws Exception {
        Method tail = Bridge.class.getDeclaredMethod("correctedSearchTail", String.class, String.class, String.class);
        tail.setAccessible(true);
        if (!"quiero ".equals(tail.invoke(null, "qiero ", "qiero", "quiero")))
            throw new AssertionError("Google space correction lost");
        if (tail.invoke(null, "otroqiero ", "qiero", "quiero") != null)
            throw new AssertionError("Unrelated word suffix corrected");
        if (tail.invoke(null, "qiero x", "qiero", "quiero") != null)
            throw new AssertionError("Changed search text corrected");
        Method inputType = Bridge.class.getDeclaredMethod("inputTypeFor", String.class, int.class);
        inputType.setAccessible(true);
        if ((Integer) inputType.invoke(null, "com.google.android.googlequicksearchbox", 0xac001) != 0x2c001)
            throw new AssertionError("Google disables corrections despite being a text field");
        if ((Integer) inputType.invoke(null, "com.whatsapp", 0xac001) != 0xac001)
            throw new AssertionError("Unrelated app flags changed");
        if ((Integer) inputType.invoke(null, "com.google.android.googlequicksearchbox", 0x80081) != 0x80081)
            throw new AssertionError("Password field flags changed");
        Field state = Bridge.class.getDeclaredField("state");
        state.setAccessible(true); state.setInt(null, 2);
        Field secret = Bridge.class.getDeclaredField("secret");
        secret.setAccessible(true); secret.setBoolean(null, false);
        Method owns = Bridge.class.getDeclaredMethod("delightOwns", String.class);
        owns.setAccessible(true);
        if (!(Boolean) owns.invoke(null, "hola")) throw new AssertionError("Supported input not owned");
        if (Bridge.decideAutoReplace() != 0) throw new AssertionError("Samsung may autocorrect after a missed decision");
        secret.setBoolean(null, true);
        if ((Boolean) owns.invoke(null, "hola")) throw new AssertionError("Private field intercepted");
        secret.setBoolean(null, false);
        Method learnable = Bridge.class.getDeclaredMethod("learnable", String.class);
        learnable.setAccessible(true);
        if ((Boolean) learnable.invoke(null, "TextoPegado")) throw new AssertionError("Unobserved pasted word learned");
        Field scores = Bridge.class.getDeclaredField("litSeen"); scores.setAccessible(true);
        Map<String, Double> seen = (Map<String, Double>) scores.get(null);
        seen.put("hola", -2.0);
        if (!(Boolean) learnable.invoke(null, "hola")) throw new AssertionError("Known typed word rejected");
        seen.put("apodopropio", Double.NaN);
        if ((Boolean) learnable.invoke(null, "apodopropio")) throw new AssertionError("Unknown word learned first time");
        if (!(Boolean) learnable.invoke(null, "apodopropio")) throw new AssertionError("Repeated name never learned");
        if ((Boolean) learnable.invoke(null, "nooooo")) throw new AssertionError("Elongation learned");
        Field fixes = Bridge.class.getDeclaredField("fixSeen"); fixes.setAccessible(true);
        ((Map<String,String>) fixes.get(null)).put("ytu", "Y tú");
        seen.put("ytu", -5.0);
        if ((Boolean) learnable.invoke(null, "ytu"))
            throw new AssertionError("A known missing-space typo was learned as a valid word");
        System.out.println("PASS Delight ownership, missed decision, private input, pasted text, known word, repeated name");
    }
}
