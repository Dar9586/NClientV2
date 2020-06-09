package com.dar.nclientv2.settings;

import android.content.Context;

import androidx.annotation.NonNull;

import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.loginapi.User;

public class Login{
    private static User user;
    private static boolean accountTag;

    public static void  initUseAccountTag(@NonNull Context context){

    }

    public static boolean useAccountTag(){
        return false;
    }

    public static void logout(){
        updateUser(null);
        clearOnlineTags();
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

    public static boolean isLogged(){
        return false;
    }



    public static User getUser() {
        return null;
    }

    public static void updateUser(User user) {
        Login.user = user;
    }


    public static boolean isOnlineTags(Tag tag){
        return Queries.TagTable.isBlackListed(tag);
    }
}
