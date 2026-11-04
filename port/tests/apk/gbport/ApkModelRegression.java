package gbport;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
public final class ApkModelRegression {
 static String digest(byte[] bytes)throws Exception{
  StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format("%02x",b&255));return s.toString();
 }
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("honeydelight-apk-models");
  byte[] data="base model fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8);
  File file=dir.resolve("es_us.dict").toFile();
  try{
   ApkModels.installIfMissing(file,()->new ByteArrayInputStream(data),digest(data));
   if(!java.util.Arrays.equals(data,Files.readAllBytes(file.toPath())))throw new AssertionError("Packaged model not installed");
   ApkModels.installIfMissing(file,()->{throw new AssertionError("Existing models must be preserved without reopening assets");},digest(data));
   File second=dir.resolve("en_us.dict").toFile();
   try{ApkModels.installIfMissing(second,()->new ByteArrayInputStream(data),"0".repeat(64));throw new AssertionError("Corrupt asset accepted");}
   catch(IOException expected){}
   if(second.exists()||dir.resolve("en_us.dict.part").toFile().exists())throw new AssertionError("Failed model remains installable");
   System.out.println("PASS packaged base models installed atomically, existing files preserved and corrupt assets rejected");
  }finally{for(File entry:dir.toFile().listFiles())entry.delete();Files.delete(dir);}
 }
}
