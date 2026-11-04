package gbport.test;
import gbport.Bridge;
import java.lang.reflect.*;
import java.util.*;
public final class CommitRefreshRegression {
 static Field f(String n)throws Exception{Field f=Bridge.class.getDeclaredField(n);f.setAccessible(true);return f;}
 public static void main(String[] args)throws Exception{
  Method refresh;
  try{refresh=Bridge.class.getDeclaredMethod("needsCommitRefresh",String.class);}
  catch(NoSuchMethodException e){throw new AssertionError("Space trusts an incomplete cached decision for a missing-space word",e);}
  refresh.setAccessible(true);
  f("state").setInt(null,2);f("secret").setBoolean(null,false);
  Map<String,Double> scores=(Map<String,Double>)f("litSeen").get(null);
  scores.put("buenosdias",Double.NaN);f("decWord").set(null,"buenosdias");
  f("decResult").setInt(null,1);f("correctionText").set(null,"buenosdías");
  if(!(Boolean)refresh.invoke(null,"buenosdias"))throw new AssertionError("Accent-only result masks the untested word boundary");
  f("correctionText").set(null,"buenos días");
  if((Boolean)refresh.invoke(null,"buenosdias"))throw new AssertionError("A complete cached separation triggers redundant work");
  scores.put("erroes",-30.0);f("decWord").set(null,"erroes");f("decResult").setInt(null,0);f("correctionText").set(null,null);
  if(!(Boolean)refresh.invoke(null,"erroes"))throw new AssertionError("An uncorrected unknown word is never reconsidered on space");
  scores.put("hola",-3.0);f("decWord").set(null,"hola");
  if((Boolean)refresh.invoke(null,"hola"))throw new AssertionError("Every valid word incurs extra native work on space");
  f("secret").setBoolean(null,true);
  if((Boolean)refresh.invoke(null,"erroes"))throw new AssertionError("Private fields enter final correction work");
  System.out.println("PASS incomplete commits refreshed, full splits reused, known/private words bypass extra work");
 }
}
