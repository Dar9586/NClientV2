package com.dar.nclientv2.settings;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

import com.dar.nclientv2.CopyToClipboardActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.loginapi.LoadTags;
import com.dar.nclientv2.loginapi.User;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import okhttp3.OkHttpClient;

public final class Global {
    public enum ThemeScheme{LIGHT,DARK,BLACK}

    public static OkHttpClient client=null;
    public static final File GALLERYFOLDER=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"NClientV2");
    public static final File DOWNLOADFOLDER=new File(Environment.getExternalStorageDirectory(),"NClientV2");
    public static final String LOGTAG="NCLIENTLOG";
    public static final String CHANNEL_ID1="download_gallery",CHANNEL_ID2="create_pdf";
    private static TitleType titleType=TitleType.PRETTY;
    private static Language onlyLanguage=null;
    private static boolean byPopular,loadImages,hideFromGallery,highRes,onlyTag,infiniteScroll,removeIgnoredGalleries;
    private static ThemeScheme theme;
    private static int notificationId,columnCount,maxId,imageQuality;

    public static void     initTitleType    (@NonNull Context context){titleType=TitleType.values()[context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_title_type),1)];}
    public static void     initByPopular    (@NonNull Context context){byPopular=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_by_popular),false);}
    public static void     initInfiniteScroll    (@NonNull Context context){infiniteScroll=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_infinite_scroll),false);}
    public static void  initHideFromGallery    (@NonNull Context context){hideFromGallery=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_hide_saved_images),false);}
    public static void     initHighRes    (@NonNull Context context){highRes=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_high_res_gallery),true);}
    public static void     initRemoveIgnoredGalleries    (@NonNull Context context){removeIgnoredGalleries=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_remove_ignored),true);}
    public static void     initOnlyTag    (@NonNull Context context){onlyTag=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_ignore_tags),true);}
    public static boolean  initLoadImages   (@NonNull Context context){loadImages=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_load_images),true);return loadImages;}
    public static void     initOnlyLanguage (@NonNull Context context){int x=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_only_language),-1);onlyLanguage=x==-1?null:Language.values()[x];}
    public static void     initColumnCount  (@NonNull Context context){columnCount=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_column_count),1);}
    public static int      initImageQuality (@NonNull Context context){imageQuality=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_image_quality),90);return imageQuality;}
    public static void     initMaxId        (@NonNull Context context){maxId=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_max_id),236000);}

    public static void initHttpClient(@NonNull Context context){
        if(client!=null)return;
        client=new OkHttpClient.Builder()
                .cookieJar(new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context.getSharedPreferences("Login",0))))
                .build();
        client.dispatcher().setMaxRequests(25);
        client.dispatcher().setMaxRequestsPerHost(25);
        if(Login.isLogged()&&Login.getUser()==null){
            User.createUser(new User.CreateUser() {
                @Override
                public void onCreateUser(User user) {
                    if(user!=null)new LoadTags(null).start();
                }
            });
        }
    }

    private static ThemeScheme initTheme(Context context){
        String h=context.getSharedPreferences("Settings",0).getString(context.getString(R.string.key_theme_select),"light");
        return theme=h.equals("light")?ThemeScheme.LIGHT:h.equals("dark")?ThemeScheme.DARK:ThemeScheme.BLACK;
    }


    private static int getLogo(){ return theme==ThemeScheme.LIGHT?R.drawable.ic_logo_dark:R.drawable.ic_logo; }
    public static TitleType getTitleType() {
        return titleType;
    }
    public static int getNotificationId() {
        return notificationId++;
    }
    public static ThemeScheme getTheme() {
        return theme;
    }
    public static int getImageQuality() {
        return imageQuality;
    }
    public static boolean getRemoveIgnoredGalleries(){return removeIgnoredGalleries;}
    @Nullable public static Language getOnlyLanguage() {
        return onlyLanguage;
    }
    public static boolean isHideFromGallery() {
        return hideFromGallery;
    }
    public static boolean isHighRes() {
        return highRes;
    }
    public static boolean isOnlyTag() {
        return onlyTag;
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

    public static void addToGallery(Context context, File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    public static void saveNoMedia(Context context) {
        if(!hasStoragePermission(context))return;
        Global.initHideFromGallery(context);
        GALLERYFOLDER.mkdirs();
        try {
            File x=new File(GALLERYFOLDER, ".nomedia");
            if(hideFromGallery) x.createNewFile();
            else if(x.exists())x.delete();
        }catch (IOException e){
            Log.e(LOGTAG,e.getLocalizedMessage(),e);
        }
    }

    public static void updateTitleType(@NonNull Context context, TitleType type){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_title_type)),type.ordinal()).apply();titleType=type; }
    public static void updateOnlyLanguage(@NonNull Context context, @Nullable Language type){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_only_language)),type==null?-1:type.ordinal()).apply();onlyLanguage=type; }
    public static boolean  updateByPopular(@NonNull Context context,boolean popular){context.getSharedPreferences("Settings", 0).edit().putBoolean(context.getString((R.string.key_by_popular)),popular).apply();byPopular=popular; return byPopular;}
    public static boolean  updateLoadImages(@NonNull Context context,boolean load){context.getSharedPreferences("Settings", 0).edit().putBoolean(context.getString((R.string.key_load_images)),load).apply();loadImages=load; return loadImages;}
    public static void updateColumnCount(@NonNull Context context, int count){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_column_count)),count).apply();columnCount=count; }
    public static void updateImageQuality(@NonNull Context context, int quality){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_image_quality)),quality).apply();imageQuality=quality; }
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
        DrawableCompat.setTint(drawable,theme== ThemeScheme.LIGHT?Color.BLACK:Color.WHITE);
    }

    public static void loadNotificationChannel(@NonNull Context context){
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
    public static void preloadImage(String url){
        if(!isLoadImages())return;
        //Glide.with(context).load(url).preload();
        Picasso.get().load(url).fetch();
    }

    public static void loadImage(String url, final ImageView imageView){loadImage(url,imageView,false);}
    public static void loadImage(String url, final ImageView imageView,boolean force){
        if(loadImages||force)Picasso.get().load(url).placeholder(getLogo()).into(imageView);
        else Picasso.get().load(getLogo()).placeholder(getLogo()).into(imageView);
    }
    public static void loadImage(File file, ImageView imageView){
        Picasso.get().load(file).placeholder(getLogo()).into(imageView);
    }
    public static void loadImage(@DrawableRes int drawable, ImageView imageView){
        Picasso.get().load(drawable).into(imageView);
        //GlideApp.with(context).load(drawable).into(imageView);
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

    public static void loadTheme(Context context){
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



}
