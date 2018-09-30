package com.dar.nclientv2.settings;

import android.content.Context;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.loginapi.User;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class Login{
    private static User user;
    private static List<Tag> onlineTags=new ArrayList<>();
    private static boolean accountTag;

    public static void  initUseAccountTag(@NonNull Context context){accountTag=context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_use_account_tag),false);}

    public static boolean useAccountTag(){
        return accountTag;
    }

    public static void logout(Context context){
        context.getSharedPreferences("OnlineFavorite",0).edit().clear().apply();
    }
    public static List<Tag> getOnlineTags() {
        return onlineTags;
    }
    public static void clearOnlineTags(){onlineTags.clear();}
    public static void addOnlineTag(Tag tag){onlineTags.add(tag);}
    public static void removeOnlineTag(Tag tag){onlineTags.remove(tag);}

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

    public static void saveOnlineFavorite(@NonNull Context context, Gallery gallery){
        try {
            context.getSharedPreferences("OnlineFavorite",0).edit().putString(""+gallery.getId(),gallery.writeGallery()).apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void removeOnlineFavorite(@NonNull Context context,int id){
        context.getSharedPreferences("OnlineFavorite",0).edit().remove(""+id).apply();
    }
    @Nullable
    public static Gallery getOnlineFavorite(@NonNull Context context, int id){
        String s=context.getSharedPreferences("OnlineFavorite",0).getString(""+id,null);
        if(s==null)return null;
        try {
            return new Gallery(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
