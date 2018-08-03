package com.dar.nclientv2.settings;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.widget.ImageView;

import com.dar.nclientv2.CopyToClipboardActivity;
import com.dar.nclientv2.FavoriteActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.adapters.FavoriteAdapter;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.async.LoadFavorite;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public final class Global {
    public enum ThemeScheme{LIGHT,DARK,BLACK}
    private static User user;
    public static OkHttpClient client=null;
    public static final File GALLERYFOLDER=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"NClientV2");
    public static final File DOWNLOADFOLDER=new File(Environment.getExternalStorageDirectory(),"NClientV2");
    public static final String LOGTAG="NCLIENTLOG";
    public static final String CHANNEL_ID1="download_gallery",CHANNEL_ID2="create_pdf";
    private static TitleType titleType=TitleType.PRETTY;
    private static Language onlyLanguage=null;
    private static boolean byPopular,loadImages,tagOrder,hideFromGallery,highRes,onlyTag,accountTag,infiniteScroll;
    private static List<Tag> accepted=new ArrayList<>(),avoided=new ArrayList<>();
    private static ThemeScheme theme;
    private static int notificationId,columnCount,minTagCount,maxId,totalFavorite,imageQuality;
    public static final int MAXFAVORITE=10000;
    public static final int MAXTAGS=100;
    private static final List<Tag>[] sets= new List[5];
    private static List<Tag>onlineTags=new ArrayList<>();
    public static void logout(Context context){
        context.getSharedPreferences("OnlineFavorite",0).edit().clear().apply();
    }
    public static List<Tag> getOnlineTags() {
        return onlineTags;
    }
    public static void clearOnlineTags(){onlineTags.clear();}
    public static void addOnlineTag(Tag tag){onlineTags.add(tag);}
    public static void removeOnlineTag(Tag tag){onlineTags.remove(tag);}

    public static void initTagSets(@NonNull Context context){
        boolean already=true;
        for(int a=0;a<5;a++)if(sets[a]==null){already=false;break;}
        if(already)return;
        sets[0]=getSet(context,getScraperId(TagType.TAG));
        sets[1]=getSet(context,getScraperId(TagType.ARTIST));
        sets[2]=getSet(context,getScraperId(TagType.GROUP));
        sets[3]=getSet(context,getScraperId(TagType.PARODY));
        sets[4]=getSet(context,getScraperId(TagType.CHARACTER));
    }

    public static User getUser() {
        return user;
    }

    public static void updateUser(User user) {
        Global.user = user;
    }
    public static void saveOnlineFavorite(@NonNull Context context,Gallery gallery){
        try {
            context.getSharedPreferences("OnlineFavorite",0).edit().putString(""+gallery.getId(),gallery.writeGallery()).apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void removeOnlineFavorite(@NonNull Context context,int id){
        context.getSharedPreferences("OnlineFavorite",0).edit().remove(""+id).apply();
    }
    @Nullable public static Gallery getOnlineFavorite(@NonNull Context context,int id){
        String s=context.getSharedPreferences("OnlineFavorite",0).getString(""+id,null);
        if(s==null)return null;
        try {
            return new Gallery(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void initHttpClient(@NonNull Context context){
        if(client!=null)return;

        client=new OkHttpClient.Builder()
                .cookieJar(new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context.getSharedPreferences("Login",0))))
                .build();
        client.dispatcher().setMaxRequests(25);
        client.dispatcher().setMaxRequestsPerHost(25);
        if(isLogged()&&user==null){
            User.createUser(new User.CreateUser() {
                @Override
                public void onCreateUser(User user) {
                    if(user!=null)new LoadTags(null).start();
                }
            });

        }
    }
    public static boolean isLogged(){
        if(client==null)return false;
        PersistentCookieJar p=((PersistentCookieJar)client.cookieJar());
        for(Cookie c:p.loadForRequest(HttpUrl.get("https://nhentai.net/"))){
            if(c.name().equals("sessionid"))return true;
        }
        return false;

    }
    public static void  initUseAccountTag(@NonNull Context context){accountTag=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_use_account_tag),false);}
    public static void     initTitleType    (@NonNull Context context){titleType=TitleType.values()[context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_title_type),1)];}
    public static void     initByPopular    (@NonNull Context context){byPopular=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_by_popular),false);}
    public static void     initInfiniteScroll    (@NonNull Context context){infiniteScroll=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_infinite_scroll),false);}
    public static void  initHideFromGallery    (@NonNull Context context){hideFromGallery=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_hide_saved_images),false);}
    public static void     initHighRes    (@NonNull Context context){highRes=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_high_res_gallery),true);}
    public static void     initOnlyTag    (@NonNull Context context){onlyTag=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_ignore_tags),true);}
    public static void     initTagOrder     (@NonNull Context context){tagOrder=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_tag_order),true);}
    public static boolean  initLoadImages   (@NonNull Context context){loadImages=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_load_images),true);return loadImages;}
    public static void     initOnlyLanguage (@NonNull Context context){int x=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_only_language),-1);onlyLanguage=x==-1?null:Language.values()[x];}
    public static void     initColumnCount  (@NonNull Context context){columnCount=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_column_count),1);}
    public static int      initImageQuality (@NonNull Context context){imageQuality=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_image_quality),90);return imageQuality;}
    public static void     initMinTagCount  (@NonNull Context context){minTagCount=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_min_tag_count),25);}
    public static void     initMaxId        (@NonNull Context context){maxId=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_max_id),236000);}
    private static ThemeScheme initTheme(Context context){
        String h=context.getSharedPreferences("Settings",0).getString(context.getString(R.string.key_theme_select),"light");
        return theme=h.equals("light")?ThemeScheme.LIGHT:h.equals("dark")?ThemeScheme.DARK:ThemeScheme.BLACK;
    }
    public static void  initTagPreferencesSets(@NonNull Context context){
        if(accepted.size()!=0&&avoided.size()!=0)return;
        SharedPreferences preferences=context.getSharedPreferences("TagPreferences", 0);
        accepted=Tag.toArrayList(preferences.getStringSet(context.getString(R.string.key_accepted_tags),new HashSet<String>()));
        avoided=Tag.toArrayList(preferences.getStringSet(context.getString(R.string.key_avoided_tags),new HashSet<String>()));
        Log.i(LOGTAG,"Accepted"+accepted.toString());
        Log.i(LOGTAG,"Avoided"+avoided.toString());
    }

    public static int getMinTagCount() {
        return minTagCount;
    }

    public static TitleType getTitleType() {
        return titleType;
    }

    public static int getImageQuality() {
        return imageQuality;
    }

    @Nullable
    public static Language getOnlyLanguage() {
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

    public static boolean isTagOrderByPopular() {
        return tagOrder;
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
    public static TagStatus getStatus(Tag tag){
        if(accepted.contains(tag))return TagStatus.ACCEPTED;
        if(avoided.contains(tag))return TagStatus.AVOIDED;
        return TagStatus.DEFAULT;
    }
    public static String getQueryString(String query){
        StringBuilder builder=new StringBuilder("");
        for (Tag x:accepted)if(!query.contains(x.getName()))builder.append('+').append(x.toQueryTag(TagStatus.ACCEPTED));
        for (Tag x:avoided) if(!query.contains(x.getName()))builder.append('+').append(x.toQueryTag(TagStatus.AVOIDED));
        if(accountTag)for(Tag x:onlineTags)if(!accepted.contains(x)&&!avoided.contains(x)&&!query.contains(x.getName()))builder.append('+').append(x.toQueryTag(TagStatus.AVOIDED));
        return builder.toString();
    }
    public static void resetAllStatus(@NonNull Context context){
        context.getSharedPreferences("TagPreferences",0).edit().clear().apply();
        avoided.clear();accepted.clear();
    }
    public static TagStatus updateStatus(@NonNull Context context,Tag tag){
        if(accepted.contains(tag)){
            accepted.remove(tag);
            avoided.add(tag);
            return updateSharedTagPreferences(context)?TagStatus.AVOIDED:TagStatus.ACCEPTED;
        }
        if(avoided.contains(tag)){
            avoided.remove(tag);
            return updateSharedTagPreferences(context)?TagStatus.DEFAULT:TagStatus.AVOIDED;
        }
        if(maxTagReached())return TagStatus.DEFAULT;
        accepted.add(tag);
        return updateSharedTagPreferences(context)?TagStatus.ACCEPTED:TagStatus.DEFAULT;
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
    private static boolean updateSharedTagPreferences(Context context){
        Log.i(LOGTAG,context.getSharedPreferences("TagPreferences",0).getAll().toString());
        boolean x=context.getSharedPreferences("TagPreferences",0).edit().clear()
                .putStringSet(context.getString(R.string.key_accepted_tags),Tag.toStringSet(accepted))
                .putStringSet(context.getString(R.string.key_avoided_tags),Tag.toStringSet(avoided)).commit();
        Log.i(LOGTAG,context.getSharedPreferences("TagPreferences",0).getAll().toString());
        return x;

    }
    public static void updateTitleType(@NonNull Context context, TitleType type){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_title_type)),type.ordinal()).apply();titleType=type;
    }
    public static void updateOnlyLanguage(@NonNull Context context, @Nullable Language type){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_only_language)),type==null?-1:type.ordinal()).apply();onlyLanguage=type;
    }
    public static boolean  updateByPopular(@NonNull Context context,boolean popular){context.getSharedPreferences("Settings", 0).edit().putBoolean(context.getString((R.string.key_by_popular)),popular).apply();byPopular=popular; return byPopular;}
    public static boolean  updateTagOrder(@NonNull Context context,boolean order){context.getSharedPreferences("Settings", 0).edit().putBoolean(context.getString((R.string.key_tag_order)),order).apply();tagOrder=order; return tagOrder;}
    public static boolean  updateLoadImages(@NonNull Context context,boolean load){context.getSharedPreferences("Settings", 0).edit().putBoolean(context.getString((R.string.key_load_images)),load).apply();loadImages=load; return loadImages;}
    public static void updateColumnCount(@NonNull Context context, int count){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_column_count)),count).apply();columnCount=count; }
    public static void updateImageQuality(@NonNull Context context, int quality){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_image_quality)),quality).apply();imageQuality=quality; }
    public static void updateMaxId(@NonNull Context context, int id){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_max_id)),id).apply();maxId=id; }
    public static void updateMinTagCount(@NonNull Context context, int count){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_min_tag_count)),count).apply();minTagCount=count; }

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
    public static ThemeScheme getTheme() {
        return theme;
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
    private static int getLogo(){
        return theme==ThemeScheme.LIGHT?R.drawable.ic_logo_dark:R.drawable.ic_logo;
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
    public static List<Tag>getListPrefer(){
        List<Tag>x=new ArrayList<>(accepted.size()+avoided.size());
        Log.i(LOGTAG,"Accepted"+accepted.toString());
        Log.i(LOGTAG,"Avoided"+avoided.toString());
        x.addAll(accepted);
        x.addAll(avoided);
        Collections.sort(x, new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return x;
    }

    public static int getNotificationId() {
        return notificationId++;
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
    @NonNull
    private static List<Tag> getSet(@NonNull Context context, @StringRes int res){
        Set<String>x= context.getSharedPreferences("ScrapedTags", 0).getStringSet(context.getString(res),new HashSet<String>());
        List<Tag>tags=new ArrayList<>(x.size());
        for(String y:x){
            tags.add(new Tag(y));
        }
        Collections.sort(tags, new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return tags;
    }
    public static List<Tag>getTagSet(TagType type){
        switch (type){
            case TAG:return sets[0];
            case ARTIST:return sets[1];
            case GROUP:return sets[2];
            case PARODY:return sets[3];
            case CHARACTER:return sets[4];
        }
        return null;
    }
    private static int getOrdinalFromTag(TagType type){
        switch (type){
            case TAG:return 0;
            case ARTIST:return 1;
            case GROUP:return 2;
            case PARODY:return 3;
            case CHARACTER:return 4;
        }
        return -1;
    }
    public static void updateSet(@NonNull Context context, List<Tag>tags , TagType type,boolean mustWrite){
        if(mustWrite) {
            Set<String> x = new HashSet<>(tags.size());
            for (Tag y : tags) {
                x.add(y.toScrapedString());
            }
            if (!context.getSharedPreferences("ScrapedTags", 0).edit().putStringSet(context.getString(getScraperId(type)), x).commit()) {
                Log.e(LOGTAG, "Error to write set: " + type);
            }else sets[getOrdinalFromTag(type)]=tags;
        }else sets[getOrdinalFromTag(type)]=tags;
    }
    private static int getScraperId(TagType tag){
        switch (tag){
            case TAG:return R.string.key_scraped_tags;
            case ARTIST:return R.string.key_scraped_artists;
            case GROUP:return R.string.key_scraped_group;
            case PARODY:return R.string.key_scraped_parodies;
            case CHARACTER:return R.string.key_scraped_characters;
        }
        return -1;
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
    public static void loadFavorites(FavoriteActivity context,FavoriteAdapter adapter){
        new LoadFavorite(adapter).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,context);
    }
    public static boolean addFavorite(Context context,Gallery gallery){
        if(totalFavorite>=MAXFAVORITE)return false;
        Set<String> x=context.getSharedPreferences("FavoriteList", 0).getStringSet(context.getString(R.string.key_favorite_list),new HashSet<String>());
        try {
            x.add(gallery.writeGallery());
        }catch (IOException e){
            Log.e(LOGTAG,e.getLocalizedMessage(),e);
        }
        if(context.getSharedPreferences("FavoriteList", 0).edit().clear().putStringSet(context.getString(R.string.key_favorite_list),x).commit()) {
            totalFavorite++;
            return true;
        }
        return false;
    }
    public static boolean removeFavorite(Context context,GenericGallery gallery){
        Log.i(LOGTAG,"Called remove");
        try {
            Set<String> x = context.getSharedPreferences("FavoriteList", 0).getStringSet(context.getString(R.string.key_favorite_list), new HashSet<String>());
            for (String y : x) {
                try {
                    if (Integer.parseInt(y.substring(1, y.indexOf(','))) == gallery.getId())
                        x.remove(y);
                } catch (NumberFormatException e) {
                    Log.e(LOGTAG, e.getLocalizedMessage(), e);
                }
            }
            if(context.getSharedPreferences("FavoriteList", 0).edit().clear().putStringSet(context.getString(R.string.key_favorite_list),x).commit()) {
                totalFavorite--;
                return true;
            }
        }catch (ConcurrentModificationException e){
            Log.e(LOGTAG,e.getLocalizedMessage(),e);
        }
        return false;
    }
    public static boolean isFavorite(Context context,GenericGallery gallery){
        if(gallery==null)return false;
        Set<String> x=context.getSharedPreferences("FavoriteList", 0).getStringSet(context.getString(R.string.key_favorite_list),new HashSet<String>());
        for(String y:x){
            try{
                if(Integer.parseInt(y.substring(1,y.indexOf(',')))==gallery.getId())return true;
            }catch (NumberFormatException e){
                Log.e(LOGTAG,e.getLocalizedMessage(),e);
            }
        }
        return false;
    }
    public static void countFavorite(Context context){
        totalFavorite=context.getSharedPreferences("FavoriteList", 0).getStringSet(context.getString(R.string.key_favorite_list),new HashSet<String>()).size();

    }
    public static boolean maxTagReached(){
        return accepted.size()+avoided.size()>=MAXTAGS;
    }

}
