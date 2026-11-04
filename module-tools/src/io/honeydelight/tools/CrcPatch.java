package io.honeydelight.tools;
import java.nio.file.*;import java.util.zip.*;
/** Small local installer tool; contains no Samsung or Gboard implementation. */
public final class CrcPatch {
 public static void main(String[] args)throws Exception{
  if(args.length!=4)throw new IllegalArgumentException("Usage: original.apk modified.zip artifact output");
  byte[] bytes=Files.readAllBytes(Paths.get(args[2]));String[] names={"classes.dex","classes2.dex","classes3.dex"};
  int[] positions={-1,-1,-1};long[] targets=new long[3];
  try(ZipFile stock=new ZipFile(args[0]);ZipFile modified=new ZipFile(args[1])){
   for(int k=0;k<names.length;k++){
    ZipEntry a=stock.getEntry(names[k]),b=modified.getEntry(names[k]);if(a==null||b==null)throw new IllegalArgumentException("Missing DEX entry");
    long source=b.getCrc();targets[k]=a.getCrc();if(source==targets[k])continue;int count=0;
    for(int i=0;i<=bytes.length-4;i++)if((bytes[i]&255)==(int)(source&255)&&(bytes[i+1]&255)==(int)((source>>>8)&255)&&(bytes[i+2]&255)==(int)((source>>>16)&255)&&(bytes[i+3]&255)==(int)((source>>>24)&255)){positions[k]=i;count++;}
    if(count!=1)throw new IllegalStateException("Unexpected CRC count for "+names[k]+": "+count);
   }
  }
  for(int k=0;k<3;k++)if(positions[k]>=0)for(int i=0;i<4;i++)bytes[positions[k]+i]=(byte)(targets[k]>>>(8*i));
  Files.write(Paths.get(args[3]),bytes);
 }
}
