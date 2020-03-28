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
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import com.dar.nclientv2.components.classes.CustomSSLSocketFactory;
import com.dar.nclientv2.loginapi.LoadTags;
import com.dar.nclientv2.loginapi.User;
import com.dar.nclientv2.targets.BitmapTarget;
import com.dar.nclientv2.utility.LogUtility;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.acra.ACRA;

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

    public static int getFavoriteLimit(Context context) {
        return context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_favorite_limit),10);
    }

    public static String getLastVersion(Context context) {
        if(context!=null)lastVersion=context.getSharedPreferences("Settings", 0).getString("last_version","0.0.0");
        return lastVersion;
    }

    public static void setLastVersion(Context context) {
        lastVersion=getVersionName(context);
        context.getSharedPreferences("Settings", 0).edit().putString("last_version",lastVersion).apply();
    }



    public static int getColLandHistory() {
        return colLandHist;
    }

    public static int getColPortHistory() {
        return colPortHist;
    }

    public static void updateACRAReportStatus(Context context) {
        ACRA.getErrorReporter().setEnabled(context.getSharedPreferences("Settings",0).getBoolean(context.getString(R.string.key_send_report),true));
    }

    public enum ThemeScheme{LIGHT,DARK,BLACK}

    public static OkHttpClient client=null;
    public static  File OLD_GALLERYFOLDER;
    public static  File MAINFOLDER;
    public static  File DOWNLOADFOLDER;
    public static  File SCREENFOLDER;
    public static  File PDFFOLDER;
    public static  File UPDATEFOLDER;
    public static  File ZIPFOLDER;

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
        ZIPFOLDER =new File(MAINFOLDER,"ZIP");
    }


    public static final String LOGTAG="NCLIENTLOG";
    public static final String CHANNEL_ID1="download_gallery",CHANNEL_ID2="create_pdf",CHANNEL_ID3="create_pdf";
    private static Language onlyLanguage;
    private static TitleType titleType;
    private static boolean volumeOverride,byPopular,keepHistory,loadImages,highRes,lockScreen,onlyTag,showTitles,infiniteScroll, removeAvoidedGalleries,useRtl;
    private static ThemeScheme theme;
    private static String lastVersion;
    private static int maxHistory,notificationId,columnCount,maxId,galleryWidth=-1, galleryHeight =-1;
    private static int colPortHist,colLandHist,colPortMain,colLandMain,colPortDownload,colLandDownload,colLandFavorite,colPortFavorite;
    private static Point screenSize;

    public static int getGalleryWidth(){
        return galleryWidth;
    }
    public static void initScreenSize(AppCompatActivity activity){
        if(screenSize==null) {
            screenSize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(screenSize);
        }
    }
    private static void initGallerySize() {
        galleryHeight=screenSize.y/2;
        galleryWidth=(galleryHeight*3)/4;//the ratio is 3:4
    }
    public static int getScreenHeight(){
        return screenSize.y;
    }
    public static int getScreenWidth(){
        return screenSize.x;
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

    public static int getMaxHistory() {
        return maxHistory;
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
        volumeOverride=shared.getBoolean(context.getString(R.string.key_override_volume),true);
        columnCount=shared.getInt(context.getString(R.string.key_column_count),2);
        showTitles=shared.getBoolean(context.getString(R.string.key_show_titles),true);
        lockScreen=shared.getBoolean(context.getString(R.string.key_disable_lock),false);
        maxId=shared.getInt(context.getString(R.string.key_max_id),300000);
        maxHistory=shared.getInt(context.getString(R.string.key_max_history_size),2);
        colPortMain=shared.getInt(context.getString(R.string.key_column_port_main),2);
        colLandMain=shared.getInt(context.getString(R.string.key_column_land_main),4);
        colPortDownload=shared.getInt(context.getString(R.string.key_column_port_down),2);
        colLandDownload=shared.getInt(context.getString(R.string.key_column_land_down),4);
        colPortFavorite=shared.getInt(context.getString(R.string.key_column_port_favo),2);
        colLandFavorite=shared.getInt(context.getString(R.string.key_column_land_favo),4);
        colPortHist=shared.getInt(context.getString(R.string.key_column_port_hist),2);
        colLandHist=shared.getInt(context.getString(R.string.key_column_land_hist),4);
        int x=shared.getInt(context.getString(R.string.key_only_language),Language.ALL.ordinal());
        if(Language.values()[x]==Language.UNKNOWN){
            updateOnlyLanguage(context,Language.ALL);
            x=Language.ALL.ordinal();
        }
        onlyLanguage=Language.values()[x];

    }

    public static boolean volumeOverride() {
        return volumeOverride;
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
    public static Drawable getLogo(Resources resources){ return ResourcesCompat.getDrawable(resources,theme==ThemeScheme.LIGHT?R.drawable.ic_logo_dark:R.drawable.ic_logo,null); }
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
    @NonNull public static Language getOnlyLanguage() {
        return onlyLanguage;
    }
    public static boolean isHighRes() {
        return highRes;
    }
    public static boolean isOnlyTag() {
        return onlyTag;
    }

    public static boolean isLockScreen() {
        return lockScreen;
    }

    public static int getColLandDownload() {
        return colLandDownload;
    }

    public static int getColPortMain() {
        return colPortMain;
    }

    public static int getColLandMain() {
        return colLandMain;
    }

    public static int getColPortDownload() {
        return colPortDownload;
    }


    public static int getColLandFavorite() {
        return colLandFavorite;
    }

    public static int getColPortFavorite() {
        return colPortFavorite;
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
        LogUtility.d(
                "0:"+context.getFilesDir()+'\n'+
                "1:"+Global.MAINFOLDER+Global.MAINFOLDER.mkdirs()+'\n'+
                "2:"+Global.DOWNLOADFOLDER+Global.DOWNLOADFOLDER.mkdir()+'\n'+
                "3:"+Global.PDFFOLDER+Global.PDFFOLDER.mkdir()+'\n'+
                "4:"+Global.UPDATEFOLDER+Global.UPDATEFOLDER.mkdir()+'\n'+
                "5:"+Global.SCREENFOLDER+Global.SCREENFOLDER.mkdir()+'\n'+
                "5:"+Global.ZIPFOLDER+Global.ZIPFOLDER.mkdir()+'\n'
        );

        try {
            new File(Global.DOWNLOADFOLDER,".nomedia").createNewFile();
        } catch (IOException e) {e.printStackTrace();}
    }


    public static void updateOnlyLanguage(@NonNull Context context, @Nullable Language type){ context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_only_language)),type.ordinal()).apply();onlyLanguage=type; }
    public static void  updateByPopular(@NonNull Context context,boolean popular){context.getSharedPreferences("Settings", 0).edit().putBoolean(context.getString((R.string.key_by_popular)),popular).apply();byPopular=popular;}
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
            NotificationChannel channel3 = new NotificationChannel(CHANNEL_ID3, context.getString(R.string.channel3_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel1.setDescription(context.getString(R.string.channel1_description));
            channel2.setDescription(context.getString(R.string.channel2_description));
            channel3.setDescription(context.getString(R.string.channel3_description));
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null){
                notificationManager.createNotificationChannel(channel1);
                notificationManager.createNotificationChannel(channel2);
                notificationManager.createNotificationChannel(channel3);
            }
        }
    }
    public static void preloadImage(Context context, String url){
        if(!isLoadImages())return;
        //Glide.with(context).load(url).preload();
        Glide.with(context).load(url).preload();

    }
    public static BitmapTarget loadImageOp(Context context,ImageView view,File file){
        Drawable logo=getLogo(context.getResources());
        BitmapTarget target=new BitmapTarget(view);
        Glide.with(context).asBitmap().error(logo).placeholder(logo).load(file).into(target);
        return target;
    }


    public static BitmapTarget loadImageOp(Context context,ImageView view,String url){
        Drawable logo=getLogo(context.getResources());
        BitmapTarget target=new BitmapTarget(view);
        Glide.with(context).asBitmap().error(logo).placeholder(logo).load(url).into(target);
        return target;
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
            LogUtility.e(e.getMessage(),e);}
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
                    LogUtility.e(e.getLocalizedMessage(),e);
                }
            }
        }
        return null;
    }

    public static void initActivity(AppCompatActivity context){
        initScreenSize(context);
        initGallerySize();
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
