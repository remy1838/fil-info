package fr.remy.gtaviwidget;
import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.os.*;import android.net.Uri;
public final class ReleaseReminder extends BroadcastReceiver {
 static final int NOTIFICATION_ID=1908;
 static final String CHANNEL="gta-countdown-end";
 static PendingIntent alarm(Context c){return PendingIntent.getBroadcast(c,1908,new Intent(c,ReleaseReminder.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 public static void schedule(Context c){
  Config conf=Config.load(c);long now=System.currentTimeMillis();if(now>=conf.release)return;
  AlarmManager a=c.getSystemService(AlarmManager.class);
  if(Build.VERSION.SDK_INT<31||a.canScheduleExactAlarms())a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,conf.release,alarm(c));
  else a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,conf.release,alarm(c));
 }
 static boolean post(Context c){
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
  NotificationManager manager=c.getSystemService(NotificationManager.class);
  manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Fin du compte à rebours GTA VI",NotificationManager.IMPORTANCE_HIGH));
  PendingIntent open=PendingIntent.getActivity(c,1908,new Intent(Intent.ACTION_VIEW,Uri.parse(Config.PAGE)),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.app_icon).setContentTitle("GTA VI · C’est l’heure !").setContentText("Ton compte à rebours est terminé : 19 novembre, 08 h.").setStyle(new Notification.BigTextStyle().bigText("Ton compte à rebours GTA VI est arrivé au 19 novembre 2026 à 08 h, heure française.")).setAutoCancel(true).setContentIntent(open).build();
  manager.notify(NOTIFICATION_ID,n);return true;
 }
 @Override public void onReceive(Context c,Intent intent){
  Config conf=Config.load(c);long now=System.currentTimeMillis();
  if(now<conf.release){schedule(c);return;}
  android.content.SharedPreferences prefs=c.getSharedPreferences("reminder",Context.MODE_PRIVATE);
  if(prefs.getLong("delivered",0)==conf.release||now-conf.release>86400000L)return;
  if(post(c))prefs.edit().putLong("delivered",conf.release).apply();
 }
}