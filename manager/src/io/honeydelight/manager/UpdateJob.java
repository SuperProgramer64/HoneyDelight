package io.honeydelight.manager;
import android.app.job.*;import android.content.*;
public final class UpdateJob extends JobService {
 Thread task;
 public static void schedule(Context context,boolean enabled){JobScheduler scheduler=context.getSystemService(JobScheduler.class);if(!enabled){scheduler.cancel(8101);return;}scheduler.schedule(new JobInfo.Builder(8101,new ComponentName(context,UpdateJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED).setPersisted(true).setPeriodic(6L*60*60*1000).build());}
 @Override public boolean onStartJob(JobParameters params){
  if(!Updates.prefs(this).getBoolean("automatic",true))return false;
  task=new Thread(()->{try{Updates.Release release=Updates.latest(this);if(release!=null&&!(release.tag+release.name+release.digest).equals(Updates.prefs(this).getString("notified",null))){Updates.download(this,release);Updates.notifyReady(this,release.tag);Updates.prefs(this).edit().putString("notified",release.tag+release.name+release.digest).apply();}}catch(Exception ignored){}finally{jobFinished(params,false);}},"HoneyDelight-update");task.start();return true;
 }
 @Override public boolean onStopJob(JobParameters params){if(task!=null)task.interrupt();return true;}
}
