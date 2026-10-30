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
 public String[] images=new String[3];
 public Config(String json)throws Exception{
  JSONObject o=new JSONObject(json);release=OffsetDateTime.parse(o.getString("release")).toInstant().toEpochMilli();
  pageUrl=o.getString("pageUrl");logo=o.getString("logo");
  if(!PAGE.equals(pageUrl))throw new IllegalArgumentException("Lien non pris en charge");
  JSONArray a=o.getJSONArray("images");if(a.length()!=3)throw new IllegalArgumentException("Trois images requises");
  for(int i=0;i<3;i++)images[i]=a.getString(i);
 }
 public static Config load(Context c){
  try{
   File f=new File(c.getFilesDir(),"widget.json");
   String json=f.exists()?new String(java.nio.file.Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8):readAsset(c,"widget.json");
   return new Config(json);
  }catch(Exception e){try{return new Config(readAsset(c,"widget.json"));}catch(Exception fatal){throw new IllegalStateException(fatal);}}
 }
 static String readAsset(Context c,String n)throws Exception{try(InputStream in=c.getAssets().open(n)){return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
 static byte[] fetch(String url)throws Exception{
  if(!url.startsWith("https://remy1838.github.io/fil-info/gta6/")&&!url.startsWith("https://www.rockstargames.com/VI/"))throw new SecurityException("Source non autorisée");
  HttpURLConnection cn=(HttpURLConnection)new URL(url).openConnection();cn.setConnectTimeout(7000);cn.setReadTimeout(10000);cn.setRequestProperty("User-Agent","GTA-VI-Widget/1.0");
  try(InputStream in=cn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){if(out.size()+n>6000000)throw new IOException("Fichier trop grand");out.write(b,0,n);}return out.toByteArray();
  }finally{cn.disconnect();}
 }
 public static void sync(Context c)throws Exception{
  byte[] bytes=fetch(PAGE+"widget.json");Config next=new Config(new String(bytes,StandardCharsets.UTF_8));
  // Download and validate everything before publishing a new set.
  byte[][] all=new byte[4][];
  for(int i=0;i<3;i++)all[i]=fetch(next.images[i]);all[3]=fetch(next.logo);
  for(byte[] b:all)if(BitmapFactory.decodeByteArray(b,0,b.length)==null)throw new IOException("Image invalide");
  for(int i=0;i<4;i++)java.nio.file.Files.write(new File(c.getFilesDir(),i==3?"logo.png":"image"+i+".jpg").toPath(),all[i]);
  java.nio.file.Files.write(new File(c.getFilesDir(),"widget.json").toPath(),bytes);
 }
 public static Bitmap bitmap(Context c,String name){
  File f=new File(c.getFilesDir(),name);
  if(f.exists()){Bitmap b=BitmapFactory.decodeFile(f.toString());if(b!=null)return b;}
  try(InputStream in=c.getAssets().open(name)){return BitmapFactory.decodeStream(in);}catch(Exception e){return null;}
 }
}