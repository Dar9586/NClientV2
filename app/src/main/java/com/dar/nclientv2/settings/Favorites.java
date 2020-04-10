package com.dar.nclientv2.settings;

import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.utility.LogUtility;

public class Favorites{
    public static final int MAXFAVORITE=10000;
    private static int totalFavorite;




    public static boolean addFavorite(Gallery gallery){
        if(totalFavorite>=MAXFAVORITE)return false;
        Queries.FavoriteTable.addFavorite(gallery);
        return true;
    }

    public static boolean removeFavorite(GenericGallery gallery){
        LogUtility.i("Called remove");
        Queries.FavoriteTable.removeFavorite(gallery.getId());
        return true;
    }

    public static boolean isFavorite(GenericGallery gallery){
        if(gallery==null||!gallery.isValid())return false;
        return Queries.FavoriteTable.isFavorite(gallery.getId());
    }

    public static void countFavorite(){
        totalFavorite=Queries.FavoriteTable.countFavorite();
    }
}
