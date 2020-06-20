package com.dar.nclientv2.settings;

import android.content.Context;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.loginapi.User;
import com.dar.nclientv2.utility.Utility;

import java.util.Collections;

import okhttp3.HttpUrl;

public class Login{
    private static User user;
    private static boolean accountTag;

    public static void  initUseAccountTag(@NonNull Context context){
        accountTag=context.getSharedPreferences("Settings", Context.MODE_PRIVATE).getBoolean(context.getString(R.string.key_use_account_tag),false);
    }

    public static boolean useAccountTag(){
        return accountTag;
    }

    public static void logout(Context context){
        Global.getClient().cookieJar().saveFromResponse(HttpUrl.get(Utility.getBaseUrl()), Collections.emptyList());
        updateUser(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null);
        }else{
            clearCookies(context,Utility.getBaseUrl());
        }
        clearOnlineTags();

    }
    public static void clearCookies(Context context,String domain) {
        CookieSyncManager.createInstance(context);
        CookieManager cookieManager = CookieManager.getInstance();
        String cookiestring = cookieManager.getCookie(domain);
        String[] cookies =  cookiestring.split(";");
        for (String cookie : cookies) {
            String[] cookieparts = cookie.split("=");
            cookieManager.setCookie(domain, cookieparts[0].trim() + "=; Expires=Tue, 31 Dec 2019 23:59:59 GMT");
        }
        CookieSyncManager.getInstance().sync();
    }

    public static void clearOnlineTags(){
        Queries.TagTable.removeAllBlacklisted();
    }
    public static void addOnlineTag(Tag tag){
        Queries.TagTable.insert(tag);
        Queries.TagTable.updateBlacklistedTag(tag,true);
    }
    public static void removeOnlineTag(Tag tag){
        Queries.TagTable.updateBlacklistedTag(tag,false);
    }
    public static boolean isLogged(boolean x){
        String cookies=CookieManager.getInstance().getCookie(Utility.getBaseUrl());
        return cookies!=null&&cookies.contains("sessionid");
    }
    public static boolean isLogged(){
        return false;
    }



    public static User getUser() {
        return user;
    }

    public static void updateUser(User user) {
        Login.user = user;
        Login.user = null;//to delete
    }


    public static boolean isOnlineTags(Tag tag){
        return Queries.TagTable.isBlackListed(tag);
    }

    public static void hasLogged(WebView webView) {
    }
}
