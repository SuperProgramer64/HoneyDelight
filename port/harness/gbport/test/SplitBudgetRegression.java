package gbport.test;
import gbport.Split;
import java.lang.reflect.*;
import java.util.*;
public final class SplitBudgetRegression {
 static Field field(String n)throws Exception{Field f=Split.class.getDeclaredField(n);f.setAccessible(true);return f;}
 public static void main(String[] args)throws Exception{
  field("ctxKey").set(null,"fixture context ");field("afterKey").set(null,"");
  Constructor<Split.Result> ctor=Split.Result.class.getDeclaredConstructor(String.class,double.class);ctor.setAccessible(true);
  Map<String,Split.Result> results=(Map<String,Split.Result>)field("splits").get(null);
  results.put("gustamucho",ctor.newInstance("gusta mucho",-10.0));
  Split.Result kept=Split.update("fixture context ","","gustamucho",System.nanoTime()-1);
  if(kept==null||!"gusta mucho".equals(kept.text))throw new AssertionError("An expired optional budget erased a valid separation from the same context");
  Split.Result changed=Split.update("another context ","","gustamucho",System.nanoTime()-1);
  if(changed!=null)throw new AssertionError("A separation from an old context was reused");
  System.out.println("PASS optional deadline preserves a valid split and a new context invalidates it");
 }
}
