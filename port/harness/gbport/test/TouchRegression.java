package gbport.test;
import java.lang.reflect.*;
import java.util.*;
import gbport.Bridge;
import com.microsoft.fluency.Point;
public final class TouchRegression {
    public static class Sample {
        public Point a;
        public int b;
        Sample(float x, int code) { a = new Point() {
            public float getX() { return x; }
            public float getY() { return 100; }
        }; b = code; }
    }
    public static class History { public List<Sample> d = new ArrayList<>(); }
    public static class Input { public History e = new History(); }
    static void field(String name, Field value) throws Exception {
        Field target = Bridge.class.getDeclaredField(name); target.setAccessible(true); target.set(null, value);
    }
    public static void main(String[] args) throws Exception {
        field("fK", Input.class.getField("e"));
        field("fList", History.class.getField("d"));
        field("fPt", Sample.class.getField("a"));
        field("fCode", Sample.class.getField("b"));
        Method read = Bridge.class.getDeclaredMethod("realXY", Object.class, String.class);
        read.setAccessible(true);
        Input input = new Input();
        input.e.d.add(new Sample(42, 'a'));
        float[][] points = (float[][]) read.invoke(null, input, "hola");
        if (points[2][0] != 0 || points[2][3] != 1 || points[0][3] != 42)
            throw new AssertionError("Partial touch history must align to word suffix");
        Field samples = Bridge.class.getDeclaredField("tapSamples");
        samples.setAccessible(true);
        int count = samples.getInt(null);
        read.invoke(null, input, "hola");
        if (samples.getInt(null) != count)
            throw new AssertionError("Repeated predictions counted the same physical touch twice");
        input.e.d.add(new Sample(80, 's'));
        read.invoke(null, input, "holas");
        if (samples.getInt(null) != count + 1)
            throw new AssertionError("A new physical touch was not counted exactly once");
        input.e.d.clear(); input.e.d.add(new Sample(Float.NaN, 0));
        points = (float[][]) read.invoke(null, input, "a");
        if (points[2][0] != 0) throw new AssertionError("Non-finite point accepted");
        input.e.d.clear(); input.e.d.add(new Sample(42, 'x'));
        points = (float[][]) read.invoke(null, input, "a");
        if (points[2][0] != 0) throw new AssertionError("Mismatched character accepted");
        System.out.println("PASS partial history, non-finite points, character mismatch");
    }
}
