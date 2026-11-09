package fr.remy.gtaviwidget;
import android.app.job.*;import android.appwidget.*;import android.content.*;import java.util.concurrent.atomic.AtomicBoolean;
public final class GtaSync extends JobService {
 static final int PERIODIC=120,ONCE=121;
 private final AtomicBoolean stopped=new AtomicBoolean(false);
 public static void schedule(Context c){
  JobScheduler jobs=c.getSystemService(JobScheduler.class);
  if(jobs.getPendingJob(PERIODIC)==null)jobs.schedule(new JobInfo.Builder(PERIODIC,new ComponentName(c,GtaSync.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(6*60*60*1000L).setPersisted(true).build());
 }
 public static void enqueue(Context c){
  if(AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,GtaWidget.class)).length==0)return;
  c.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(ONCE,new ComponentName(c,GtaSync.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(1000).build());
 }
 @Override public boolean onStartJob(JobParameters p){
  stopped.set(false);
  new Thread(()->{boolean retry=false;try{Config.sync(this);GtaWidget.updateAll(this);}catch(Exception error){retry=true;}finally{if(!stopped.get())jobFinished(p,retry);}},"GTA-artwork-sync").start();
  return true;
 }
 @Override public boolean onStopJob(JobParameters p){stopped.set(true);return true;}
}