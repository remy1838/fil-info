package fr.remy.gtaviwidget;
import android.app.*;import android.content.*;import android.content.res.AssetFileDescriptor;import android.media.*;import android.os.*;
public final class MusicService extends Service {
 public static final String STOP="fr.remy.gtaviwidget.STOP_MUSIC";
 public static volatile boolean playing=false;
 private MediaPlayer player;private AudioManager audio;private AudioFocusRequest focus;private boolean closing=false;
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&STOP.equals(i.getAction())){endPlayback();return START_NOT_STICKY;}
  if(player!=null)return START_NOT_STICKY;
  closing=false;startForeground(ReleaseReminder.NOTIFICATION_ID,ReleaseReminder.notification(this,true));
  audio=getSystemService(AudioManager.class);
  AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
  focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs).setOnAudioFocusChangeListener(change->{
   if(change==AudioManager.AUDIOFOCUS_LOSS||change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)endPlayback();
   else if(player!=null){try{player.setVolume(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK?.2f:1f,change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK?.2f:1f);}catch(Exception ignored){}}
  }).build();
  if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){endPlayback();return START_NOT_STICKY;}
  try(AssetFileDescriptor fd=getAssets().openFd("notification_music.mp3")){
   player=new MediaPlayer();player.setAudioAttributes(attrs);player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
   player.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
   player.setOnPreparedListener(p->{p.start();playing=true;});
   player.setOnCompletionListener(p->endPlayback());
   player.setOnErrorListener((p,what,extra)->{endPlayback();return true;});
   player.prepareAsync();
  }catch(Exception error){endPlayback();}
  return START_NOT_STICKY;
 }
 private void endPlayback(){
  if(closing)return;closing=true;releaseAudio();
  stopForeground(STOP_FOREGROUND_DETACH);
  if(ReleaseReminder.allowed(this))getSystemService(NotificationManager.class).notify(ReleaseReminder.NOTIFICATION_ID,ReleaseReminder.notification(this,false));
  stopSelf();
 }
 private void releaseAudio(){
  playing=false;if(player!=null){try{player.stop();}catch(Exception ignored){}player.release();player=null;}
  if(audio!=null&&focus!=null){audio.abandonAudioFocusRequest(focus);focus=null;}
 }
 @Override public void onDestroy(){releaseAudio();super.onDestroy();}
}