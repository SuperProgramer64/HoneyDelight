package gbport.test;
import java.lang.reflect.*;
public final class LocalEditorRegression {
    public static void main(String[] args) throws Exception {
        Class<?> type;
        try { type=Class.forName("gbport.LocalEditorText"); }
        catch (ClassNotFoundException e) { throw new AssertionError("Native builder still needs a read on the blocked UI thread",e); }
        Method capture=type.getDeclaredMethod("capture",CharSequence.class,int.class,int.class);
        Method read=type.getDeclaredMethod("read",boolean.class,int.class,int.class);
        capture.setAccessible(true); read.setAccessible(true);
        StringBuilder mutable=new StringBuilder("hola mundo");
        Object text=capture.invoke(null,mutable,5,5); mutable.setLength(0);
        if (!"la ".contentEquals((CharSequence)read.invoke(text,true,3,0))) throw new AssertionError("Wrong preceding text");
        if (!"mun".contentEquals((CharSequence)read.invoke(text,false,3,0))) throw new AssertionError("Wrong following text");
        if (!"hola ".contentEquals((CharSequence)read.invoke(text,true,100,0))) throw new AssertionError("Large read lost data");
        if (!"".contentEquals((CharSequence)read.invoke(text,true,0,0))) throw new AssertionError("Zero read not empty");
        if (read.invoke(text,true,-1,0)!=null) throw new AssertionError("Invalid native request intercepted");
        Object selected=capture.invoke(null,"hola mundo",4,0);
        if (!" mundo".contentEquals((CharSequence)read.invoke(selected,false,100,0))) throw new AssertionError("Selection leaked into after text");
        System.out.println("PASS native local editor reads, immutable text, bounds, reversed selection");
    }
}
