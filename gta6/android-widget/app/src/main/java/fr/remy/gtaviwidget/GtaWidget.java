package fr.remy.gtaviwidget;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
public final class GtaWidget extends AppWidgetProvider {
 static final String REFRESH="fr.remy.gtaviwidget.REFRESH";
 static PendingIntent alarmIntent(Context c){return PendingIntent.getBroadcast(c,1,new Intent(c,GtaWidget.class).setAction(REFRESH),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 @Override public void onReceive(Context c,Intent i){
  super.onReceive(c,i);
  if(!AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(i.getAction()))updateAll(c);
 }
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){updateAll(c);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle opts){updateAll(c);}
 @Override public void onDisabled(Context c){c.getSystemService(AlarmManager.class).cancel(alarmIntent(c));}
 public static void updateAll(Context c){
  AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new ComponentName(c,GtaWidget.class));
  Config conf=Config.load(c);long now=System.currentTimeMillis();
  for(int id:ids){Bundle opts=m.getAppWidgetOptions(id);
   int w=Math.max(180,opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,300));
   int h=Math.max(130,opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,225));
   m.updateAppWidget(id,views(c,conf,now,w,h));
  }
  if(ids.length>0)schedule(c,conf,now);
 }
 public static RemoteViews views(Context c,Config conf,long now,int widthDp,int heightDp){
  int w=Math.min(900,widthDp*2),h=(int)(w*(heightDp/(float)widthDp));
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
  v.setImageViewBitmap(R.id.art,render(c,conf,now,w,h));
  boolean active=now<conf.release;v.setViewVisibility(R.id.timer_panel,active?View.VISIBLE:View.GONE);
  long base=SystemClock.elapsedRealtime()+Countdown.withinDay(conf.release,now);
  v.setChronometerCountDown(R.id.chrono,true);v.setChronometer(R.id.chrono,base,"%s",active);
  // Chronometer is hosted by the launcher, so it ticks without waking our app each second.
  v.setTextViewTextSize(R.id.chrono,TypedValue.COMPLEX_UNIT_DIP,Math.max(20,heightDp*.11f));
  v.setTextViewTextSize(R.id.time_labels,TypedValue.COMPLEX_UNIT_DIP,Math.max(7,heightDp*.027f));
  v.setViewPadding(R.id.timer_panel,0,0,0,(int)(heightDp*.10f*c.getResources().getDisplayMetrics().density));
  Intent open=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(conf.pageUrl));
  v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
  return v;
 }
 public static Bitmap render(Context c,Config conf,long now,int w,int h){
  Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(out);Paint p=new Paint(3);
  cv.drawColor(Color.rgb(23,16,33));Bitmap image=Config.bitmap(c,"image"+Countdown.imageSlot(now)+".jpg");
  if(image!=null){float s=Math.max(w/(float)image.getWidth(),h/(float)image.getHeight());float dw=image.getWidth()*s,dh=image.getHeight()*s;cv.drawBitmap(image,null,new RectF((w-dw)/2,(h-dh)/2,(w+dw)/2,(h+dh)/2),p);}
  p.setShader(new LinearGradient(0,0,0,h,new int[]{0x330b0718,0x110b0718,0xdd0b0718},new float[]{0,.45f,1},Shader.TileMode.CLAMP));cv.drawRect(0,0,w,h,p);p.setShader(null);
  Bitmap logo=Config.bitmap(c,"logo.png");
  if(logo!=null){float lw=w*.20f,lh=lw*logo.getHeight()/logo.getWidth();cv.drawBitmap(logo,null,new RectF(w*.04f,h*.05f,w*.04f+lw,h*.05f+lh),p);}
  p.setColor(Color.WHITE);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setShadowLayer(5,0,2,0x66000000);
  p.setTextAlign(Paint.Align.RIGHT);p.setTextSize(h*.025f);
  cv.drawText("19 NOVEMBRE 2026",w*.95f,h*.075f,p);cv.drawText("00 H · FRANCE",w*.95f,h*.115f,p);
  p.setTextAlign(Paint.Align.CENTER);
  if(now>=conf.release){p.setTextSize(w*.08f);cv.drawText("BIENVENUE À VICE CITY",w/2f,h*.55f,p);}
  else{
   String days=String.valueOf(Countdown.days(conf.release,now));p.setTypeface(Typeface.create("sans-serif-black",Typeface.NORMAL));
   p.setTextSize(Math.min(w*.56f,h*.51f));Rect b=new Rect();p.getTextBounds(days,0,days.length(),b);
   cv.drawText(days,w/2f,h*.43f-(b.top+b.bottom)/2f,p);
   p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(h*.033f);cv.drawText(Countdown.days(conf.release,now)==1?"J O U R":"J O U R S",w/2f,h*.685f,p);
  }
  p.clearShadowLayer();p.setColor(0xbbffffff);p.setTextSize(h*.019f);p.setTypeface(Typeface.DEFAULT);cv.drawText("Visuels © Rockstar Games · Fan widget",w/2f,h*.965f,p);
  return out;
 }
 static void schedule(Context c,Config conf,long now){
  AlarmManager a=c.getSystemService(AlarmManager.class);
  long next=Countdown.nextImage(now);
  if(now<conf.release)next=Math.min(next,Countdown.nextDay(conf.release,now));
  next=Math.max(now+1000,next);
  PendingIntent pi=alarmIntent(c);
  if(Build.VERSION.SDK_INT<31||a.canScheduleExactAlarms())a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,pi);
  else a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,pi);
 }
}