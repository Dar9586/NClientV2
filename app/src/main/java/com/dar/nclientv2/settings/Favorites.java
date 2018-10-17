package com.dar.nclientv2.settings;

import android.os.AsyncTask;
import android.util.Log;

import com.dar.nclientv2.FavoriteActivity;
import com.dar.nclientv2.adapters.FavoriteAdapter;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.async.LoadFavorite;
import com.dar.nclientv2.async.database.Queries;

import java.io.IOException;

import static com.dar.nclientv2.settings.Global.LOGTAG;

public class Favorites{
    public static final int MAXFAVORITE=10000;
    private static int totalFavorite;

    public static void loadFavorites(FavoriteActivity context, FavoriteAdapter adapter){
        new LoadFavorite(adapter).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,context);
    }


    public static boolean addFavorite(Gallery gallery){
        if(totalFavorite>=MAXFAVORITE)return false;
        try{
            Queries.GalleryTable.addFavorite(Database.getDatabase(),gallery,false);
        }catch(IOException e){
            return false;
        }
        return true;
    }

    public static boolean removeFavorite(Gallery gallery){
        Log.i(LOGTAG,"Called remove");
        Queries.GalleryTable.removeFavorite(Database.getDatabase(),gallery,false);
        return false;
    }

    public static boolean isFavorite(Gallery gallery){
        if(gallery==null)return false;
        return Queries.GalleryTable.isFavorite(Queries.GalleryTable.isFavorite(Database.getDatabase(),gallery),false);
    }

    public static void countFavorite(){
        totalFavorite=Queries.GalleryTable.countFavorite(Database.getDatabase());
    }
}
