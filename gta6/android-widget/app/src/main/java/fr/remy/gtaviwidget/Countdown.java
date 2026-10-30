package fr.remy.gtaviwidget;
import java.time.*;
public final class Countdown {
 public static final long DAY=86400000L;
 public static long remaining(long release,long now){return Math.max(0,release-now);}
 // ceil days aligns with the native Chronometer's ceiling-to-next-second countdown.
 public static long days(long release,long now){return (remaining(release,now)+999)/1000/86400;}
 public static long withinDay(long release,long now){return remaining(release,now)-days(release,now)*DAY;}
 public static int imageSlot(long now){return Instant.ofEpochMilli(now).atZone(ZoneId.of("Europe/Paris")).getHour()/8;}
 public static long nextImage(long now){
  ZonedDateTime t=Instant.ofEpochMilli(now).atZone(ZoneId.of("Europe/Paris"));
  return t.toLocalDate().atStartOfDay(t.getZone()).plusHours((t.getHour()/8+1)*8).toInstant().toEpochMilli();
 }
 public static long nextDay(long release,long now){
  long d=days(release,now);
  return d>0?release-d*DAY+1000:release;
 }
}