package fr.remy.gtaviwidget;
import android.app.*;import android.content.*;import android.os.*;import android.widget.*;import java.time.*;
public class TickInstrumentation extends Instrumentation {
 @Override public void onCreate(Bundle b){super.onCreate(b);start();}
 @Override public void onStart(){
  Bundle result=new Bundle();
  try{
   long release=OffsetDateTime.parse("2026-11-19T08:00:00+01:00").toInstant().toEpochMilli();
   if(Countdown.remaining(release,release+1)!=0)throw new AssertionError("Negative countdown");
   String[] times={"2026-10-30T07:59:59+01:00","2026-10-30T08:00:00+01:00","2026-10-30T16:00:00+01:00","2026-10-31T00:00:00+01:00"};
   int[] slots={7,8,16,0};for(int i=0;i<times.length;i++)if(Countdown.imageSlot(OffsetDateTime.parse(times[i]).toInstant().toEpochMilli())!=slots[i])throw new AssertionError("Image slot");
   String source;
   try(java.io.InputStream in=getContext().getAssets().open("source-page.html")){source=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
   Config parsed=new Config(Config.configurationFromPage(source));
   if(parsed.images.length!=24||!parsed.images[0].startsWith("https://www.rockstargames.com/VI/"))throw new AssertionError("Official gallery parse failed");
   GtaSync.schedule(getTargetContext());
   if(getTargetContext().getSystemService(android.app.job.JobScheduler.class).getPendingJob(GtaSync.PERIODIC)==null)throw new AssertionError("Background update job missing");
   for(int hour=0;hour<24;hour++){long t=OffsetDateTime.parse("2026-11-09T"+String.format(java.util.Locale.ROOT,"%02d",hour)+":00:00+01:00").toInstant().toEpochMilli();if(Countdown.imageSlot(t)!=hour)throw new AssertionError("Twenty-four hourly slots");}
   if(parsed.release!=release)throw new AssertionError("Release must be 19 November 08:00 Paris");
   ReleaseReminder.schedule(getTargetContext());
   if(!ReleaseReminder.post(getTargetContext()))throw new AssertionError("Notification permission missing in test");
   boolean notificationFound=false;for(int attempt=0;attempt<20&&!notificationFound;attempt++){for(android.service.notification.StatusBarNotification n:getTargetContext().getSystemService(android.app.NotificationManager.class).getActiveNotifications())if(n.getId()==ReleaseReminder.NOTIFICATION_ID)notificationFound=true;if(!notificationFound)SystemClock.sleep(200);}
   if(!notificationFound)throw new AssertionError("Notification not posted");
   getTargetContext().getSystemService(android.app.NotificationManager.class).cancel(ReleaseReminder.NOTIFICATION_ID);
   MainActivity a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   waitForIdleSync();SystemClock.sleep(700);
   runOnMainSync(()->a.startForegroundService(new Intent(a,MusicService.class)));
   for(int wait=0;wait<40&&!MusicService.playing;wait++)SystemClock.sleep(100);
   if(!MusicService.playing)throw new AssertionError("Automatic music playback did not start");
   runOnMainSync(()->a.startService(new Intent(a,MusicService.class).setAction(MusicService.STOP)));
   for(int wait=0;wait<20&&MusicService.playing;wait++)SystemClock.sleep(100);
   if(MusicService.playing)throw new AssertionError("Stop music action failed");
   String[] texts=new String[2];
   runOnMainSync(()->texts[0]=((Chronometer)a.findViewById(R.id.chrono)).getText().toString());
   SystemClock.sleep(2400);
   runOnMainSync(()->texts[1]=((Chronometer)a.findViewById(R.id.chrono)).getText().toString());
   if(texts[0].equals(texts[1]))throw new AssertionError("Native countdown frozen");
   for(int i=0;i<24;i++)if(Config.bitmap(getTargetContext(),"image"+i+".jpg")==null)throw new AssertionError("Missing image");
   if(Config.bitmap(getTargetContext(),"logo.png")==null)throw new AssertionError("Missing logo");
   result.putString("stream","PASS: native RemoteViews countdown changed from "+texts[0]+" to "+texts[1]+"; 08:00 Paris target, notification image, automatic music playback and stop control, 24 hourly slots, gallery and background job verified.\n");
   finish(Activity.RESULT_OK,result);
  }catch(Throwable e){result.putString("stream","FAIL: "+e.toString()+"\n");finish(Activity.RESULT_CANCELED,result);}
 }
}