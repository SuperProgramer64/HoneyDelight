package gbport.test;
import gbport.Bridge;
import com.microsoft.fluency.*;
import java.lang.reflect.*;
public final class PrivateFieldRegression {
 public static void main(String[] args)throws Exception {
  Field state=Bridge.class.getDeclaredField("state");state.setAccessible(true);state.setInt(null,2);
  Field secret=Bridge.class.getDeclaredField("secret");secret.setAccessible(true);secret.setBoolean(null,true);
  Predictor nativeSamsung=(Predictor)Proxy.newProxyInstance(Predictor.class.getClassLoader(),new Class<?>[]{Predictor.class},(proxy,method,params)->null);
  // A private field must return before context capture, scheduling, clocks or any Delight JNI.
  try {Bridge.getPredictions(nativeSamsung,null,null,null);}
  catch(Throwable e){throw new AssertionError("A private field still enters the Delight prediction pipeline",e);}
  System.out.println("PASS private fields bypass Delight before context capture or native work");
 }
}
