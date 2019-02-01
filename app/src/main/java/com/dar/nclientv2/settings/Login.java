package com.dar.nclientv2.settings;

import android.content.Context;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.loginapi.User;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class Login{
    private static User user;
    private static boolean accountTag;

    public static void  initUseAccountTag(@NonNull Context context){accountTag=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_use_account_tag),false);}

    public static boolean useAccountTag(){
        return accountTag;
    }

    public static void logout(){
        Queries.GalleryTable.removeAllFavorite(Database.getDatabase(),true);
    }
    public static Tag[] getOnlineTags() {
        return Queries.TagTable.getAllOnlineFavorite(Database.getDatabase());
    }
    public static void clearOnlineTags(){
        Queries.TagTable.resetOnlineFavorite(Database.getDatabase());
    }
    public static void addOnlineTag(Tag tag){
        Queries.TagTable.insert(Database.getDatabase(),tag);
        Queries.TagTable.updateOnlineFavorite(Database.getDatabase(),tag,true);
    }
    public static void removeOnlineTag(Tag tag){
        Queries.TagTable.updateOnlineFavorite(Database.getDatabase(),tag,false);
    }

    public static boolean isLogged(){
        if(Global.client==null)return false;
        PersistentCookieJar p=((PersistentCookieJar)Global.client.cookieJar());
        for(Cookie c:p.loadForRequest(HttpUrl.get("https://nhentai.net/"))){
            if(c.name().equals("sessionid"))return true;
        }
        return false;

    }



    public static User getUser() {
        return user;
    }

    public static void updateUser(User user) {
        Login.user = user;
    }

    public static void saveOnlineFavorite(Gallery gallery){
        Queries.GalleryTable.addFavorite(Database.getDatabase(),gallery,true);
    }
    public static void removeOnlineFavorite(Gallery gallery){
        Queries.GalleryTable.removeFavorite(Database.getDatabase(),gallery,true);
    }
    public static boolean isOnlineFavorite(int id){
        return Queries.GalleryTable.isFavorite(Queries.GalleryTable.isFavorite(Database.getDatabase(),id),true);
    }
    @Nullable
    public static Gallery getOnlineFavorite(int id){
        try{
            return Queries.GalleryTable.galleryFromId(Database.getDatabase(),id);
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }


    public static boolean isOnlineTags(Tag tag){
        return Queries.TagTable.isOnlineFavorite(Database.getDatabase(),tag);
    }
}
