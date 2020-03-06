package com.dar.nclientv2.settings;

import android.content.Context;

import androidx.annotation.NonNull;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.loginapi.User;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;

import java.util.List;

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

    }
    public static List<Tag> getOnlineTags() {
        return Queries.TagTable.getAllOnlineBlacklisted();
    }
    public static void clearOnlineTags(){
        Queries.TagTable.removeAllBlacklisted(Database.getDatabase());
    }
    public static void addOnlineTag(Tag tag){
        Queries.TagTable.insert(tag);
        Queries.TagTable.updateBlacklistedTag(tag,true);
    }
    public static void removeOnlineTag(Tag tag){
        Queries.TagTable.updateBlacklistedTag(tag,false);
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


    public static boolean isOnlineTags(Tag tag){
        return Queries.TagTable.isBlackListed(tag);
    }
}
