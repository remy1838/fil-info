package fr.remy.gtaviwidget;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
public class MainActivity extends Activity {
 LinearLayout root;FrameLayout preview;TextView state;
 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextColor(0xfff8eef9);t.setTextSize(size);t.setPadding(0,dp(7),0,dp(7));return t;}
 Button button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setOnClickListener(v->action.run());root.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;}
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);getWindow().setStatusBarColor(0xff171021);getWindow().setNavigationBarColor(0xff171021);
  ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xff171021);
  root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(18),dp(20),dp(20));scroll.addView(root);setContentView(scroll);
  scroll.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return i;});
  root.addView(text("GTA VI · Ton widget",26));
  root.addView(text("Secondes en direct · Une photo par heure · Fin le 19/11 à 08 h · Nouveautés automatiques",14));
  preview=new FrameLayout(this);int w=(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-40;
  root.addView(preview,new LinearLayout.LayoutParams(-1,dp(w*.75f)));
  root.addView(text("Ton lien reste le même :",13));
  TextView link=text(Config.PAGE,12);link.setTextIsSelectable(true);root.addView(link);
  state=text("",13);root.addView(state);
  button("1. Autoriser les changements aux bonnes heures",()->{
   if(Build.VERSION.SDK_INT>=31&&!getSystemService(AlarmManager.class).canScheduleExactAlarms())startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));
   else Toast.makeText(this,"Horaires précis déjà autorisés",Toast.LENGTH_SHORT).show();
  });
  button("2. Autoriser la notification du 19 novembre à 08 h",()->{
   if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},1908);
   else{ReleaseReminder.schedule(this);Toast.makeText(this,"Notification programmée pour le 19 novembre à 08 h",Toast.LENGTH_LONG).show();}
  });
  button("3. Ajouter le widget 4 × 3",()->{
   AppWidgetManager m=getSystemService(AppWidgetManager.class);
   if(m.isRequestPinAppWidgetSupported())m.requestPinAppWidget(new ComponentName(this,GtaWidget.class),null,null);
   else Toast.makeText(this,"Appui long sur l'accueil → Widgets → GTA VI Widget",Toast.LENGTH_LONG).show();
  });
  button("Synchroniser depuis mon lien",()->{
   state.setText("Synchronisation des images horaires…");
   new Thread(()->{try{Config.sync(this);runOnUiThread(()->{state.setText("Visuels et date synchronisés.");refresh();});}
   catch(Exception e){runOnUiThread(()->state.setText("Connexion indisponible. Les visuels déjà installés restent utilisables."));}}).start();
  });
  button("Ouvrir mon lien",()->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(Config.PAGE))));
  root.addView(text("Le widget récupère automatiquement les nouveautés de ton lien avec une connexion Internet. Les images changent chaque heure. Le décompte se termine le 19 novembre à 08 h, heure de Paris. Hors connexion, les dernières images restent disponibles.",12));
  root.addView(text("Si Samsung met l'application en veille : Paramètres → Applications → GTA VI Widget → Batterie → Non restreinte.",12));
  refresh();
 }
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==1908){ReleaseReminder.schedule(this);refresh();}}
 @Override protected void onResume(){super.onResume();if(preview!=null)refresh();}
 void refresh(){
  Config conf=Config.load(this);GtaWidget.updateAll(this);preview.removeAllViews();
  int w=(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-40;
  preview.addView(GtaWidget.views(this,conf,System.currentTimeMillis(),w,(int)(w*.75f)).apply(this,preview));
  AlarmManager a=getSystemService(AlarmManager.class);
  boolean exact=Build.VERSION.SDK_INT<31||a.canScheduleExactAlarms();
  boolean notifications=Build.VERSION.SDK_INT<33||checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED;
  state.setText((exact?"Horaires précis activés.":"Autorise les horaires précis.")+" "+(notifications?"Notification du 19/11 à 08 h activée.":"Autorise aussi les notifications pour le rappel à 08 h."));
 }
}