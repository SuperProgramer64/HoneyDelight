package gbport.test;
import java.lang.reflect.*;
public class PrivacyRegression {
 public static void main(String[] args)throws Exception {
  Class<?> c=Class.forName("gbport.GLog");Method w=c.getDeclaredMethod("w",String.class);w.setAccessible(true);
  Field calls=Class.forName("android.util.Log").getField("calls");int before=calls.getInt(null);w.invoke(null,"private diagnostic sample");
  if(calls.getInt(null)!=before)throw new AssertionError("Default logging exposes entered text to logcat");
  Field enabled=c.getDeclaredField("enabled");enabled.setAccessible(true);enabled.setBoolean(null,true);
  w.invoke(null,"explicitly enabled test diagnostic");
  if(calls.getInt(null)!=before+1)throw new AssertionError("Explicit diagnostics unavailable");
  enabled.setBoolean(null,false);w.invoke(null,"disabled again");
  if(calls.getInt(null)!=before+1)throw new AssertionError("Disabling did not stop logcat output");
  System.out.println("PASS no default text logging, explicit opt-in and disabling");
 }
}
