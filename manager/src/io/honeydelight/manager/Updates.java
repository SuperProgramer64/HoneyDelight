package io.honeydelight.manager;

import android.app.*;import android.content.*;import android.database.Cursor;import android.net.*;import android.os.*;import android.provider.MediaStore;
import org.json.*;import java.io.*;import java.net.*;import java.security.*;

final class Updates {
 static final String API="https://api.github.com/repos/SuperProgramer64/HoneyDelight/releases?per_page=10";
 static final String PAGE="https://github.com/SuperProgramer64/HoneyDelight/releases";
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("updates",Context.MODE_PRIVATE);}
 static String hash(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}StringBuilder out=new StringBuilder();for(byte b:d.digest())out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();}
 static HttpURLConnection connection(String url)throws Exception{
  for(int redirects=0;redirects<6;redirects++){
   if(!UpdatePolicy.trustedUrl(url))throw new IOException("El enlace no pertenece a las publicaciones de HoneyDelight");
   HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","HoneyDelight-Updater/0.1");
   int code=c.getResponseCode();if(code>=300&&code<400){String next=c.getHeaderField("Location");c.disconnect();if(next==null)throw new IOException("Enlace de descarga incompleto");url=new URL(new URL(url),next).toString();continue;}return c;
  }throw new IOException("Demasiadas redirecciones");
 }
 static final class Release {final String tag,name,url,digest;Release(String t,String n,String u,String d){tag=t;name=n;url=u;digest=d;}}
 static Release latest(Context context)throws Exception{
  HttpURLConnection c=connection(API);
  if(c.getResponseCode()==404){c.disconnect();return null;}
  if(c.getResponseCode()!=200){int code=c.getResponseCode();c.disconnect();throw new IOException(code==403?"GitHub limitó temporalmente las consultas. Inténtalo más tarde.":"No se pudo consultar GitHub ("+code+")");}
  JSONArray releases;try(InputStream in=c.getInputStream()){releases=new JSONArray(new String(in.readNBytes(1024*1024),java.nio.charset.StandardCharsets.UTF_8));}finally{c.disconnect();}
  if(releases.length()==0)return null;
  String apk=context.getPackageManager().getApplicationInfo("com.samsung.android.honeyboard",0).sourceDir;
  String id=UpdatePolicy.compatibilityId(Build.FINGERPRINT,hash(new File(apk)));
  for(int r=0;r<releases.length();r++){JSONObject json=releases.getJSONObject(r);if(json.optBoolean("draft",false))continue;JSONArray assets=json.getJSONArray("assets");
   for(int i=0;i<assets.length();i++){JSONObject a=assets.getJSONObject(i);String name=a.getString("name");if(UpdatePolicy.compatible(name,id))return new Release(json.getString("tag_name"),name,a.getString("browser_download_url"),a.optString("digest",null));}
  }
  throw new IOException("Hay una publicación, pero todavía no incluye un módulo para tu compilación exacta.");
 }
 static boolean exists(Context c,String value){if(value==null)return false;try(android.os.ParcelFileDescriptor fd=c.getContentResolver().openFileDescriptor(Uri.parse(value),"r")){return fd!=null;}catch(Exception e){return false;}}
 static synchronized Uri download(Context context,Release release)throws Exception{
  if(!UpdatePolicy.validDigest(release.digest))throw new IOException("La publicación no ofrece un SHA-256 verificable. No se descargó el módulo.");
  String oldTag=prefs(context).getString("tag",null),oldUri=prefs(context).getString("uri",null);
  if(release.tag.equals(oldTag)&&release.name.equals(prefs(context).getString("asset",null))&&release.digest.equals(prefs(context).getString("digest",null))&&exists(context,oldUri))return Uri.parse(oldUri);
  File temp=new File(context.getCacheDir(),"module-download.part");
  HttpURLConnection c=connection(release.url);
  if(c.getResponseCode()!=200){c.disconnect();throw new IOException("No se pudo descargar el módulo");}
  try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(temp)){
   byte[] b=new byte[65536];int n;long total=0;
   while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new IOException("Descarga detenida");total+=n;if(total>256L*1024*1024)throw new IOException("El módulo supera el límite de descarga");out.write(b,0,n);}
  }catch(Exception e){temp.delete();throw e;}finally{c.disconnect();}
  if(!hash(temp).equalsIgnoreCase(release.digest.substring(7))){temp.delete();throw new IOException("El archivo no coincide con su SHA-256. Se descartó la descarga.");}
  ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,release.name);values.put(MediaStore.Downloads.MIME_TYPE,"application/zip");values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/HoneyDelight");values.put(MediaStore.Downloads.IS_PENDING,1);
  Uri uri=context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(uri==null){temp.delete();throw new IOException("No se pudo guardar en Descargas");}
  try(InputStream in=new FileInputStream(temp);OutputStream out=context.getContentResolver().openOutputStream(uri)){
   if(out==null)throw new IOException("No se pudo guardar el módulo");in.transferTo(out);
  }catch(Exception e){context.getContentResolver().delete(uri,null,null);throw e;}finally{temp.delete();}
  values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);context.getContentResolver().update(uri,values,null,null);
  prefs(context).edit().putString("tag",release.tag).putString("uri",uri.toString()).putString("asset",release.name).putString("digest",release.digest).apply();return uri;
 }
 static void notifyReady(Context context,String tag){
  NotificationManager manager=context.getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("updates","Actualizaciones de HoneyDelight",NotificationManager.IMPORTANCE_DEFAULT));
  PendingIntent action=PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification n=new Notification.Builder(context,"updates").setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle("HoneyDelight "+tag+" disponible").setContentText("Módulo verificado en Descargas/HoneyDelight. Instálalo desde tu gestor root.").setAutoCancel(true).setContentIntent(action).build();
  try{manager.notify(1,n);}catch(SecurityException ignored){}
 }
}
