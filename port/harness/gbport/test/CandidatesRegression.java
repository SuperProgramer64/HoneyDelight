package gbport.test;
import gbport.*;
import java.util.*;
public final class CandidatesRegression {
    static List<Delight.Cand> candidates(String[] text, int[] type, float[] scores) {
        Pb list = Pb.n();
        for (int i = 0; i < text.length; i++) list.m(1,
            Pb.n().i(1, type[i]).s(2, text[i]).f(4, scores[i]));
        return Delight.parseCands(Pb.n().m(2, Pb.n().m(3, list)).bytes());
    }
    public static void main(String[] args) throws Exception {
        List<Delight.Cand> values = candidates(
            new String[]{"Ytú", "Ytútútú", "Y tú"}, new int[]{1,0,0}, new float[]{-30,-1,-10});
        Decide.Result choice = Decide.decide(values, "Ytú", false);
        if (choice.cand == null || !"Y tú".equals(choice.cand.text))
            throw new AssertionError("A highly scored decoder loop beats the valid word separation");
        java.lang.reflect.Method rank = Decide.class.getDeclaredMethod("rankCandidates", List.class, String.class);
        List<Delight.Cand> ordered = (List<Delight.Cand>)rank.invoke(null, candidates(
            new String[]{"programador", "programación", "programa", "progra", "corrupta"},
            new int[]{13,13,13,1,0}, new float[]{-20,-8,-12,Float.NaN,Float.POSITIVE_INFINITY}), "progra");
        if (ordered.size() != 4 || !"programación".equals(ordered.get(0).text)
                || !"programa".equals(ordered.get(1).text))
            throw new AssertionError("Suggestions are not ranked by finite decoder score");
        List<Delight.Cand> valid = (List<Delight.Cand>)rank.invoke(null, candidates(
            new String[]{"cocodrilo", "coco"}, new int[]{13,1}, new float[]{-8,-2}), "coco");
        if (valid.size()!=2) throw new AssertionError("A valid repeated-syllable completion was removed");
        java.lang.reflect.Method display = Decide.class.getDeclaredMethod("displayAlternative", String.class, String.class, String.class);
        for (String bad : new String[]{"Buenosdigas", "buenosdirás", "buenosdías"})
            if ((Boolean)display.invoke(null,"buenosdias","buenos días",bad))
                throw new AssertionError("An accepted separation still displays joined alternatives: "+bad);
        if ((Boolean)display.invoke(null,"holaxomo","hola cómo","Holacómo"))
            throw new AssertionError("A typo-corrected split still displays the glued alternative");
        if (!(Boolean)display.invoke(null,"buenosdias","buenos días","buenos días a todos")
                || !(Boolean)display.invoke(null,"progra","programa","programación")
                || !(Boolean)display.invoke(null,"hola"," hola","holas"))
            throw new AssertionError("Valid phrase/completion/punctuation alternatives were hidden");
        System.out.println("PASS scored loops rejected, finite ranking, unknown literal and valid completions");
    }
}
