package gbport.test;
import java.lang.reflect.*;
public final class CorrectionContextRegression {
 public static void main(String[] args)throws Exception{
  Class<?> type=Class.forName("gbport.EditorContext");
  Method from=type.getDeclaredMethod("fromRaw",String.class,String.class,String.class);from.setAccessible(true);
  Field before=type.getDeclaredField("before"),after=type.getDeclaredField("after");before.setAccessible(true);after.setAccessible(true);
  for(String[] sample:new String[][]{
   {"holacómo","","holaxomo","",""},
   {"Hola holacómo","","holaxomo","Hola ",""},
   {"holacómo","","holacomo","",""},
   {"Ahora holacóm","o después","holaxomo","Ahora "," después"},
   {"otra cosa","","holaxomo","otra cosa",""},
   {"como ","","holaxomo","como ",""},
   {"cocodrilo","","coco","",""}}){
   Object context=from.invoke(null,sample[0],sample[1],sample[2]);
   if(!sample[3].equals(before.get(context))||!sample[4].equals(after.get(context)))
    throw new AssertionError("Old corrected composition becomes prior context: "+sample[0]+" / "+sample[2]);
  }
  System.out.println("PASS corrected accents/typos are excluded from context without consuming unrelated words");
 }
}
