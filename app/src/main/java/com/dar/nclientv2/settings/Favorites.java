package com.dar.nclientv2.settings;

import android.util.Log;

import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.async.database.Queries;

import static com.dar.nclientv2.settings.Global.LOGTAG;

public class Favorites{
    public static final int MAXFAVORITE=10000;
    private static int totalFavorite;




    public static boolean addFavorite(Gallery gallery){
        if(totalFavorite>=MAXFAVORITE)return false;
        Queries.GalleryTable.addFavorite(Database.getDatabase(),gallery,false);
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
