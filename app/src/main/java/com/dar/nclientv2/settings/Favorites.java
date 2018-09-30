package com.dar.nclientv2.settings;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dar.nclientv2.FavoriteActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.adapters.FavoriteAdapter;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.async.LoadFavorite;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;

import static com.dar.nclientv2.settings.Global.LOGTAG;

public class Favorites{
    public static final int MAXFAVORITE=10000;
    private static int totalFavorite;

    public static void loadFavorites(FavoriteActivity context, FavoriteAdapter adapter){
        new LoadFavorite(adapter).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,context);
    }


    public static boolean addFavorite(Context context, Gallery gallery){
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
}
