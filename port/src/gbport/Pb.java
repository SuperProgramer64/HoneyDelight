package gbport;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Escritor/lector mínimo de protobuf (wire format) para hablar con Delight5 sin el runtime de Gboard.
public final class Pb {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public static Pb n() { return new Pb(); }

    private void varint(long v) {
        while ((v & ~0x7FL) != 0) { out.write((int) ((v & 0x7F) | 0x80)); v >>>= 7; }
        out.write((int) v);
    }

    private void tag(int f, int wt) { varint(((long) f << 3) | wt); }

    public Pb i(int f, long v) { tag(f, 0); varint(v); return this; }
    public Pb b(int f, boolean v) { return i(f, v ? 1 : 0); }
    public Pb f(int f, float v) {
        tag(f, 5);
        int x = Float.floatToIntBits(v);
        out.write(x); out.write(x >> 8); out.write(x >> 16); out.write(x >> 24);
        return this;
    }
    public Pb s(int f, String v) { return y(f, v.getBytes(StandardCharsets.UTF_8)); }
    public Pb y(int f, byte[] v) { tag(f, 2); varint(v.length); out.write(v, 0, v.length); return this; }
    public Pb m(int f, Pb v) { return y(f, v.bytes()); }
    public byte[] bytes() { return out.toByteArray(); }

    // Campo leído de un mensaje.
    public static final class F {
        public final int num, wt; public final long v; public final byte[] d;
        F(int num, int wt, long v, byte[] d) { this.num = num; this.wt = wt; this.v = v; this.d = d; }
        public String str() { return new String(d, StandardCharsets.UTF_8); }
        public float flt() { return Float.intBitsToFloat((int) v); }
    }

    public static List<F> parse(byte[] b) {
        List<F> r = new ArrayList<>();
        if (b == null) return r;
        int[] p = {0};
        while (p[0] < b.length) {
            long key = rv(b, p);
            int num = (int) (key >>> 3), wt = (int) (key & 7);
            if (wt == 0) r.add(new F(num, wt, rv(b, p), null));
            else if (wt == 1) { long v = 0; for (int k = 0; k < 8; k++) v |= (b[p[0] + k] & 0xFFL) << (8 * k); p[0] += 8; r.add(new F(num, wt, v, null)); }
            else if (wt == 5) { long v = 0; for (int k = 0; k < 4; k++) v |= (b[p[0] + k] & 0xFFL) << (8 * k); p[0] += 4; r.add(new F(num, wt, v, null)); }
            else if (wt == 2) { int len = (int) rv(b, p); byte[] d = new byte[len]; System.arraycopy(b, p[0], d, 0, len); p[0] += len; r.add(new F(num, wt, len, d)); }
            else throw new IllegalArgumentException("wire type " + wt);
        }
        return r;
    }

    private static long rv(byte[] b, int[] p) {
        long r = 0; int sh = 0;
        while (true) { int c = b[p[0]++] & 0xFF; r |= (long) (c & 0x7F) << sh; if ((c & 0x80) == 0) return r; sh += 7; }
    }

    public static List<F> all(List<F> fs, int num) {
        List<F> r = new ArrayList<>();
        for (F x : fs) if (x.num == num) r.add(x);
        return r;
    }

    public static F one(List<F> fs, int num) {
        for (F x : fs) if (x.num == num) return x;
        return null;
    }

    // Volcado legible para depurar respuestas desconocidas.
    public static String dump(byte[] b, String ind) {
        StringBuilder sb = new StringBuilder();
        try {
            for (F x : parse(b)) {
                sb.append(ind).append(x.num).append(':');
                if (x.wt == 2) {
                    boolean printable = true;
                    for (byte c : x.d) if ((c & 0xFF) < 0x20 && c != '\n' && c != '\t') { printable = false; break; }
                    String sub = null;
                    if (!printable || x.d.length == 0) { try { parse(x.d); sub = dump(x.d, ind + "  "); } catch (Throwable t) { sub = null; } }
                    if (sub != null && !sub.isEmpty()) sb.append(" {\n").append(sub).append(ind).append("}\n");
                    else sb.append(" \"").append(x.str().replace("\n", "\\n")).append("\"\n");
                } else if (x.wt == 5) sb.append(' ').append(x.flt()).append('\n');
                else sb.append(' ').append(x.v).append('\n');
            }
        } catch (Throwable t) {
            sb.append(ind).append("<bytes ").append(b.length).append(">\n");
        }
        return sb.toString();
    }
}
