package fr.remy.gtaviwidget;
import android.app.*;import android.content.*;import android.os.*;import android.widget.*;import java.time.*;
public class TickInstrumentation extends Instrumentation {
 @Override public void onCreate(Bundle b){super.onCreate(b);start();}
 @Override public void onStart(){
  Bundle result=new Bundle();
  try{
   long release=OffsetDateTime.parse("2026-11-19T00:00:00+01:00").toInstant().toEpochMilli();
   if(Countdown.remaining(release,release+1)!=0)throw new AssertionError("Negative countdown");
   String[] times={"2026-10-30T07:59:59+01:00","2026-10-30T08:00:00+01:00","2026-10-30T16:00:00+01:00","2026-10-31T00:00:00+01:00"};
   int[] slots={0,1,2,0};for(int i=0;i<times.length;i++)if(Countdown.imageSlot(OffsetDateTime.parse(times[i]).toInstant().toEpochMilli())!=slots[i])throw new AssertionError("Image slot");
   MainActivity a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   waitForIdleSync();SystemClock.sleep(700);
   String[] texts=new String[2];
   runOnMainSync(()->texts[0]=((Chronometer)a.findViewById(R.id.chrono)).getText().toString());
   SystemClock.sleep(2400);
   runOnMainSync(()->texts[1]=((Chronometer)a.findViewById(R.id.chrono)).getText().toString());
   if(texts[0].equals(texts[1]))throw new AssertionError("Native countdown frozen");
   for(int i=0;i<3;i++)if(Config.bitmap(getTargetContext(),"image"+i+".jpg")==null)throw new AssertionError("Missing image");
   if(Config.bitmap(getTargetContext(),"logo.png")==null)throw new AssertionError("Missing logo");
   result.putString("stream","PASS: native RemoteViews countdown changed from "+texts[0]+" to "+texts[1]+"; Paris rotation and assets verified.\n");
   finish(Activity.RESULT_OK,result);
  }catch(Throwable e){result.putString("stream","FAIL: "+e.toString()+"\n");finish(Activity.RESULT_CANCELED,result);}
 }
}