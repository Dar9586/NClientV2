package com.dar.nclientv3.settings;

import com.dar.nclientv3.api.components.Gallery;
import com.dar.nclientv3.api.components.GenericGallery;
import com.dar.nclientv3.async.database.Queries;

public class Favorites {


    public static boolean addFavorite(Gallery gallery) {
        Queries.FavoriteTable.addFavorite(gallery);
        return true;
    }

    public static boolean removeFavorite(GenericGallery gallery) {
        Queries.FavoriteTable.removeFavorite(gallery.getId());
        return true;
    }

    public static boolean isFavorite(GenericGallery gallery) {
        if (gallery == null || !gallery.isValid()) return false;
        return Queries.FavoriteTable.isFavorite(gallery.getId());
    }


}
