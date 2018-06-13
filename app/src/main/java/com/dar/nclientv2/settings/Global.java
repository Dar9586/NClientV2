package com.dar.nclientv2.settings;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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

public final class Global {
    public enum ThemeScheme{LIGHT,DARK,BLACK}
    public static final String LOGTAG="NCLIENTLOG";
    public static final String CHANNEL_ID="download_gallery";
    private static TitleType titleType=TitleType.PRETTY;
    private static Language onlyLanguage=null;
    private static boolean byPopular=false,loadImages=true,tagOrder=true;
    private static final ArrayList<Integer>downloadingManga=new ArrayList<>();
    private static List<Tag> accepted=new ArrayList<>(),avoided=new ArrayList<>();
    private static ThemeScheme theme;
    private static int notificationId,columnCount,minTagCount,maxId;
    private static int totalFavorite;
    public static final int MAXFAVORITE=10000;
    public static final int MAXTAGS=100;
    private static List<Tag>[] sets= new List[5];
    public static void initTagSets(@NonNull Context context){
        sets[0]=getSet(context,getScraperId(TagType.TAG));
        sets[1]=getSet(context,getScraperId(TagType.ARTIST));
        sets[2]=getSet(context,getScraperId(TagType.GROUP));
        sets[3]=getSet(context,getScraperId(TagType.PARODY));
        sets[4]=getSet(context,getScraperId(TagType.CHARACTER));
    }
    public static void     initTitleType    (@NonNull Context context){titleType=TitleType.values()[context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_title_type),1)];}
    public static void     initByPopular    (@NonNull Context context){byPopular=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_by_popular),false);}
    public static void     initTagOrder     (@NonNull Context context){tagOrder=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_tag_order),true);}
    public static boolean  initLoadImages   (@NonNull Context context){loadImages=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_load_images),true);return loadImages;}
    public static void     initOnlyLanguage (@NonNull Context context){int x=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_only_language),-1);onlyLanguage=x==-1?null:Language.values()[x];}
    public static void     initColumnCount  (@NonNull Context context){columnCount=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_column_count),1);}
    public static void     initMinTagCount  (@NonNull Context context){minTagCount=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_min_tag_count),25);}
    public static void     initMaxId        (@NonNull Context context){maxId=context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_max_id),236000);}
    private static ThemeScheme initTheme(Context context){
        String h=context.getSharedPreferences("Settings",0).getString(context.getString(R.string.key_theme_select),"light");
        return theme=h.equals("light")?ThemeScheme.LIGHT:h.equals("dark")?ThemeScheme.DARK:ThemeScheme.BLACK;
    }
    public static void  initTagPreferencesSets      (@NonNull Context context){
        accepted=Tag.toArrayList(context.getSharedPreferences("TagPreferences", 0).getStringSet(context.getString(R.string.key_accepted_tags),new HashSet<String>()));
        avoided=Tag.toArrayList(context.getSharedPreferences("TagPreferences", 0).getStringSet(context.getString(R.string.key_avoided_tags),new HashSet<String>()));
    }

    public static int getMinTagCount() {
        return minTagCount;
    }

    public static TitleType getTitleType() {
        return titleType;
    }

    @Nullable
    public static Language getOnlyLanguage() {
        return onlyLanguage;
    }

    public static boolean isByPopular() {
        return byPopular;
    }

    public static boolean isTagOrderByPopular() {
        return tagOrder;
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
    public static String getQueryString(){
        StringBuilder builder=new StringBuilder("");
        for (Tag x:accepted)builder.append('+').append(x.toQueryTag(TagStatus.ACCEPTED));
        for (Tag x:avoided)builder.append('+').append(x.toQueryTag(TagStatus.AVOIDED));
        return builder.toString();
    }
    public static void resetAllStatus(@NonNull Context context){
        context.getSharedPreferences("TagPreferences",0).edit()
                .putStringSet(context.getString(R.string.key_accepted_tags),new HashSet<String>())
                .putStringSet(context.getString(R.string.key_avoided_tags),new HashSet<String>()).apply();
        avoided.clear();accepted.clear();
    }
    public static TagStatus updateStatus(@NonNull Context context,Tag tag){
        if(accepted.contains(tag)){
            accepted.remove(tag);
            context.getSharedPreferences("TagPreferences",0).edit().putStringSet(context.getString(R.string.key_accepted_tags),Tag.toStringSet(accepted)).apply();
            avoided.add(tag);
            context.getSharedPreferences("TagPreferences",0).edit().putStringSet(context.getString(R.string.key_avoided_tags),Tag.toStringSet(avoided)).apply();
            return TagStatus.AVOIDED;
        }
        if(avoided.contains(tag)){
            avoided.remove(tag);
            context.getSharedPreferences("TagPreferences",0).edit().putStringSet(context.getString(R.string.key_avoided_tags),Tag.toStringSet(avoided)).apply();
            return TagStatus.DEFAULT;
        }
        if(maxTagReached())return TagStatus.DEFAULT;
        accepted.add(tag);
        context.getSharedPreferences("TagPreferences",0).edit().putStringSet(context.getString(R.string.key_accepted_tags),Tag.toStringSet(accepted)).apply();
        return TagStatus.ACCEPTED;
    }
    public static void updateTitleType(@NonNull Context context, TitleType type){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_title_type)),type.ordinal()).apply();titleType=type;
    }
    public static void updateOnlyLanguage(@NonNull Context context, @Nullable Language type){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_only_language)),type==null?-1:type.ordinal()).apply();onlyLanguage=type;
    }
    public static boolean  updateByPopular(@NonNull Context context,boolean popular){context.getSharedPreferences("Settings", 0).edit().putBoolean(context.getString((R.string.key_by_popular)),popular).apply();byPopular=popular; return byPopular;}
    public static boolean  updateTagOrder(@NonNull Context context,boolean order){context.getSharedPreferences("Settings", 0).edit().putBoolean(context.getString((R.string.key_tag_order)),order).apply();tagOrder=order; return tagOrder;}
    public static boolean  updateLoadImages(@NonNull Context context,boolean load){context.getSharedPreferences("Settings", 0).edit().putBoolean(context.getString((R.string.key_load_images)),load).apply();loadImages=load; return loadImages;}
    public static void updateColumnCount(@NonNull Context context, int count){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_column_count)),count).apply();columnCount=count; }
    public static void updateMaxId(@NonNull Context context, int id){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_max_id)),id).apply();maxId=id; }
    public static void updateMinTagCount(@NonNull Context context, int count){context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_min_tag_count)),count).apply();minTagCount=count; }

    public static int getNavigationBarHeight(Context context){
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }
    public static void setTint(Drawable drawable){
        DrawableCompat.setTint(drawable,theme== ThemeScheme.LIGHT?Color.BLACK:Color.WHITE);
    }
    public static ThemeScheme getTheme() {
        return theme;
    }

    public static void loadNotificationChannel(@NonNull Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.channel_description));
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null)notificationManager.createNotificationChannel(channel);
        }
    }
    public static void preloadImage(Context context,String url){
        if(!isLoadImages())return;
        //Glide.with(context).load(url).preload();
        Picasso.get().load(url).fetch();
    }
    private static int getLogo(){
        return theme==ThemeScheme.LIGHT?R.drawable.ic_logo_dark:R.drawable.ic_logo;
    }
    public static void loadImage(final Context context, String url, final ImageView imageView){
        if(loadImages)Picasso.get().load(url).placeholder(getLogo()).into(imageView);
        else Picasso.get().load(getLogo()).placeholder(getLogo()).into(imageView);
    }
    public static void loadImage(Context context, File file, ImageView imageView){
        Picasso.get().load(file).placeholder(getLogo()).into(imageView);
    }
    public static void loadImage(Context context, @DrawableRes int drawable, ImageView imageView){
        Picasso.get().load(drawable).into(imageView);
        //GlideApp.with(context).load(drawable).into(imageView);
    }
    public static List<Tag>getListPrefer(){
        List<Tag>x=new ArrayList<>(accepted.size()+avoided.size());
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
    public static void removeFromDownloadList(int id){
        downloadingManga.remove(Integer.valueOf(id));
    }
    public static void addToDownloadList(int id){
        downloadingManga.add(id);
    }
    public static boolean isDownloading(int id){
        for (int x:downloadingManga)if(x==id)return true;
        return false;
    }
    @Nullable
    public static File findGalleryFolder(int id){
        File parent=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/NClientV2/");
        parent.mkdirs();
        File[] tmp=parent.listFiles();
        for (File tmp2 : tmp) {
            if (tmp2.isDirectory() && (tmp2 = new File(tmp2, ".nomedia")).exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(tmp2));
                    String h = br.readLine();
                    br.close();
                    if (h!=null&&h.equals("" + id))return tmp2.getParentFile();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

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
    public static void updateSet(@NonNull Context context, List<Tag>tags , TagType type){
        Set<String>x=new HashSet<>(tags.size());
        for(Tag y:tags){
            x.add(y.toScrapedString());
        }
        context.getSharedPreferences("ScrapedTags", 0).edit().putStringSet(context.getString(getScraperId(type)),x).apply();
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
        context.getSharedPreferences("FavoriteList", 0).edit().putStringSet(context.getString(R.string.key_favorite_list),x).apply();
        totalFavorite++;
        return true;
    }
    public static boolean removeFavorite(Context context,GenericGallery gallery){
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
            context.getSharedPreferences("FavoriteList", 0).edit().putStringSet(context.getString(R.string.key_favorite_list), x).apply();
            totalFavorite--;
            return true;
        }catch (ConcurrentModificationException e){
            Log.e(LOGTAG,e.getLocalizedMessage(),e);
            return false;
        }
    }
    public static boolean isFavorite(Context context,GenericGallery gallery){
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
