package gbport.test;
import gbport.*;
import java.util.*;
public final class ContextOrderRegression {
    public static void main(String[] args) throws Exception {
        String base = args[0];
        Delight.loadLibrary(base + "/libintegrated_shared_object.so", base + "/crash");
        Delight.createDecoder(new String[][]{{"es","US"},{"en","US"}}, base + "/data");
        if (!Delight.loadMainLm(base + "/lm/datadownloadfile_1788031289653", "es", "US")
                || !Delight.loadMainLm(base + "/lm/main_en_us_2026080409_2", "en", "US"))
            throw new AssertionError("Missing base fixture models");
        Delight.setQwertyLayout();
        Delight.setRuntimeParams(true, "org.telegram.messenger", false);
        for (String[] example : new String[][]{
                {"El equipo ","supero","superó"}, {"Yo ","supero","supero"},
                {"La casa ","esta","está"}, {"Esta ","casa","casa"},
                {"Yo ","supero","supero"}, {"El equipo ","supero","superó"}}) {
            List<Delight.Cand> cs = Delight.parseCands(Delight.suggestEchoRaw(example[0], "", example[1], 20));
            Decide.Result decision = Decide.decide(cs, example[1], false);
            String actual = decision.cand == null ? example[1] : decision.cand.text;
            System.out.println("CONTEXT " + example[0] + example[1] + " => " + actual);
            if (!example[2].equals(actual))
                throw new AssertionError("Changed context reused a decision: expected " + example[2] + " but got " + actual);
            Delight.suggest(example[0], "", "", 20);
        }
        System.out.println("PASS native context changes preserve first/third person in both query orders");
    }
}
