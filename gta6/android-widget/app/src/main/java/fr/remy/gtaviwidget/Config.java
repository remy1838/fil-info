package fr.remy.gtaviwidget;
import android.content.Context;
import android.graphics.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
public final class Config {
 public static final String PAGE="https://remy1838.github.io/fil-info/gta6/";
 public long release;
 public String pageUrl,logo;
 public String[] images;
 public Config(String json)throws Exception{
  JSONObject o=new JSONObject(json);release=OffsetDateTime.parse(o.getString("release")).toInstant().toEpochMilli();
  pageUrl=o.getString("pageUrl");logo=o.getString("logo");
  if(!PAGE.equals(pageUrl))throw new IllegalArgumentException("Lien non pris en charge");
  JSONArray a=o.getJSONArray("images");if(a.length()!=8)throw new IllegalArgumentException("Huit images requises");
  images=new String[a.length()];
  for(int i=0;i<images.length;i++)images[i]=a.getString(i);
 }
 public static Config load(Context c){
  try{
   File f=new File(c.getFilesDir(),"widget.json");
   String json=f.exists()?new String(java.nio.file.Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8):readAsset(c,"widget.json");
   return new Config(json);
  }catch(Exception e){try{return new Config(readAsset(c,"widget.json"));}catch(Exception fatal){throw new IllegalStateException(fatal);}}
 }
 static String readAsset(Context c,String n)throws Exception{try(InputStream in=c.getAssets().open(n)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int count;while((count=in.read(b))!=-1)out.write(b,0,count);return out.toString("UTF-8");}}
 static byte[] fetch(String url)throws Exception{
  if(!url.startsWith("https://remy1838.github.io/fil-info/gta6/")&&!url.startsWith("https://www.rockstargames.com/VI/"))throw new SecurityException("Source non autorisée");
  HttpURLConnection cn=(HttpURLConnection)new URL(url).openConnection();cn.setConnectTimeout(7000);cn.setReadTimeout(10000);cn.setRequestProperty("User-Agent","GTA-VI-Widget/1.0");
  try(InputStream in=cn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){if(out.size()+n>6000000)throw new IOException("Fichier trop grand");out.write(b,0,n);}return out.toByteArray();
  }finally{cn.disconnect();}
 }
 public static String configurationFromPage(String html)throws Exception{
  java.util.regex.Matcher date=java.util.regex.Pattern.compile("const release=Date\\.parse\\('([^']+)'\\)").matcher(html);
  java.util.regex.Matcher media=java.util.regex.Pattern.compile("const media='([^']+)'").matcher(html);
  java.util.regex.Matcher artworks=java.util.regex.Pattern.compile("const artworks=\\[([\\s\\S]*?)\\]").matcher(html);
  java.util.regex.Matcher logoMatch=java.util.regex.Pattern.compile("<img class=\"logo\"[^>]+src=\"([^\"]+)\"").matcher(html);
  if(!date.find()||!media.find()||!artworks.find()||!logoMatch.find())throw new IOException("Format de page modifié");
  java.util.regex.Matcher names=java.util.regex.Pattern.compile("'([^']+)'").matcher(artworks.group(1));
  JSONArray urls=new JSONArray();while(names.find())urls.put(media.group(1)+names.group(1)+"?akim=1&imdensity=1&imwidth=1280");
  java.util.regex.Matcher poolMatch=java.util.regex.Pattern.compile("const latestArtworks=(\\[[\\s\\S]*?\\]);").matcher(html);
  java.util.regex.Matcher anchorMatch=java.util.regex.Pattern.compile("const artworkAnchorDay=(\\d+);").matcher(html);
  if(poolMatch.find()&&anchorMatch.find()){
   JSONArray pool=new JSONArray(poolMatch.group(1));
   if(pool.length()>=8){
    long today=java.time.LocalDate.now(java.time.ZoneId.of("Europe/Paris")).toEpochDay();
    long anchor=Long.parseLong(anchorMatch.group(1));
    int start=(int)Math.floorMod((today-anchor)*8L,(long)pool.length());
    urls=new JSONArray();
    for(int i=0;i<8;i++)urls.put(media.group(1)+pool.getString((start+i)%pool.length())+"?akim=1&imdensity=1&imwidth=1280");
   }
  }
  JSONObject data=new JSONObject();data.put("pageUrl",PAGE);data.put("release",date.group(1));data.put("images",urls);data.put("logo",logoMatch.group(1).replace("&amp;","&"));
  return data.toString();
 }
 public static void sync(Context c)throws Exception{
  byte[] bytes=configurationFromPage(new String(fetch(PAGE),StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);Config next=new Config(new String(bytes,StandardCharsets.UTF_8));
  // Download and validate everything before publishing a new set.
  byte[][] all=new byte[next.images.length+1][];
  for(int i=0;i<next.images.length;i++)all[i]=fetch(next.images[i]);all[next.images.length]=fetch(next.logo);
  for(byte[] b:all)if(BitmapFactory.decodeByteArray(b,0,b.length)==null)throw new IOException("Image invalide");
  for(int i=0;i<all.length;i++)java.nio.file.Files.write(new File(c.getFilesDir(),i==next.images.length?"logo.png":"image"+i+".jpg").toPath(),all[i]);
  java.nio.file.Files.write(new File(c.getFilesDir(),"widget.json").toPath(),bytes);
 }
 public static Bitmap bitmap(Context c,String name){
  File f=new File(c.getFilesDir(),name);
  if(f.exists()){Bitmap b=BitmapFactory.decodeFile(f.toString());if(b!=null)return b;}
  try(InputStream in=c.getAssets().open(name)){return BitmapFactory.decodeStream(in);}catch(Exception e){return null;}
 }
}