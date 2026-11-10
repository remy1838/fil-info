package fr.remy.gtaviwidget;
import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.os.*;import android.net.Uri;import android.graphics.*;
public final class ReleaseReminder extends BroadcastReceiver {
 static final int NOTIFICATION_ID=1908;
 static final String CHANNEL="gta-countdown-music-v2";
 static PendingIntent alarm(Context c){return PendingIntent.getBroadcast(c,1908,new Intent(c,ReleaseReminder.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 public static void schedule(Context c){
  Config conf=Config.load(c);long now=System.currentTimeMillis();if(now>=conf.release)return;
  AlarmManager a=c.getSystemService(AlarmManager.class);
  if(Build.VERSION.SDK_INT<31||a.canScheduleExactAlarms())a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,conf.release,alarm(c));
  else a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,conf.release,alarm(c));
 }
 static Notification notification(Context c,boolean playing){
  NotificationManager manager=c.getSystemService(NotificationManager.class);
  NotificationChannel channel=new NotificationChannel(CHANNEL,"GTA VI · Image et musique",NotificationManager.IMPORTANCE_HIGH);
  channel.setSound(null,null);channel.enableVibration(true);manager.createNotificationChannel(channel);
  PendingIntent open=PendingIntent.getActivity(c,1908,new Intent(Intent.ACTION_VIEW,Uri.parse(Config.PAGE)),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.app_icon).setContentTitle("GTA VI · C’est l’heure !").setContentText(playing?"Flower In A Hurricane · Justice":"Ton compte à rebours est terminé : 19 novembre, 08 h.").setAutoCancel(!playing).setOngoing(playing).setOnlyAlertOnce(true).setContentIntent(open);
  try(java.io.InputStream in=c.getAssets().open("notification_cover.jpg")){
   Bitmap image=BitmapFactory.decodeStream(in);
   if(image!=null)b.setLargeIcon(image).setStyle(new Notification.BigPictureStyle().bigPicture(image).bigLargeIcon((Bitmap)null).setSummaryText("19 novembre 2026 · 08 h, heure française"));
  }catch(Exception ignored){b.setStyle(new Notification.BigTextStyle().bigText("19 novembre 2026 à 08 h, heure française."));}
  if(playing){
   PendingIntent stop=PendingIntent.getService(c,1909,new Intent(c,MusicService.class).setAction(MusicService.STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   b.addAction(new Notification.Action.Builder((android.graphics.drawable.Icon)null,"Arrêter la musique",stop).build());
  }
  return b.build();
 }
 static boolean allowed(Context c){return Build.VERSION.SDK_INT<33||c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;}
 static boolean post(Context c){if(!allowed(c))return false;c.getSystemService(NotificationManager.class).notify(NOTIFICATION_ID,notification(c,false));return true;}
 @Override public void onReceive(Context c,Intent intent){
  Config conf=Config.load(c);long now=System.currentTimeMillis();
  if(now<conf.release){schedule(c);return;}
  android.content.SharedPreferences prefs=c.getSharedPreferences("reminder",Context.MODE_PRIVATE);
  if(prefs.getLong("delivered",0)==conf.release||now-conf.release>86400000L||!allowed(c))return;
  try{c.startForegroundService(new Intent(c,MusicService.class));prefs.edit().putLong("delivered",conf.release).apply();}
  catch(Exception blocked){if(post(c))prefs.edit().putLong("delivered",conf.release).apply();}
 }
}