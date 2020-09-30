package com.dar.nclientv2.settings;

import android.Manifest;
import android.app.Activity;
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
import android.util.DisplayMetrics;
import android.webkit.CookieSyncManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.dar.nclientv2.CopyToClipboardActivity;
import com.dar.nclientv2.MainActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.SortType;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.components.classes.CustomSSLSocketFactory;
import com.dar.nclientv2.loginapi.LoadTags;
import com.dar.nclientv2.loginapi.User;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;
import com.dar.nclientv2.utility.network.NetworkUtil;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.acra.ACRA;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class Global {
    public static OkHttpClient client=null;
    public static  File OLD_GALLERYFOLDER;
    public static  File MAINFOLDER;
    public static  File DOWNLOADFOLDER;
    public static  File SCREENFOLDER;
    public static  File PDFFOLDER;
    public static  File UPDATEFOLDER;
    public static  File ZIPFOLDER;

    private static final String MAINFOLDER_NAME="NClientV2";
    private static final String DOWNLOADFOLDER_NAME="Download";
    private static final String SCREENFOLDER_NAME="Screen";
    private static final String PDFFOLDER_NAME="PDF";
    private static final String UPDATEFOLDER_NAME="Update";
    private static final String ZIPFOLDER_NAME="ZIP";
    public static final String CHANNEL_ID1="download_gallery",CHANNEL_ID2="create_pdf",CHANNEL_ID3="create_zip";

    private static Language onlyLanguage;
    private static TitleType titleType;
    private static SortType sortType;
    private static boolean enableBeta,localSortByName,alternativeSite,volumeOverride,zoomOneColumn,keepHistory,lockScreen,onlyTag,showTitles,infiniteScroll, removeAvoidedGalleries,useRtl;
    private static ThemeScheme theme;
    private static DataUsageType usageMobile, usageWifi;
    private static String lastVersion;
    private static int maxHistory,columnCount,maxId,galleryWidth=-1, galleryHeight =-1;
    private static int colPortStat,colLandStat,colPortHist,colLandHist,colPortMain,colLandMain,colPortDownload,colLandDownload,colLandFavorite,colPortFavorite;
    private static int defaultZoom;
    private static Point screenSize;
    private static final DisplayMetrics lastDisplay=new DisplayMetrics();


    public static long recursiveSize(File path) {
        if(path.isFile())return path.length();
        long size=0;
        File[]files=path.listFiles();
        if(files==null)return size;
        for(File f:files)
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

    public static void setEnableBeta(boolean enableBeta) {
        Global.enableBeta = enableBeta;
    }

    public static boolean isEnableBeta() {
        return enableBeta;
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

    public static int getColLandStatus() {
        return colLandStat;
    }

    public static int getColPortStatus() {
        return colPortStat;
    }

    public static void updateACRAReportStatus(Context context) {
        ACRA.getErrorReporter().setEnabled(context.getSharedPreferences("Settings",0).getBoolean(context.getString(R.string.key_send_report),true));
    }

    public static boolean isDestroyed(Activity activity) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed();
    }

    public static String getUserAgent() {
        return "NClientV2 "+Global.getLastVersion(null);
    }

    public enum ThemeScheme{LIGHT,DARK}
    public enum DataUsageType{NONE,THUMBNAIL,FULL}

    public static boolean isLocalSortByName() {
        return localSortByName;
    }

    public static void toggleLocalSort(Context context) {
        Global.localSortByName = !Global.localSortByName;
        context.getSharedPreferences("Settings", 0)
                .edit().putBoolean(context.getString(R.string.key_local_sort),Global.localSortByName)
                .apply();
    }

    public static String getDefaultFileParent(Context context){
        File f;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            f= context.getExternalFilesDir(null);
        }else{
            f= Environment.getExternalStorageDirectory();
        }
        return f.getAbsolutePath();
    }



    private static void initFilesTree(Context context){
        String path=context.getSharedPreferences("Settings",Context.MODE_PRIVATE).getString(context.getString(R.string.key_save_path),getDefaultFileParent(context));
        assert path != null;
        MAINFOLDER=new File(path,MAINFOLDER_NAME);
        OLD_GALLERYFOLDER=new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),MAINFOLDER_NAME);
        DOWNLOADFOLDER=new File(MAINFOLDER,DOWNLOADFOLDER_NAME);
        SCREENFOLDER =new File(MAINFOLDER,SCREENFOLDER_NAME);
        PDFFOLDER =new File(MAINFOLDER,PDFFOLDER_NAME);
        UPDATEFOLDER =new File(MAINFOLDER,UPDATEFOLDER_NAME);
        ZIPFOLDER =new File(MAINFOLDER,ZIPFOLDER_NAME);
    }


    @Nullable
    public static OkHttpClient getClient(){
        return client;
    }
    @NonNull
    public static OkHttpClient getClient(Context context){
        if(client==null)initHttpClient(context);
        return client;
    }
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

    public static int getGalleryHeight(){
        return galleryHeight;
    }

    public static int getMaxHistory() {
        return maxHistory;
    }

    public static boolean useAlternativeSite() {
        return alternativeSite;
    }

    private static void initTitleType(@NonNull Context context){
        String s=context.getSharedPreferences("Settings", 0).getString(context.getString(R.string.key_title_type),"pretty");
        switch (s){
            case "pretty":titleType= TitleType.PRETTY;break;
            case "english":titleType=  TitleType.ENGLISH;break;
            case "japanese":titleType=  TitleType.JAPANESE;break;
        }
    }
    public static int getDeviceWidth(@Nullable Activity activity){
        getDeviceMetrics(activity);
        return lastDisplay.widthPixels;
    }
    public static int getDeviceHeight(@Nullable Activity activity){
        getDeviceMetrics(activity);
        return lastDisplay.heightPixels;
    }
    private static void getDeviceMetrics(Activity activity){
        if (activity != null)
            activity.getWindowManager().getDefaultDisplay().getMetrics(lastDisplay);
    }
    public static void initFromShared(@NonNull Context context){
        SharedPreferences shared=context.getSharedPreferences("Settings", 0);
        CookieSyncManager.createInstance(context);
        initHttpClient(context);
        initTitleType(context);
        initTheme(context);
        loadNotificationChannel(context);
        NotificationSettings.initializeNotificationManager(context);
        Login.initUseAccountTag(context);
        Global.initStorage(context);
        localSortByName=shared.getBoolean(context.getString(R.string.key_local_sort),true);
        useRtl=shared.getBoolean(context.getString(R.string.key_use_rtl),false);
        keepHistory=shared.getBoolean(context.getString(R.string.key_keep_history),true);
        infiniteScroll=shared.getBoolean(context.getString(R.string.key_infinite_scroll),false);
        removeAvoidedGalleries =shared.getBoolean(context.getString(R.string.key_remove_ignored),true);
        onlyTag=shared.getBoolean(context.getString(R.string.key_ignore_tags),true);
        volumeOverride=shared.getBoolean(context.getString(R.string.key_override_volume),true);
        enableBeta=shared.getBoolean(context.getString(R.string.key_enable_beta),true);
        columnCount=shared.getInt(context.getString(R.string.key_column_count),2);
        showTitles=shared.getBoolean(context.getString(R.string.key_show_titles),true);
        lockScreen=shared.getBoolean(context.getString(R.string.key_disable_lock),false);
        maxId=shared.getInt(context.getString(R.string.key_max_id),300000);
        maxHistory=shared.getInt(context.getString(R.string.key_max_history_size),2);
        defaultZoom=shared.getInt(context.getString(R.string.key_default_zoom),100);
        colPortMain=shared.getInt(context.getString(R.string.key_column_port_main),2);
        colLandMain=shared.getInt(context.getString(R.string.key_column_land_main),4);
        colPortDownload=shared.getInt(context.getString(R.string.key_column_port_down),2);
        colLandDownload=shared.getInt(context.getString(R.string.key_column_land_down),4);
        colPortFavorite=shared.getInt(context.getString(R.string.key_column_port_favo),2);
        colLandFavorite=shared.getInt(context.getString(R.string.key_column_land_favo),4);
        colPortHist=shared.getInt(context.getString(R.string.key_column_port_hist),2);
        colLandHist=shared.getInt(context.getString(R.string.key_column_land_hist),4);
        colPortStat=shared.getInt(context.getString(R.string.key_column_port_stat),2);
        colLandStat=shared.getInt(context.getString(R.string.key_column_land_stat),4);
        zoomOneColumn=shared.getBoolean(context.getString(R.string.key_zoom_one_column),false);
        alternativeSite=shared.getBoolean(context.getString(R.string.key_alternative_site),false);
        int x=Math.max(0,shared.getInt(context.getString(R.string.key_only_language),Language.ALL.ordinal()));
        sortType = SortType.values()[shared.getInt(context.getString(R.string.key_by_popular),SortType.RECENT_ALL_TIME.ordinal())];
        usageMobile =DataUsageType.values()[shared.getInt(context.getString(R.string.key_mobile_usage),DataUsageType.FULL.ordinal())];
        usageWifi =DataUsageType.values()[shared.getInt(context.getString(R.string.key_wifi_usage),DataUsageType.FULL.ordinal())];
        if(Language.values()[x]==Language.UNKNOWN){
            updateOnlyLanguage(context,Language.ALL);
            x=Language.ALL.ordinal();
        }
        onlyLanguage=Language.values()[x];

    }
    public static DataUsageType getDownloadPolicy(){
        switch (NetworkUtil.getType()){
            case WIFI:return usageWifi;
            case CELLULAR:return usageMobile;
        }
        return usageWifi;
    }
    public static boolean volumeOverride() {
        return volumeOverride;
    }

    public static boolean isZoomOneColumn() {
        return zoomOneColumn;
    }
    public static void reloadHttpClient(@NonNull Context context){
        OkHttpClient.Builder builder=new OkHttpClient.Builder()
                .cookieJar(
                        new PersistentCookieJar(
                                new SetCookieCache(),
                                new SharedPrefsCookiePersistor(context.getSharedPreferences("Login",0))
                        )
                );
        CustomSSLSocketFactory.enableTls12OnPreLollipop(builder);
        builder.addInterceptor(new CustomInterceptor());
        client=builder.build();
        client.dispatcher().setMaxRequests(25);
        client.dispatcher().setMaxRequestsPerHost(25);
        for(Cookie cookie:client.cookieJar().loadForRequest(HttpUrl.get(Utility.getBaseUrl()))){
            LogUtility.d("Cookie: "+cookie);
        }
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
    private static void initHttpClient(@NonNull Context context){
        if(client!=null)return;
        reloadHttpClient(context);
    }
    public static Locale initLanguage(Context context){
        String x=context.getSharedPreferences("Settings",0).getString(context.getString(R.string.key_language),"en");
        assert x != null;
        return new Locale(x);
    }
    private static ThemeScheme initTheme(Context context){
        String h=context.getSharedPreferences("Settings",0).getString(context.getString(R.string.key_theme_select),"dark");
        assert h != null;
        return theme=h.equals("light")?ThemeScheme.LIGHT:ThemeScheme.DARK;
    }

    public static boolean shouldCheckForUpdates(Context context){
        return context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_check_update),true);
    }
    public static int getLogo(){
        return theme==ThemeScheme.LIGHT?R.drawable.ic_logo_dark:R.drawable.ic_logo;
    }
    public static Drawable getLogo(Resources resources){
        return ResourcesCompat.getDrawable(resources,getLogo(),null);
    }

    public static float getDefaultZoom() {
        return ((float)defaultZoom)/100f;
    }

    public static TitleType getTitleType() {
        return titleType;
    }

    public static ThemeScheme getTheme() {
        return theme;
    }
    public static boolean removeAvoidedGalleries(){return removeAvoidedGalleries;}
    @NonNull public static Language getOnlyLanguage() {
        return onlyLanguage;
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

    public static SortType getSortType() {
        return sortType;
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

    public static void initStorage(Context context){
        if(!Global.hasStoragePermission(context))return;
        Global.initFilesTree(context);
        boolean[] bools=new boolean[]{
                Global.MAINFOLDER.mkdirs(),
                Global.DOWNLOADFOLDER.mkdir(),
                Global.PDFFOLDER.mkdir(),
                Global.UPDATEFOLDER.mkdir(),
                Global.SCREENFOLDER.mkdir(),
                Global.ZIPFOLDER.mkdir(),
        };
        LogUtility.d(
                "0:"+context.getFilesDir()+'\n'+
                "1:"+Global.MAINFOLDER+bools[0]+'\n'+
                "2:"+Global.DOWNLOADFOLDER+bools[1]+'\n'+
                "3:"+Global.PDFFOLDER+bools[2]+'\n'+
                "4:"+Global.UPDATEFOLDER+bools[3]+'\n'+
                "5:"+Global.SCREENFOLDER+bools[4]+'\n'+
                "5:"+Global.ZIPFOLDER+bools[5]+'\n'
        );

        try {
            new File(Global.MAINFOLDER,".nomedia").createNewFile();
        } catch (IOException e) {e.printStackTrace();}
    }


    public static void updateOnlyLanguage(@NonNull Context context, @Nullable Language type){ context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_only_language)),type.ordinal()).apply();onlyLanguage=type; }
    public static void updateSortType(@NonNull Context context, @NonNull SortType sortType){
        context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_by_popular)),sortType.ordinal()).apply();
        Global.sortType =sortType;
    }
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
    public static void shareURL(Context context,String title, String url){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,title+": "+url);
        sendIntent.setType("text/plain");
        Intent clipboardIntent = new Intent(context, CopyToClipboardActivity.class);
        clipboardIntent.setData(Uri.parse(url));
        Intent chooserIntent = Intent.createChooser(sendIntent,context.getString(R.string.share_with));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { clipboardIntent });
        context.startActivity(chooserIntent);
    }
    public static void shareGallery(Context context, GenericGallery gallery) {
        shareURL(context, gallery.getTitle(),"https://"+ Utility.getHost()+"/g/"+gallery.getId());
    }

    public static void setTint(Drawable drawable){
        if(drawable==null)return;
        DrawableCompat.setTint(drawable,theme== ThemeScheme.LIGHT?Color.BLACK:Color.WHITE);
    }

    private static void loadNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel1 = new NotificationChannel(CHANNEL_ID1, context.getString(R.string.channel1_name), NotificationManager.IMPORTANCE_DEFAULT);
            NotificationChannel channel2 = new NotificationChannel(CHANNEL_ID2, context.getString(R.string.channel2_name), NotificationManager.IMPORTANCE_DEFAULT);
            NotificationChannel channel3 = new NotificationChannel(CHANNEL_ID3, context.getString(R.string.channel3_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel1.setDescription(context.getString(R.string.channel1_description));
            channel2.setDescription(context.getString(R.string.channel2_description));
            channel3.setDescription(context.getString(R.string.channel3_description));
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel1);
                notificationManager.createNotificationChannel(channel2);
                notificationManager.createNotificationChannel(channel3);
            }
        }
    }

    public static List<File>getUsableFolders(Context context){
        List<File>strings=new ArrayList<>();
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q)
            strings.add(Environment.getExternalStorageDirectory());

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return strings;

        File[]files=context.getExternalFilesDirs(null);
        strings.addAll(Arrays.asList(files));
        return strings;
    }

    public static boolean hasStoragePermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isJPEGCorrupted(String path){
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
    private static File findGalleryFolder(File directory,int id){
        if(directory==null||!directory.exists()||!directory.isDirectory())return null;
        String fileName="."+id;
        File[] tmp=directory.listFiles();
        if(tmp==null)return null;
        for (File tmp2 : tmp) {
            if (tmp2.isDirectory()&&new File(tmp2,fileName).exists()) {
                return tmp2;
            }
        }
        return null;
    }
    @Nullable
    public static File findGalleryFolder(Context context, int id){
        for(File dir:getUsableFolders(context)){
            dir=new File(dir,MAINFOLDER_NAME);
            dir=new File(dir,DOWNLOADFOLDER_NAME);
            File f=findGalleryFolder(dir,id);
            if(f!=null)return f;
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
        }
    }

    public static void recursiveDelete(File file){
        if(file==null||!file.exists())return;
        if(file.isDirectory()){
            File[] files = file.listFiles();
            if(files==null)return;
            for(File x:files)recursiveDelete(x);
        }
        file.delete();
    }

    @NonNull
    public static String getVersionName(Context context){
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "0.0.0";
    }


}
