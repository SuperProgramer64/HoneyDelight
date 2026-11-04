package gbport.test;
import gbport.Split;
import java.lang.reflect.Method;
public final class CommonBoundaryRegression {
 public static void main(String[] args)throws Exception{
  Method clear=Split.class.getDeclaredMethod("clearCache");clear.setAccessible(true);
  for(String[] example:new String[][]{
   {"buenosdias","buenos días"},{"buenosdías","buenos días"},{"Buenasnoches","Buenas noches"},
   {"BUENASTARDES","BUENAS TARDES"},{"hastamanana","hasta mañana"},{"porfavor","por favor"},
   {"muchasgracias","muchas gracias"},{"nosvemos","nos vemos"},{"tequiero","te quiero"}}){
   clear.invoke(null);
   Split.Result result=Split.update("", "",example[0],System.nanoTime()-1);
   if(result==null||!example[1].equals(result.text))
    throw new AssertionError("Common boundary depends on optional native budget: "+example[0]);
   if(!Split.wins(result,Double.NaN,-18))throw new AssertionError("A validated phrase loses to a glued correction");
   if(Split.wins(result,-3,-2))throw new AssertionError("A common phrase overrides a known literal");
  }
  for(String word:new String[]{"programación","Camila","cocodrilo","HoneyDelight","kukuruzo","nooooo","PorFavor","buenosdias.com","www.buenosdias","hola@buenosdias","buenosdias123","quet","supero","portavoz","porque"}){
   clear.invoke(null);
   if(Split.update("","",word,System.nanoTime()-1)!=null)
    throw new AssertionError("False local split: "+word);
  }
  clear.invoke(null);
  if(Split.update("https://","","buenosdias",System.nanoTime()-1)!=null)
    throw new AssertionError("Common boundary changed a URL token");
  clear.invoke(null);
  if(Split.update("user@","","buenosdias",System.nanoTime()-1)!=null)
    throw new AssertionError("Common boundary changed an email token");
  System.out.println("PASS deterministic common phrases, accent/case, known words, names, URLs and email");
 }
}
