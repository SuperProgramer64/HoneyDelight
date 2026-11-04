package gbport.test;
import gbport.Bridge;
import java.lang.reflect.*;
public final class SessionRegression {
 static String word="qiero";
 static Field f(String n)throws Exception{Field f=Bridge.class.getDeclaredField(n);f.setAccessible(true);return f;}
 public static void main(String[] args)throws Exception{
  Method reset;
  try{reset=Bridge.class.getDeclaredMethod("resetInputSession");}
  catch(NoSuchMethodException e){throw new AssertionError("New fields retain old correction decisions and pending prediction results",e);}
  reset.setAccessible(true);
  Method current=Bridge.class.getDeclaredMethod("predictionCurrent",long.class,String.class);current.setAccessible(true);
  Bridge.setInput((Qi.c)()->word);
  long issued=((java.util.concurrent.atomic.AtomicLong)f("inputGeneration").get(null)).get();
  if(!(Boolean)current.invoke(null,issued,"qiero"))throw new AssertionError("Current request rejected");
  word="quiero";
  if((Boolean)current.invoke(null,issued,"qiero"))throw new AssertionError("A previous prefix can publish over the latest word");
  f("decWord").set(null,"qiero");f("correctionText").set(null,"quiero");f("decResult").setInt(null,1);
  reset.invoke(null);
  word="qiero";Bridge.setInput((Qi.c)()->word);
  if((Boolean)current.invoke(null,issued,"qiero"))throw new AssertionError("An old field result is accepted in another field with the same word");
  if(f("decWord").get(null)!=null||f("correctionText").get(null)!=null||f("decResult").getInt(null)!=0)
   throw new AssertionError("Autocorrection survives a field switch");
  System.out.println("PASS stale prefix/field results rejected and previous correction cleared");
 }
}
