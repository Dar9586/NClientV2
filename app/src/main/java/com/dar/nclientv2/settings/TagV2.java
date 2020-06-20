package com.dar.nclientv2.settings;

import android.content.Context;

import androidx.annotation.NonNull;

import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.database.Queries;

import java.util.List;
import java.util.Set;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TagV2{
    public static final int MAXTAGS=100;
    private static int minCount;
    private static boolean sortByName;

    public static List<Tag> getTagSet(TagType type){
        return Queries.TagTable.getAllTagOfType(type);
    }
    public static List<Tag> getTagStatus(TagStatus status){
        return Queries.TagTable.getAllStatus(status);
    }
    public static String getQueryString(String query,@NonNull Set<Tag> all){
        StringBuilder builder=new StringBuilder();
        for(Tag t:all) if(!query.contains(t.getName()))builder.append('+').append(t.toQueryTag());
        return builder.toString();
    }
    public static List<Tag> getListPrefer(boolean removeIgnoredGalleries){
        return removeIgnoredGalleries?Queries.TagTable.getAllFiltered():
                Queries.TagTable.getAllStatus(TagStatus.ACCEPTED);
    }

    public static TagStatus updateStatus(Tag t){
        TagStatus old=t.getStatus();
        switch(t.getStatus()){
            case ACCEPTED:t.setStatus(TagStatus.AVOIDED);break;
            case AVOIDED:t.setStatus(TagStatus.DEFAULT);break;
            case DEFAULT:t.setStatus(TagStatus.ACCEPTED);break;
        }
        if(Queries.TagTable.updateTag(t)==1)return t.getStatus();
        throw new RuntimeException("Unable to update: "+t);

    }
    public static void resetAllStatus(){
        Queries.TagTable.resetAllStatus();
    }
    public static boolean containTag(Tag[]tags,Tag t){
        for(Tag t1:tags)if(t.equals(t1))return true;
        return false;
    }


    public static TagStatus getStatus(Tag tag){
        return Queries.TagTable.getStatus(tag);
    }



    public static boolean maxTagReached(){
        return getListPrefer(Global.removeAvoidedGalleries()).size()>=MAXTAGS;
    }
    public static void updateMinCount(Context context,int min){
        context.getSharedPreferences("ScrapedTags",Context.MODE_PRIVATE).edit().putInt("min_count",minCount=min).apply();
    }
    public static void initMinCount(Context context){
        minCount=context.getSharedPreferences("ScrapedTags",Context.MODE_PRIVATE).getInt("min_count",25);
    }
    public static void initSortByName(Context context){
        sortByName=context.getSharedPreferences("ScrapedTags",Context.MODE_PRIVATE).getBoolean("sort_by_name",false);
    }
    public static boolean updateSortByName(Context context){
        context.getSharedPreferences("ScrapedTags",Context.MODE_PRIVATE).edit().putBoolean("sort_by_name",sortByName=!sortByName).apply();
        return sortByName;
    }
    public static boolean isSortedByName(){
        return sortByName;
    }

    public static int getMinCount(){
        return minCount;
    }

    public static String getAvoidedTags() {
        StringBuilder builder=new StringBuilder();
        List<Tag>tags= Queries.TagTable.getAllStatus(TagStatus.AVOIDED);
        for(Tag t:tags)builder.append('+').append(t.toQueryTag(TagStatus.AVOIDED));
        return builder.toString();
    }
}
