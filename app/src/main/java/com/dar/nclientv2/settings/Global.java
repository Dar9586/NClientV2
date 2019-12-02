package com.dar.nclientv2.settings;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.bumptech.glide.Glide;
import com.dar.nclientv2.CopyToClipboardActivity;
import com.dar.nclientv2.MainActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.components.CustomSSLSocketFactory;
import com.dar.nclientv2.loginapi.LoadTags;
import com.dar.nclientv2.loginapi.User;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;

import okhttp3.OkHttpClient;

public class Global {
    public static long recursiveSize(File path) {
        if(path.isFile())return path.length();
        long size=0;
        for(File f:path.listFiles())
            size+=f.isFile()?f.length():recursiveSize(f);

        return size;
    }

    public static void updateFavoriteLimit(Context context, int limit) {
        context.getSharedPreferences("Settings", 0).edit().putInt(context.getString(R.string.key_favorite_limit),limit).apply();
    }
    public static int getFavoriteLimit(Context context) {
        return context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_favorite_limit),10);
    }

    public static String getLastVersion(Context context) {
        return context.getSharedPreferences("Settings", 0).getString("last_version","0.0.0");

    }

    public static void setLastVersion(Context context) {
        context.getSharedPreferences("Settings", 0).edit().putString("last_version",getVersionName(context)).apply();
    }

    public enum ThemeScheme{LIGHT,DARK,BLACK}

    public static OkHttpClient client=null;
    public static  File OLD_GALLERYFOLDER;
    public static  File MAINFOLDER;
    public static  File DOWNLOADFOLDER;
    public static  File SCREENFOLDER;
    public static  File PDFFOLDER;
    public static  File UPDATEFOLDER;

    private static void initFilesTree(Context context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            MAINFOLDER=new File(context.getExternalFilesDir(null),"NClientV2");
        }else{
            MAINFOLDER=new File(Environment.getExternalStorageDirectory(),"NClientV2");
        }
        OLD_GALLERYFOLDER=new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"NClientV2");
        DOWNLOADFOLDER=new File(MAINFOLDER,"Download");
        SCREENFOLDER =new File(MAINFOLDER,"Screen");
        PDFFOLDER =new File(MAINFOLDER,"PDF");
        UPDATEFOLDER =new File(MAINFOLDER,"Update");
    }


    public static final String LOGTAG="NCLIENTLOG";
    public static final String CHANNEL_ID1="download_gallery",CHANNEL_ID2="create_pdf";
    private static Language onlyLanguage=null;
    private static TitleType titleType;
    private static boolean byPopular,keepHistory,fourColumnPort,fourColumnLand,loadImages,highRes,onlyTag,showTitles,infiniteScroll, removeAvoidedGalleries,useRtl;
    private static ThemeScheme theme;
    private static int notificationId,columnCount,maxId,galleryWidth=-1, galleryHeight =-1;


    public static int getGalleryWidth(){
        return galleryWidth;
    }

    public static void setGalleryWidth(int galleryWidth){
        Global.galleryWidth = galleryWidth;
    }

    public static int getGalleryHeight(){
        return galleryHeight;
    }

    public static void setGalleryHeigth(int galleryHeight){
        Global.galleryHeight = galleryHeight;
    }


    private static void initTitleType(@NonNull Context context){
        String s=context.getSharedPreferences("Settings", 0).getString(context.getString(R.string.key_title_type),"pretty");
        switch (s){
            case "pretty":titleType= TitleType.PRETTY;break;
            case "english":titleType=  TitleType.ENGLISH;break;
            case "japanese":titleType=  TitleType.JAPANESE;break;
        }
    }

    public static void initFromShared(@NonNull Context context){
        SharedPreferences shared=context.getSharedPreferences("Settings", 0);
        initHttpClient(context);
        initTitleType(context);
        initTheme(context);
        loadNotificationChannel(context);
        Login.initUseAccountTag(context);

        useRtl=     shared.getBoolean(context.getString(R.string.key_use_rtl),false);
        byPopular=  shared.getBoolean(context.getString(R.string.key_by_popular),false);
        keepHistory=shared.getBoolean(context.getString(R.string.key_keep_history),true);
        infiniteScroll=shared.getBoolean(context.getString(R.string.key_infinite_scroll),false);
        highRes=shared.getBoolean(context.getString(R.string.key_high_res_gallery),true);
        removeAvoidedGalleries =shared.getBoolean(context.getString(R.string.key_remove_ignored),true);
        onlyTag=shared.getBoolean(context.getString(R.string.key_ignore_tags),true);
        loadImages=shared.getBoolean(context.getString(R.string.key_load_images),true);
        columnCount=shared.getInt(context.getString(R.string.key_column_count),2);
        showTitles=shared.getBoolean(context.getString(R.string.key_show_titles),true);
        fourColumnLand=shared.getBoolean(context.getString(R.string.key_four_column_land),true);
        fourColumnPort=shared.getBoolean(context.getString(R.string.key_four_column_port),true);
        maxId=shared.getInt(context.getString(R.string.key_max_id),291738);
        int x=shared.getInt(context.getString(R.string.key_only_language),-1);
        onlyLanguage=x==-1?null:Language.values()[x];

    }



    private static void initHttpClient(@NonNull Context context){
        if(client!=null)return;
        OkHttpClient.Builder builder=new OkHttpClient.Builder()
                .cookieJar(new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context.getSharedPreferences("Login",0))));

        CustomSSLSocketFactory.enableTls12OnPreLollipop(builder);
        builder.addInterceptor(new CustomInterceptor());
        client=builder.build();
        client.dispatcher().setMaxRequests(25);
        client.dispatcher().setMaxRequestsPerHost(25);
        if(Login.isLogged()&&Login.getUser()==null){
            User.createUser(user -> {
                if(user!=null){
                    new LoadTags(null).start();
                    if(context instanceof MainActivity){
                        ((MainActivity) context).runOnUiThread(() -> ((MainActivity)context).loginItem.setTitle(context.getString(R.string.login_formatted,user.getUsername())));
                    }
                }
            });
        }
    }
    public static Locale initLanguage(Context context){
        String x=context.getSharedPreferences("Settings",0).getString(context.getString(R.string.key_language),"en");
        return new Locale(x);
    }
    private static ThemeScheme initTheme(Context context){
        String h=context.getSharedPreferences("Settings",0).getString(context.getString(R.string.key_theme_select),"dark");
        return theme=h.equals("light")?ThemeScheme.LIGHT:h.equals("dark")?ThemeScheme.DARK:ThemeScheme.BLACK;
    }

    public static boolean shouldCheckForUpdates(Context context){
        return context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_check_update),true);
    }
    private static int getLogo(){ return theme==ThemeScheme.LIGHT?R.drawable.ic_logo_dark:R.drawable.ic_logo; }
    private static Drawable getLogo(Resources resources){ return ResourcesCompat.getDrawable(resources,theme==ThemeScheme.LIGHT?R.drawable.ic_logo_dark:R.drawable.ic_logo,null); }
    public static TitleType getTitleType() {
        return titleType;
    }
    public static int getNotificationId() {
        return notificationId++;
    }
    public static ThemeScheme getTheme() {
        return theme;
    }
    public static boolean removeAvoidedGalleries(){return removeAvoidedGalleries;}
    @Nullable public static Language getOnlyLanguage() {
        return onlyLanguage;
    }
    public static boolean isHighRes() {
        return highRes;
    }
    public static boolean isOnlyTag() {
        return onlyTag;
    }

    public static boolean isFourColumnLand() {
        return fourColumnLand;
    }

    public static boolean isFourColumnPort() {
        return fourColumnPort;
    }

    public static boolean isKeepHistory() {
        return keepHistory;
    }

    public static boolean useRtl() {
        return useRtl;
    }

    public static boolean showTitles() {
        return showTitles;
    }

    public static boolean isByPopular() {
        return byPopular;
    }
    public static boolean isInfiniteScroll(){
        return infiniteScroll;
    }
    public static int getColumnCount() {
        return columnCount;
    }
    public static int getMaxId() {
        return maxId;
    }
    public static boolean isLoadImages() {
        return loadImages;
    }

    public static void initStorage(Context context){
        if(!Global.hasStoragePermission(context))return;
        Global.initFilesTree(context);
        Log.d(Global.LOGTAG,
                "0:"+context.getFilesDir()+'\n'+
                "1:"+Global.MAINFOLDER+Global.MAINFOLDER.mkdirs()+'\n'+
                "2:"+Global.DOWNLOADFOLDER+Global.DOWNLOADFOLDER.mkdir()+'\n'+
                "3:"+Global.PDFFOLDER+Global.PDFFOLDER.mkdir()+'\n'+
                "4:"+Global.UPDATEFOLDER+Global.UPDATEFOLDER.mkdir()+'\n'+
                "5:"+Global.SCREENFOLDER+Global.SCREENFOLDER.mkdir()+'\n'
        );

        try {
            new File(Global.DOWNLOADFOLDER,".nomedia").createNewFile();
        } catch (IOException e) {e.printStackTrace();}
    }


    public static void updateOnlyLanguage(@NonNull Context context, @Nullable Language type){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_only_language)),type==null?-1:type.ordinal()).apply();onlyLanguage=type; }
    public static boolean  updateByPopular(@NonNull Context context,boolean popular){context.getSharedPreferences("Settings", 0).edit().putBoolean(context.getString((R.string.key_by_popular)),popular).apply();byPopular=popular; return byPopular;}
    public static boolean  updateLoadImages(@NonNull Context context,boolean load){context.getSharedPreferences("Settings", 0).edit().putBoolean(context.getString((R.string.key_load_images)),load).apply();loadImages=load; return loadImages;}
    public static void updateColumnCount(@NonNull Context context, int count){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_column_count)),count).apply();columnCount=count; }
    public static void updateMaxId(@NonNull Context context, int id){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_max_id)),id).apply();maxId=id; }

    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }
    public static int getNavigationBarHeight(Context context){
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }
    public static void shareGallery(Context context, GenericGallery gallery) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,gallery.getTitle()+":\nhttps://nhentai.net/g/"+gallery.getId());
        sendIntent.setType("text/plain");
        Intent clipboardIntent = new Intent(context, CopyToClipboardActivity.class);
        clipboardIntent.setData(Uri.parse("https://nhentai.net/g/"+gallery.getId()));
        Intent chooserIntent = Intent.createChooser(sendIntent,context.getString(R.string.share_with));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { clipboardIntent });
        context.startActivity(chooserIntent);
    }

    public static void setTint(Drawable drawable){
        if(drawable==null)return;
        DrawableCompat.setTint(drawable,theme== ThemeScheme.LIGHT?Color.BLACK:Color.WHITE);
    }

    private static void loadNotificationChannel(@NonNull Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel1 = new NotificationChannel(CHANNEL_ID1, context.getString(R.string.channel1_name), NotificationManager.IMPORTANCE_DEFAULT);
            NotificationChannel channel2 = new NotificationChannel(CHANNEL_ID2, context.getString(R.string.channel2_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel1.setDescription(context.getString(R.string.channel1_description));
            channel2.setDescription(context.getString(R.string.channel2_description));
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null){
                notificationManager.createNotificationChannel(channel1);
                notificationManager.createNotificationChannel(channel2);
            }
        }
    }
    public static void preloadImage(Context context, String url){
        if(!isLoadImages())return;
        //Glide.with(context).load(url).preload();
        Glide.with(context).load(url).preload();

    }

    public static void loadImage(String url, final ImageView imageView){loadImage(url,imageView,false);}
    public static void loadImage(String url, final ImageView imageView,boolean force){
        if(loadImages||force)Glide.with(imageView).load(url).placeholder(getLogo(imageView.getResources())).into(imageView);
        else Glide.with(imageView).load(getLogo(imageView.getResources())).into(imageView);
    }
    public static void loadImage(File file, ImageView imageView){
        if(loadImages)Glide.with(imageView).load(file).placeholder(getLogo(imageView.getResources())).into(imageView);
        else Glide.with(imageView).load(getLogo(imageView.getResources())).into(imageView);

    }
    public static void loadImage(@DrawableRes int drawable, ImageView imageView){
        imageView.setImageResource(drawable);
    }



    public static boolean hasStoragePermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isCorrupted(String path){
        if(!new File(path).exists())return true;
        try (RandomAccessFile fh = new RandomAccessFile(path, "r")) {
            long length = fh.length();
            if (length < 10L) {
                return true;
            }
            fh.seek(length - 2);
            byte[] eoi = new byte[2];
            fh.read(eoi);
            return eoi[0] != (byte)0xFF || eoi[1] != (byte)0xD9; // FF D9
        }catch (IOException e){
            Log.e(Global.LOGTAG,e.getMessage(),e);}
        return true;
    }

    @Nullable
    public static File findGalleryFolder(int id){
        DOWNLOADFOLDER.mkdirs();
        File[] tmp=DOWNLOADFOLDER.listFiles();
        if(tmp!=null)
        for (File tmp2 : tmp) {
            if (tmp2.isDirectory() && (tmp2 = new File(tmp2, ".nomedia")).exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(tmp2));
                    String h = br.readLine();
                    br.close();
                    if (h!=null&&h.equals("" + id))return tmp2.getParentFile();

                } catch (IOException e) {
                    Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
                }
            }
        }
        return null;
    }

    public static void loadThemeAndLanguage(Context context){

        //Locale locale=new Locale()
        Resources resources=context.getResources();
        Locale locale=initLanguage(context);
        Configuration c=new Configuration(context.getResources().getConfiguration());
        c.locale=locale;
        resources.updateConfiguration(c, resources.getDisplayMetrics());
        switch (initTheme(context)){
            case LIGHT:context.setTheme(R.style.LightTheme);break;
            case DARK:context.setTheme(R.style.DarkTheme);break;
            case BLACK:context.setTheme(R.style.Theme_Amoled);break;
        }
    }

    public static void recursiveDelete(File file){
        if(!file.exists())return;
        if(file.isFile()){file.delete();return;}
        for(File x:file.listFiles())recursiveDelete(x);
        file.delete();
    }

    @Nullable
    public static String getVersionName(Context context){
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


}
