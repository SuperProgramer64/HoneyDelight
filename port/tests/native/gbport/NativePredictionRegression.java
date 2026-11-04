package gbport;
// Ejecutar con contenedores de prueba; nunca se empaqueta en el teclado.
public final class NativePredictionRegression {
 public static void main(String[] args)throws Exception{
  String base=args[0];
  Delight.loadLibrary(base+"/libintegrated_shared_object.so",base+"/crash");
  Delight.createDecoder(new String[][]{{"es","US"},{"en","US"}},base+"/data");
  if(!Delight.loadMainLm(base+"/es_us.dict","es","US")||!Delight.loadMainLm(base+"/en_us.dict","en","US"))throw new AssertionError("Missing fixture models");
  Delight.setQwertyLayout();Delight.setRuntimeParams(true,"com.google.android.googlequicksearchbox",true);
  Bridge.pkg="com.google.android.googlequicksearchbox";Bridge.state=2;Bridge.secret=false;
  int failures=0,total=0;
  long elapsed=0;
  for(int spatial=0;spatial<2;spatial++)for(int round=0;round<2;round++)for(String[] sample:new String[][]{
   {"holaxomo","hola cómo"},{"holacomo","hola cómo"},{"holacómo","hola cómo"},{"hastamanana","hasta mañana"},{"buenosdias","buenos días"},{"buenastardes","buenas tardes"},
   {"buenasnoches","buenas noches"},{"tequiero","te quiero"},{"porfavor","por favor"},
   {"muchasgracias","muchas gracias"},{"todobien","todo bien"},{"nosvemos","nos vemos"},
   {"programación","programación"},{"cocodrilo","cocodrilo"},{"mañana","mañana"},
   {"camiseta","camiseta"},{"mariposa","mariposa"},{"cancion","canción"},
   {"HoneyDelight","HoneyDelight"},{"Camila","Camila"},{"kukuruzo","kukuruzo"},
   {"jajaja","jajaja"},{"nooooo","nooooo"}}){
   if (args.length > 1 && !sample[0].startsWith(args[1])) continue;
   Split.clearCache();String typed=sample[0];Bridge.Out result=null;
   for(int i=1;i<=typed.length();i++){
    String prefix=typed.substring(0,i);float[][] points=null;
    if(spatial!=0){points=new float[2][i];for(int k=0;k<i;k++){float[] center=Delight.center(prefix.charAt(k));points[0][k]=center[0]+5;points[1][k]=center[1]-4;}}
    long started=System.nanoTime();
    result=Bridge.compute(null,null,prefix,false,System.nanoTime()+55_000_000L,points,new EditorContext("",""));
    elapsed+=System.nanoTime()-started;
   }
   if (args.length > 1) System.out.println("DETAIL whole="+java.util.Arrays.toString(Split.lit.get(typed))
       +" left="+java.util.Arrays.toString(Split.lit.get("hola"))+" right="
       +java.util.Arrays.toString(Split.rights.get("hola|"+typed.substring(4))));
   String best=result.p==null||result.p.isEmpty()?"":result.p.get(0).getPrediction();total++;
   boolean pass=sample[1].equals(best);if(!pass)failures++;
   StringBuilder all=new StringBuilder();if(result.p!=null)for(int k=0;k<Math.min(3,result.p.size());k++)all.append('|').append(result.p.get(k).getPrediction());
   System.out.println("REPLAY spatial="+spatial+" round="+round+" "+typed+" => "+best+" "+pass+" "+all);
  }
  System.out.println("SUMMARY "+(total-failures)+"/"+total+" total_compute_ms="+elapsed/1_000_000);System.exit(failures==0?0:1);
 }
}
