package gbport.test;
import gbport.*;
import java.lang.reflect.*;
import java.util.*;
public final class SplitTypoRegression {
 static List<Delight.Cand> fixture(String a,float sa,String b,float sb){
  Pb cs=Pb.n().m(1,Pb.n().i(1,1).s(2,a).f(4,sa)).m(1,Pb.n().i(1,0).s(2,b).f(4,sb));
  return Delight.parseCands(Pb.n().m(2,Pb.n().m(3,cs)).bytes());
 }
 public static void main(String[] args)throws Exception{
  Method method;
  try{method=Split.class.getDeclaredMethod("rightForm",List.class,String.class);}
  catch(NoSuchMethodException e){throw new AssertionError("The right-hand word of a split cannot recover a single-key typo",e);}
  method.setAccessible(true);
  Object[] corrected=(Object[])method.invoke(null,fixture("xomo",-30,"cómo",-4),"xomo");
  if(corrected==null||!"cómo".equals(corrected[0]))throw new AssertionError("xomo does not recover cómo after a valid left word");
  Object[] kept=(Object[])method.invoke(null,fixture("casa",-4,"cosa",-2),"casa");
  if(kept==null||!"casa".equals(kept[0]))throw new AssertionError("A valid right word was changed to another word");
  Object[] unrelated=(Object[])method.invoke(null,fixture("zorvian",-30,"Zorina",-4),"zorvian");
  if(unrelated!=null&&(Double)unrelated[1]>=-15)throw new AssertionError("An unrelated name is admitted as a valid split part");
  Method prefix=Split.class.getDeclaredMethod("bestForm",List.class,String.class);prefix.setAccessible(true);
  Object[] capital=(Object[])prefix.invoke(null,fixture("hola",Float.NaN,"Hola",-5),"hola");
  if(capital==null||!"hola".equals(capital[0]))throw new AssertionError("A capitalized variant loses a known left word boundary");
  Constructor<Split.Result> constructor=Split.Result.class.getDeclaredConstructor(String.class,double.class);constructor.setAccessible(true);
  Split.Result valid=constructor.newInstance("hola cómo",-21.2);
  if(!Split.wins(valid,Double.NaN,Double.NaN))throw new AssertionError("Missing literal score rejects a pair of individually validated words");
  Split.Result weak=constructor.newInstance("ho la xomo",-28.0);
  if(Split.wins(weak,Double.NaN,Double.NaN))throw new AssertionError("A weak fragmented sequence beats an unknown literal");
  System.out.println("PASS split right-half typos corrected conservatively and valid words preserved");
 }
}
