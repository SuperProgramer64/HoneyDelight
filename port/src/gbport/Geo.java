package gbport;

import java.util.Map;

// Geometría del swipe: pasa puntos del teclado real de Samsung al marco sintético del layout del motor
// (Delight.ROWS/KW/KH/OFF) con un ajuste lineal por fila de los centros reales de las teclas.
public final class Geo {
    static final String[] ROWS = {"qwertyuiop", "asdfghjklñ", "zxcvbnm"};

    private Geo() {}

    // Por fila: {pendiente x por columna, x de la columna 0, y media}. null si falta alguna fila.
    static float[][] rowFit(Map<Character, float[]> keys) {
        float[][] rf = new float[3][];
        for (int r = 0; r < 3; r++) {
            double n = 0, sc = 0, sx = 0, scc = 0, scx = 0, sy = 0;
            for (int ci = 0; ci < ROWS[r].length(); ci++) {
                float[] k = keys.get(ROWS[r].charAt(ci));
                if (k == null || k.length < 2 || !Float.isFinite(k[0]) || !Float.isFinite(k[1])) continue;
                n++; sc += ci; sx += k[0]; scc += ci * ci; scx += ci * k[0]; sy += k[1];
            }
            double d = n * scc - sc * sc;
            if (n < 2 || d == 0) return null;
            double a = (n * scx - sc * sx) / d;
            if (a <= 0) return null;
            rf[r] = new float[]{(float) a, (float) ((sx - a * sc) / n), (float) (sy / n)};
        }
        if (!(rf[0][2] < rf[1][2] && rf[1][2] < rf[2][2])) return null;
        return rf;
    }

    static float[] toEngine(float x, float y, float[][] rf) {
        float t;
        if (y <= rf[1][2]) t = (y - rf[0][2]) / (rf[1][2] - rf[0][2]);
        else t = 1 + (y - rf[1][2]) / (rf[2][2] - rf[1][2]);
        int r0 = t < 1 ? 0 : 1;
        float f = t - r0;
        float x0 = Delight.OFF[r0] + Delight.KW * ((x - rf[r0][1]) / rf[r0][0]) + Delight.KW / 2;
        float x1 = Delight.OFF[r0 + 1] + Delight.KW * ((x - rf[r0 + 1][1]) / rf[r0 + 1][0]) + Delight.KW / 2;
        return new float[]{x0 * (1 - f) + x1 * f, Delight.KH * t + Delight.KH / 2};
    }

    // Pruebas: teclado "real" simulado (otra escala, origen e indentación) y trazo de una palabra en ese marco.
    public static String testDecode(String before, String word, float kw, float kh, float x0, float y0, float indent2, float jitter) {
        Map<Character, float[]> keys = new java.util.HashMap<>();
        float[] ind = {0, 0, indent2};
        for (int r = 0; r < 3; r++)
            for (int ci = 0; ci < ROWS[r].length(); ci++)
                keys.put(ROWS[r].charAt(ci), new float[]{x0 + (ind[r] + ci) * kw + kw / 2, y0 + r * kh + kh / 2, kw, kh});
        java.util.List<float[]> pts = new java.util.ArrayList<>();
        java.util.Random rnd = new java.util.Random(word.hashCode());
        float px = Float.NaN, py = Float.NaN;
        for (int i = 0; i < word.length(); i++) {
            float[] k = keys.get(Delight.base(Character.toLowerCase(word.charAt(i))));
            if (k == null) continue;
            float cx = k[0] + (float) rnd.nextGaussian() * jitter * kw, cy = k[1] + (float) rnd.nextGaussian() * jitter * kh;
            if (!Float.isNaN(px)) {
                int s = Math.max(1, (int) (Math.hypot(cx - px, cy - py) / (kw / 5)));
                for (int j = 1; j <= s; j++) pts.add(new float[]{px + (cx - px) * j / s, py + (cy - py) * j / s});
            } else pts.add(new float[]{cx, cy});
            px = cx; py = cy;
        }
        float[][] rf = rowFit(keys);
        float[] ex = new float[pts.size()], ey = new float[pts.size()];
        long[] t = new long[pts.size()];
        for (int i = 0; i < pts.size(); i++) {
            float[] e = toEngine(pts.get(i)[0], pts.get(i)[1], rf);
            ex[i] = e[0]; ey[i] = e[1]; t[i] = 1000 + i * 12L;
        }
        byte[] r = Delight.decodeGestureRaw(before, "", ex, ey, t, 8);
        StringBuilder sb = new StringBuilder("[" + Delight.gestureText(r) + "] ");
        for (Delight.Cand c : Delight.parseCands(r)) sb.append(c.text).append(' ');
        return sb.toString();
    }
}
