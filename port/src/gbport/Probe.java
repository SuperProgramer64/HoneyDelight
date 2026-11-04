package gbport;

// Prueba de carga: si esta clase se encuentra al ejecutar el APK original de Samsung,
// el código vino del vdex modificado (el APK original no contiene gbport).
public final class Probe {
    public static void main(String[] args) throws Exception {
        System.out.println("PROBE gbport cargado desde vdex");
        Class<?> j = Class.forName("aj.j");
        System.out.println("PROBE aj.j=" + j.getName() + " bridge=" + Bridge.class.getName());
    }
}
