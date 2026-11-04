package gbport.test;
import gbport.Geo;
import java.util.*;
import java.lang.reflect.*;
public final class GeometryRegression {
    public static void main(String[] args) throws Exception {
        Method fit = Geo.class.getDeclaredMethod("rowFit", Map.class); fit.setAccessible(true);
        Map<Character, float[]> keys = new HashMap<>();
        keys.put('q', new float[]{10, 20}); keys.put('p', new float[]{910, 20});
        keys.put('a', new float[]{10, 120}); keys.put('ñ', new float[]{910, 120});
        keys.put('z', new float[]{160, 220}); keys.put('m', new float[]{760, 220});
        if (fit.invoke(null, keys) == null) throw new AssertionError("Valid keyboard rejected");
        keys.put('q', new float[]{Float.NaN, 20});
        if (fit.invoke(null, keys) != null) throw new AssertionError("Invalid x contaminated the spatial transform");
        keys.put('q', new float[]{Float.POSITIVE_INFINITY, 20});
        if (fit.invoke(null, keys) != null) throw new AssertionError("Infinite x contaminated the spatial transform");
        System.out.println("PASS valid geometry, non-finite layout coordinates");
    }
}
