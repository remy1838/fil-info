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
  root.addView(text("Secondes en direct · Images à 00 h, 08 h et 16 h · Heure de Paris",14));
  preview=new FrameLayout(this);int w=(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-40;
  root.addView(preview,new LinearLayout.LayoutParams(-1,dp(w*.75f)));
  root.addView(text("Ton lien reste le même :",13));
  TextView link=text(Config.PAGE,12);link.setTextIsSelectable(true);root.addView(link);
  state=text("",13);root.addView(state);
  button("1. Autoriser les changements aux bonnes heures",()->{
   if(Build.VERSION.SDK_INT>=31&&!getSystemService(AlarmManager.class).canScheduleExactAlarms())startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));
   else Toast.makeText(this,"Horaires précis déjà autorisés",Toast.LENGTH_SHORT).show();
  });
  button("2. Ajouter le widget 4 × 3",()->{
   AppWidgetManager m=getSystemService(AppWidgetManager.class);
   if(m.isRequestPinAppWidgetSupported())m.requestPinAppWidget(new ComponentName(this,GtaWidget.class),null,null);
   else Toast.makeText(this,"Appui long sur l'accueil → Widgets → GTA VI Widget",Toast.LENGTH_LONG).show();
  });
  button("Synchroniser depuis mon lien",()->{
   state.setText("Synchronisation des trois images…");
   new Thread(()->{try{Config.sync(this);runOnUiThread(()->{state.setText("Visuels et date synchronisés.");refresh();});}
   catch(Exception e){runOnUiThread(()->state.setText("Connexion indisponible. Les visuels déjà installés restent utilisables."));}}).start();
  });
  button("Ouvrir mon lien",()->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(Config.PAGE))));
  root.addView(text("Les images officielles sont incluses : le widget fonctionne aussi hors connexion. Un appui sur le widget ouvre ta page. Aucune musique.",12));
  root.addView(text("Si Samsung met l'application en veille : Paramètres → Applications → GTA VI Widget → Batterie → Non restreinte.",12));
  refresh();
 }
 @Override protected void onResume(){super.onResume();if(preview!=null)refresh();}
 void refresh(){
  Config conf=Config.load(this);GtaWidget.updateAll(this);preview.removeAllViews();
  int w=(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-40;
  preview.addView(GtaWidget.views(this,conf,System.currentTimeMillis(),w,(int)(w*.75f)).apply(this,preview));
  AlarmManager a=getSystemService(AlarmManager.class);
  if(state.getText().length()==0)state.setText(Build.VERSION.SDK_INT<31||a.canScheduleExactAlarms()?"Changements d'image aux horaires précis activés.":"Autorise les horaires précis pour changer les images à l'heure prévue.");
 }
}