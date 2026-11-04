package com.globaltv.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/** GLOBAL TV Premium - Android TV / Google TV. */
public class MainActivity extends AppCompatActivity {
    private static final String DEFAULT_PLAYLIST="https://iptv-org.github.io/iptv/index.m3u";
    private static final int BG=0xFF080D19, PANEL=0xFF111A2B, CARD=0xFF17233A, MUTED=0xFF91A0BA, ACCENT=0xFF746BFF, CYAN=0xFF27C8C0;
    private final ExecutorService io=Executors.newFixedThreadPool(2);
    private final ArrayList<MediaEntry> all=new ArrayList<>();
    private final Set<String> favorites=new HashSet<>();
    private LinearLayout root,sidebar,content,list,chips;
    private EditText search;
    private TextView status,heading,sourceLabel;
    private PlayerView playerView;
    private ExoPlayer player;
    private String section="ACCUEIL",activeFilter="Tout",sourceMode="M3U",xtreamBase="",xtreamUser="",xtreamPass="";

    static class MediaEntry {
        String name,url,group,logo,type,id,container;
        MediaEntry(String n,String u,String g,String l,String t){name=n;url=u;group=g;logo=l;type=t;id="";container="m3u8";}
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);favorites.addAll(getPreferences(0).getStringSet("favorites",new HashSet<>()));buildUi();loadPlaylist(DEFAULT_PLAYLIST);}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void buildUi(){
        root=row();root.setBackgroundColor(BG);setContentView(root);
        sidebar=column();sidebar.setPadding(dp(18),dp(22),dp(12),dp(14));sidebar.setBackgroundColor(0xFF0D1525);root.addView(sidebar,new LinearLayout.LayoutParams(dp(220),-1));
        TextView brand=text("GLOBAL TV",25,Color.WHITE,true);sidebar.addView(brand,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView premium=text("P R E M I U M",10,CYAN,true);sidebar.addView(premium,new LinearLayout.LayoutParams(-1,dp(25)));
        TextView tagline=text("LE DIVERTISSEMENT\nSANS FRONTIÈRES",10,MUTED,true);sidebar.addView(tagline,new LinearLayout.LayoutParams(-1,dp(54)));
        addNav("⌂   Accueil","ACCUEIL");addNav("▣   TV en direct","TV EN DIRECT");addNav("▶   Films","FILMS");addNav("▤   Séries","SÉRIES");addNav("✦   Animés","ANIMÉS");addNav("♡   Mes favoris","FAVORIS");
        View spacer=new View(this);sidebar.addView(spacer,new LinearLayout.LayoutParams(1,0,1));
        sourceLabel=text("SOURCE : M3U",10,CYAN,true);sidebar.addView(sourceLabel,new LinearLayout.LayoutParams(-1,dp(28)));
        addNav("⚙   Sources & paramètres","PARAMÈTRES");
        content=column();content.setPadding(dp(25),dp(18),dp(25),dp(14));root.addView(content,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout top=row();content.addView(top,new LinearLayout.LayoutParams(-1,dp(58)));
        heading=text("Bienvenue sur GLOBAL TV",26,Color.WHITE,true);top.addView(heading,new LinearLayout.LayoutParams(0,-1,1));
        search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);search.setHintTextColor(MUTED);search.setTextColor(Color.WHITE);search.setHint("⌕  Rechercher un contenu…");search.setPadding(dp(14),0,dp(14),0);search.setBackground(bg(CARD,12));top.addView(search,new LinearLayout.LayoutParams(dp(300),dp(44)));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){renderList();}public void afterTextChanged(Editable e){}});
        chips=row();chips.setPadding(0,dp(12),0,dp(9));content.addView(chips,new LinearLayout.LayoutParams(-1,dp(54)));
        status=text("Préparation de votre catalogue…",12,MUTED,false);content.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));list=column();list.setPadding(0,dp(4),0,dp(12));scroll.addView(list);
        TextView foot=text("GLOBAL TV • Les flux et catalogues dépendent des droits, de la disponibilité et des régions.",10,0xFF65738D,false);content.addView(foot,new LinearLayout.LayoutParams(-1,dp(26)));
        updateChips();
    }
    private void addNav(String label,String target){TextView v=text(label,14,0xFFD9E1F2,true);v.setPadding(dp(13),0,dp(8),0);v.setFocusable(true);v.setBackground(bg(0x00172035,10));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(47));p.bottomMargin=dp(3);sidebar.addView(v,p);v.setOnClickListener(w->{section=target;activeFilter="Tout";heading.setText(target.equals("ACCUEIL")?"Bienvenue sur GLOBAL TV":target);search.setHint("⌕  Rechercher…");updateChips();renderList();if(target.equals("PARAMÈTRES"))showSettings();});}
    private void updateChips(){if(chips==null)return;chips.removeAllViews();String[] opts;if(section.equals("TV EN DIRECT"))opts=new String[]{"Tout","France","Afrique","Sports","News","Music","Kids"};else if(section.equals("FAVORIS"))opts=new String[]{"Tout"};else opts=new String[]{"Tout","Action","Comédie","Drame","Aventure","Fantasy","Science-fiction","Animation"};for(String s:opts){TextView c=text(s,12,activeFilter.equals(s)?Color.WHITE:0xFFB8C4D8,true);c.setGravity(Gravity.CENTER);c.setPadding(dp(15),0,dp(15),0);c.setBackground(bg(activeFilter.equals(s)?ACCENT:CARD,18));c.setFocusable(true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(34));p.rightMargin=dp(8);chips.addView(c,p);c.setOnClickListener(v->{activeFilter=s;updateChips();renderList();});}}

    private HttpURLConnection open(String address)throws Exception{URL u=new URL(address);HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(12000);c.setReadTimeout(25000);c.setRequestProperty("User-Agent","GLOBAL-TV-Android/2.0");c.setInstanceFollowRedirects(true);return c;}
    private String get(String address)throws Exception{HttpURLConnection c=open(address);try{int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();if(code<200||code>=300)throw new IOException("Serveur HTTP "+code);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>18*1024*1024)throw new IOException("Réponse trop volumineuse");out.write(b,0,n);}in.close();return out.toString("UTF-8");}finally{c.disconnect();}}
    private void loadPlaylist(String url){status.setText("Connexion à la playlist M3U…");io.execute(()->{ArrayList<MediaEntry> parsed=new ArrayList<>();String err=null;try{String data=get(url);BufferedReader r=new BufferedReader(new StringReader(data));String line,name=null,group="Autres",logo="";int count=0;while((line=r.readLine())!=null&&count<50000){line=line.trim();if(line.startsWith("#EXTINF:")){name=line.substring(line.lastIndexOf(',')+1).trim();group="Autres";logo="";int g=line.indexOf("group-title=\"");if(g>=0){int e=line.indexOf('"',g+13);if(e>g)group=line.substring(g+13,e);}int l=line.indexOf("tvg-logo=\"");if(l>=0){int e=line.indexOf('"',l+10);if(e>l)logo=line.substring(l+10,e);}}else if(!line.isEmpty()&&!line.startsWith("#")&&name!=null){if(line.startsWith("https://")||line.startsWith("http://")){parsed.add(new MediaEntry(name,line,group,logo,"m3u"));count++;}name=null;}}}catch(Exception e){err=e.getMessage();}final String error=err;runOnUiThread(()->{if(error!=null){status.setText("Erreur M3U : "+error);return;}all.clear();all.addAll(parsed);sourceMode="M3U";sourceLabel.setText("SOURCE : M3U");status.setText(parsed.size()+" éléments chargés • Playlist M3U");renderList();});});}

    private String api(String action,String extra){String base=xtreamBase;if(!base.endsWith("/"))base+="/";return base+"player_api.php?username="+enc(xtreamUser)+"&password="+enc(xtreamPass)+"&action="+action+(extra==null?"":extra);}
    private String enc(String s){try{return java.net.URLEncoder.encode(s,"UTF-8");}catch(Exception e){return s;}}
    private void loadXtream(){status.setText("Connexion au serveur Xtream Codes…");io.execute(()->{ArrayList<MediaEntry> parsed=new ArrayList<>();String err=null;try{String auth=get(xtreamBase+(xtreamBase.endsWith("/")?"":"/")+"player_api.php?username="+enc(xtreamUser)+"&password="+enc(xtreamPass));JSONObject root=new JSONObject(auth);JSONObject user=root.optJSONObject("user_info");if(user!=null&&!user.optString("auth","1").equals("1"))throw new IOException("Identifiants refusés par le serveur");
                Map<String,String> liveCats=safeCategories("get_live_categories");Map<String,String> vodCats=safeCategories("get_vod_categories");Map<String,String> seriesCats=safeCategories("get_series_categories");
                parseXtreamArray(parsed,get(api("get_live_streams","")),"live","m3u8",liveCats);
                try{parseXtreamArray(parsed,get(api("get_vod_streams","")),"movie","mp4",vodCats);}catch(Exception ignored){}
                try{parseXtreamArray(parsed,get(api("get_series","")),"series","mp4",seriesCats);}catch(Exception ignored){}
            }catch(Exception e){err=e.getMessage();}final String error=err;runOnUiThread(()->{if(error!=null){status.setText("Connexion Xtream impossible : "+error);return;}all.clear();all.addAll(parsed);sourceMode="XTREAM";sourceLabel.setText("SOURCE : XTREAM");status.setText(parsed.size()+" éléments récupérés • Serveur Xtream");renderList();});});}
    private Map<String,String> safeCategories(String action){try{return parseCategories(get(api(action,"")));}catch(Exception ignored){return new HashMap<>();}}
    private Map<String,String> parseCategories(String json){Map<String,String> map=new HashMap<>();try{JSONArray a=new JSONArray(json);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)map.put(o.optString("category_id"),o.optString("category_name","Autres"));}}catch(Exception ignored){}return map;}
    private void parseXtreamArray(ArrayList<MediaEntry> out,String json,String type,String fallback,Map<String,String> categories){try{JSONArray a=new JSONArray(json);for(int i=0;i<a.length()&&out.size()<50000;i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;String id=o.optString(type.equals("series")?"series_id":"stream_id",o.optString("id",""));if(id.isEmpty())continue;String name=o.optString("name",type.equals("series")?"Série":"Contenu");String cat=o.optString("category_name",(categories.containsKey(o.optString("category_id"))?categories.get(o.optString("category_id")):(type.equals("live")?"TV en direct":type.equals("movie")?"Films":"Séries")));String logo=o.optString("stream_icon",o.optString("cover",""));String ext=o.optString("container_extension",fallback);String path=xtreamBase+(xtreamBase.endsWith("/")?"":"/")+type+"/"+enc(xtreamUser)+"/"+enc(xtreamPass)+"/"+id+"."+ext;MediaEntry m=new MediaEntry(name,path,cat,logo,type);m.id=id;m.container=ext;out.add(m);}}catch(Exception ignored){}}

    private boolean matchSection(MediaEntry c){String g=c.group.toLowerCase(Locale.ROOT),n=c.name.toLowerCase(Locale.ROOT);if(section.equals("FAVORIS"))return favorites.contains(c.url);if(section.equals("TV EN DIRECT"))return c.type.equals("live")||c.type.equals("m3u");if(section.equals("FILMS"))return c.type.equals("movie")||g.contains("movie")||g.contains("film")||g.contains("vod");if(section.equals("SÉRIES"))return c.type.equals("series")||g.contains("series");if(section.equals("ANIMÉS"))return g.contains("anime")||g.contains("animation")||g.contains("cartoon")||n.contains("anime");return true;}
    private void renderList(){if(list==null)return;list.removeAllViews();String q=search==null?"":search.getText().toString().toLowerCase(Locale.ROOT);int shown=0;for(MediaEntry c:all){if(!matchSection(c))continue;if(!q.isEmpty()&&!c.name.toLowerCase(Locale.ROOT).contains(q)&&!c.group.toLowerCase(Locale.ROOT).contains(q))continue;if(!activeFilter.equals("Tout")&&!c.group.toLowerCase(Locale.ROOT).contains(activeFilter.toLowerCase(Locale.ROOT))&&!c.name.toLowerCase(Locale.ROOT).contains(activeFilter.toLowerCase(Locale.ROOT)))continue;
            LinearLayout item=row();item.setPadding(dp(13),dp(4),dp(12),dp(4));item.setBackground(bg(PANEL,12));item.setFocusable(true);item.setClickable(true);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(62));ip.bottomMargin=dp(8);list.addView(item,ip);
            TextView icon=text(c.type.equals("live")||c.type.equals("m3u")?"▶":c.type.equals("series")?"▤":"▣",20,CYAN,true);icon.setGravity(Gravity.CENTER);item.addView(icon,new LinearLayout.LayoutParams(dp(48),-1));LinearLayout meta=column();item.addView(meta,new LinearLayout.LayoutParams(0,-1,1));meta.addView(text(c.name,14,Color.WHITE,true));meta.addView(text(c.group+"  •  "+(c.type.equals("live")||c.type.equals("m3u")?"En direct":c.type.equals("series")?"Série":"VOD"),10,MUTED,false));
            TextView fav=text(favorites.contains(c.url)?"♥":"♡",22,0xFFFF7199,true);fav.setGravity(Gravity.CENTER);item.addView(fav,new LinearLayout.LayoutParams(dp(44),-1));fav.setOnClickListener(v->{if(!favorites.add(c.url))favorites.remove(c.url);getPreferences(0).edit().putStringSet("favorites",new HashSet<>(favorites)).apply();fav.setText(favorites.contains(c.url)?"♥":"♡");});item.setOnClickListener(v->{if(c.type.equals("series"))showSeries(c);else play(c);});shown++;if(shown>=300)break;}
        if(shown==0){TextView empty=text(all.isEmpty()?"Aucun contenu chargé. Configure une source M3U ou Xtream dans Paramètres.":"Aucun résultat dans cette rubrique. Vérifie la source ou le filtre.",14,MUTED,false);empty.setPadding(dp(12),dp(30),dp(12),dp(20));list.addView(empty);}}
    private void play(MediaEntry c){if(player!=null)startPlayer(c);else startPlayer(c);}
    private void startPlayer(MediaEntry c){try{if(player!=null){player.release();player=null;}if(playerView!=null){content.removeView(playerView);playerView=null;}player=new ExoPlayer.Builder(this).build();playerView=new PlayerView(this);playerView.setPlayer(player);playerView.setUseController(true);playerView.setBackgroundColor(Color.BLACK);content.addView(playerView,2,new LinearLayout.LayoutParams(-1,dp(285)));player.setMediaItem(MediaItem.fromUri(Uri.parse(c.url)));player.prepare();player.play();player.addListener(new Player.Listener(){@Override public void onPlayerError(androidx.media3.common.PlaybackException e){runOnUiThread(()->Toast.makeText(MainActivity.this,"Lecture impossible : flux indisponible ou format non pris en charge",Toast.LENGTH_LONG).show());}});status.setText("Lecture : "+c.name);}catch(Exception e){Toast.makeText(this,"Impossible de lancer ce flux",Toast.LENGTH_LONG).show();}}
    private void showSeries(MediaEntry s){if(!sourceMode.equals("XTREAM")){Toast.makeText(this,"Les épisodes doivent être fournis par une source VOD compatible.",Toast.LENGTH_LONG).show();return;}status.setText("Chargement des saisons…");io.execute(()->{String err=null;ArrayList<MediaEntry> eps=new ArrayList<>();try{JSONObject data=new JSONObject(get(api("get_series_info","&series_id="+enc(s.id))));JSONObject seasons=data.optJSONObject("episodes");if(seasons!=null){Iterator<String> keys=seasons.keys();while(keys.hasNext()){String k=keys.next();JSONArray a=seasons.optJSONArray(k);if(a==null)continue;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;String id=o.optString("id","");if(id.isEmpty())continue;String ext=o.optString("container_extension","mp4");String u=xtreamBase+(xtreamBase.endsWith("/")?"":"/")+"series/"+enc(xtreamUser)+"/"+enc(xtreamPass)+"/"+id+"."+ext;eps.add(new MediaEntry(o.optString("title","Épisode "+(i+1)),u,"Saison "+k,"","episode"));}}}}catch(Exception e){err=e.getMessage();}final String error=err;runOnUiThread(()->{if(error!=null||eps.isEmpty()){Toast.makeText(this,"Aucun épisode disponible",Toast.LENGTH_LONG).show();return;}String[] names=new String[eps.size()];for(int i=0;i<eps.size();i++)names[i]=eps.get(i).group+" — "+eps.get(i).name;new AlertDialog.Builder(this).setTitle(s.name).setItems(names,(d,w)->play(eps.get(w))).setNegativeButton("Fermer",null).show();});});}

    private void showSettings(){LinearLayout box=column();box.setPadding(dp(8),dp(6),dp(8),dp(4));TextView info=text("Choisis une source de contenus que tu es autorisé à utiliser. Les identifiants Xtream restent en mémoire uniquement pendant cette session.",12,MUTED,false);box.addView(info);new AlertDialog.Builder(this).setTitle("Sources & paramètres").setMessage("M3U : playlist HTTPS/HTTP. Xtream : URL du serveur + identifiant + mot de passe.").setView(box).setNeutralButton("Charger M3U",(d,w)->showM3uDialog()).setPositiveButton("Connexion Xtream",(d,w)->showXtreamDialog()).setNegativeButton("Fermer",null).show();}
    private void showM3uDialog(){EditText input=new EditText(this);input.setSingleLine(true);input.setHint("https://exemple.com/playlist.m3u");input.setTextColor(Color.WHITE);input.setHintTextColor(MUTED);input.setText(DEFAULT_PLAYLIST);new AlertDialog.Builder(this).setTitle("Playlist M3U").setMessage("Entre l’adresse d’une playlist dont tu possèdes les droits d’utilisation.").setView(input).setNegativeButton("Annuler",null).setPositiveButton("Charger",(d,w)->{String u=input.getText().toString().trim();if(u.startsWith("https://")||u.startsWith("http://")){section="TV EN DIRECT";heading.setText(section);loadPlaylist(u);}else Toast.makeText(this,"Adresse HTTP/HTTPS requise",Toast.LENGTH_LONG).show();}).show();}
    private void showXtreamDialog(){LinearLayout form=column();form.setPadding(dp(8),dp(4),dp(8),dp(4));EditText host=field("URL du serveur (https://serveur:port)");EditText user=field("Nom d’utilisateur");EditText pass=field("Mot de passe");pass.setInputType(129);form.addView(host);form.addView(user);form.addView(pass);new AlertDialog.Builder(this).setTitle("Connexion Xtream Codes").setMessage("Utilise les identifiants fournis par ton fournisseur IPTV autorisé. HTTPS est recommandé.").setView(form).setNegativeButton("Annuler",null).setPositiveButton("Se connecter",(d,w)->{String h=host.getText().toString().trim(),u=user.getText().toString().trim(),p=pass.getText().toString();try{Uri uri=Uri.parse(h);if((!"https".equalsIgnoreCase(uri.getScheme())&&!("http".equalsIgnoreCase(uri.getScheme())))||uri.getHost()==null||u.isEmpty()||p.isEmpty())throw new Exception();xtreamBase=h;xtreamUser=u;xtreamPass=p;section="ACCUEIL";heading.setText("Bienvenue sur GLOBAL TV");loadXtream();}catch(Exception e){Toast.makeText(this,"Vérifie l’URL et les identifiants",Toast.LENGTH_LONG).show();}}).show();}
    private EditText field(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(Color.WHITE);e.setHintTextColor(MUTED);e.setHint(hint);e.setPadding(dp(10),dp(6),dp(10),dp(6));return e;}
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event){if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getKeyCode()==KeyEvent.KEYCODE_BACK&&playerView!=null){content.removeView(playerView);playerView=null;if(player!=null){player.release();player=null;}status.setText("Lecture fermée");return true;}return super.dispatchKeyEvent(event);}
    @Override protected void onStop(){super.onStop();if(player!=null)player.pause();}
    @Override protected void onDestroy(){super.onDestroy();if(player!=null){player.release();player=null;}io.shutdownNow();}
}
