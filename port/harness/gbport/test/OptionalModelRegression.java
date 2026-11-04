package gbport.test;
import gbport.Delight;
import java.nio.file.*;
public final class OptionalModelRegression {
 public static void main(String[] args) throws Exception {
  Path bad=Files.createTempFile("honeydelight-broken-emoji", ".shortcuts");
  try {
   Files.write(bad,new byte[]{7});
   int loaded=Delight.loadEmojiShortcuts(new String[][]{{bad.toString(),"es"}});
   if(loaded!=0)throw new AssertionError("Malformed optional shortcuts must be skipped");
  } finally {Files.deleteIfExists(bad);}
  System.out.println("PASS malformed optional shortcuts do not abort base-model startup");
 }
}
