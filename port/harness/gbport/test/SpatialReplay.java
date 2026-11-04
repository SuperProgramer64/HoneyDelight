package gbport.test;

import gbport.Decide;
import gbport.Delight;
import java.nio.file.*;
import java.util.*;

/** Offline temporal holdout. This never changes the running keyboard or its history. */
public final class SpatialReplay {
    private static final String[] ROWS = {"qwertyuiop", "asdfghjklñ", "zxcvbnm"};
    private static final float WIDTH = 108, HEIGHT = 160;
    private static final float[] INDENT = {0, 0, 162};

    private static float[][] points(String line, String word) {
        String[] tokens = line.split(";");
        if (tokens.length != word.length()) return null;
        float[][] result = new float[2][tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[0][i] = result[1][i] = Float.NaN;
            if ("-".equals(tokens[i])) continue;
            String[] xy = tokens[i].split(",");
            if (xy.length != 2) return null;
            result[0][i] = Float.parseFloat(xy[0]);
            result[1][i] = Float.parseFloat(xy[1]);
        }
        return result;
    }

    public static void run(String path) throws Exception {
        List<String[]> records = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(path))) {
            String[] fields = line.split("\t", -1);
            if (fields.length >= 4 && points(fields[3], fields[0]) != null) records.add(fields);
        }
        int training = records.size() / 2;
        Map<Character, double[]> samples = new HashMap<>();
        for (String[] fields : records.subList(0, training)) {
            String typed = fields[0], committed = fields[1];
            if (!typed.equalsIgnoreCase(committed)) continue;
            float[][] points = points(fields[3], typed);
            for (int i = 0; i < typed.length(); i++) {
                char key = Character.toLowerCase(typed.charAt(i));
                float[] center = Delight.center(key);
                if (center == null || !Float.isFinite(points[0][i]) || !Float.isFinite(points[1][i])) continue;
                double dx = points[0][i] - center[0], dy = points[1][i] - center[1];
                if (Math.abs(dx) > WIDTH / 2 || Math.abs(dy) > HEIGHT / 2) continue;
                double[] stats = samples.computeIfAbsent(key, k -> new double[3]);
                stats[0] += dx; stats[1] += dy; stats[2]++;
            }
        }
        String letters = String.join("", ROWS);
        int total = letters.length() + 1, adapted = 0;
        int[] codes = new int[total];
        float[] x = new float[total], y = new float[total], w = new float[total], h = new float[total];
        for (int i = 0; i < letters.length(); i++) {
            char key = letters.charAt(i);
            float[] center = Delight.center(key);
            codes[i] = key; x[i] = center[0]; y[i] = center[1]; w[i] = WIDTH; h[i] = HEIGHT;
            double[] stats = samples.get(key);
            if (stats != null && stats[2] >= 8) {
                double shrinkage = stats[2] / (stats[2] + 20);
                x[i] += stats[0] / stats[2] * shrinkage;
                y[i] += stats[1] / stats[2] * shrinkage;
                adapted++;
            }
        }
        codes[total - 1] = ' '; x[total - 1] = 540; y[total - 1] = HEIGHT * 3.5f;
        w[total - 1] = 540; h[total - 1] = HEIGHT;
        int[] correct = new int[3];
        try {
            for (int mode = 0; mode < 3; mode++) {
                if (mode < 2) Delight.setQwertyLayout();
                else Delight.setLayout(WIDTH, HEIGHT, 1080, HEIGHT * 4, codes, x, y, w, h);
                for (String[] fields : records.subList(training, records.size())) {
                    float[][] points = points(fields[3], fields[0]);
                    Decide.Result result = Decide.decide(Delight.parseCands(Delight.suggestEchoRaw(
                            fields[2].replace('⏎', '\n'), "", fields[0], 20,
                            mode == 0 ? null : points[0], mode == 0 ? null : points[1], null)), fields[0], false);
                    String predicted = result.cand == null ? fields[0] : result.cand.text;
                    if (predicted.equalsIgnoreCase(fields[1])) correct[mode]++;
                }
            }
        } finally { Delight.setQwertyLayout(); }
        System.out.println("SPATIAL_HOLDOUT training=" + training + " test=" + (records.size()-training)
                + " adaptedKeys=" + adapted + " centers=" + correct[0]
                + " actualTouches=" + correct[1] + " personalizedCenters=" + correct[2]);
    }
}
