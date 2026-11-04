package io.honeydelight.manager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
public final class UpdatePolicy {
 public static String compatibilityId(String fingerprint,String apkSha)throws Exception{
  byte[] digest=MessageDigest.getInstance("SHA-256").digest((fingerprint+"\n"+apkSha).getBytes(StandardCharsets.UTF_8));
  StringBuilder value=new StringBuilder();for(byte b:digest)value.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return value.substring(0,12);
 }
 public static boolean compatible(String name,String id){
  return id!=null && id.matches("[a-f0-9]{12}") && name!=null && name.matches("[A-Za-z0-9_.-]+") && name.startsWith("HoneyDelight-") && name.endsWith("-"+id+".zip");
 }
 public static boolean validDigest(String digest){return digest!=null&&digest.matches("sha256:[a-fA-F0-9]{64}");}
 public static boolean trustedUrl(String value){
  try{java.net.URI u=new java.net.URI(value);String h=u.getHost();return "https".equals(u.getScheme()) && u.getUserInfo()==null && (u.getPort()==-1||u.getPort()==443) && ("api.github.com".equals(h)||"release-assets.githubusercontent.com".equals(h)||("github.com".equals(h)&&u.getPath().startsWith("/SuperProgramer64/HoneyDelight/releases/download/")));}catch(Exception e){return false;}
 }
}
