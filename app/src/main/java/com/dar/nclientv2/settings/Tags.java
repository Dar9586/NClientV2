package com.dar.nclientv2.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import static com.dar.nclientv2.settings.Global.LOGTAG;

public class Tags{
    public static final int MAXTAGS=100;
    private static List<Tag> accepted=new ArrayList<>(),avoided=new ArrayList<>();
    private static final List<Tag>[] sets= new List[5];

    private static int getScraperId(TagType tag){
        switch (tag){
            case TAG:return R.string.key_scraped_tags;
            case ARTIST:return R.string.key_scraped_artists;
            case GROUP:return R.string.key_scraped_group;
            case PARODY:return R.string.key_scraped_parodies;
            case CHARACTER:return R.string.key_scraped_characters;
        }
        return -1;
    }
    public static List<Tag> getTagSet(TagType type){
        switch (type){
            case TAG:return sets[0];
            case ARTIST:return sets[1];
            case GROUP:return sets[2];
            case PARODY:return sets[3];
            case CHARACTER:return sets[4];
        }
        return null;
    }
    private static int getOrdinalFromTag(TagType type){
        switch (type){
            case TAG:return 0;
            case ARTIST:return 1;
            case GROUP:return 2;
            case PARODY:return 3;
            case CHARACTER:return 4;
        }
        return -1;
    }

    public static void initTagSets(@NonNull Context context){
        boolean already=true;
        for(int a=0;a<5;a++)if(sets[a]==null){already=false;break;}
        if(already)return;
        sets[0]=getSet(context,getScraperId(TagType.TAG));
        sets[1]=getSet(context,getScraperId(TagType.ARTIST));
        sets[2]=getSet(context,getScraperId(TagType.GROUP));
        sets[3]=getSet(context,getScraperId(TagType.PARODY));
        sets[4]=getSet(context,getScraperId(TagType.CHARACTER));
    }

    public static void  initTagPreferencesSets(@NonNull Context context){
        if(accepted.size()!=0&&avoided.size()!=0)return;
        SharedPreferences preferences=context.getSharedPreferences("TagPreferences", 0);
        accepted=Tag.toArrayList(preferences.getStringSet(context.getString(R.string.key_accepted_tags),new HashSet<String>()));
        avoided=Tag.toArrayList(preferences.getStringSet(context.getString(R.string.key_avoided_tags),new HashSet<String>()));
        Log.i(LOGTAG,"Accepted"+accepted.toString());
        Log.i(LOGTAG,"Avoided"+avoided.toString());
    }


    public static TagStatus getStatus(Tag tag){
        if(accepted.contains(tag))return TagStatus.ACCEPTED;
        if(avoided.contains(tag))return TagStatus.AVOIDED;
        return TagStatus.DEFAULT;
    }
    public static String getQueryString(String query){
        StringBuilder builder=new StringBuilder();
        for (Tag x:accepted)if(!query.contains(x.getName()))builder.append('+').append(x.toQueryTag(TagStatus.ACCEPTED));
        for (Tag x:avoided) if(!query.contains(x.getName()))builder.append('+').append(x.toQueryTag(TagStatus.AVOIDED));
        if(Login.useAccountTag())for(Tag x:Login.getOnlineTags())if(!accepted.contains(x)&&!avoided.contains(x)&&!query.contains(x.getName()))builder.append('+').append(x.toQueryTag(TagStatus.AVOIDED));
        return builder.toString();
    }
    public static List<Tag>getListPrefer(){
        List<Tag>x=new ArrayList<>(accepted.size()+avoided.size());
        Log.i(LOGTAG,"Accepted"+accepted.toString());
        Log.i(LOGTAG,"Avoided"+avoided.toString());
        x.addAll(accepted);
        x.addAll(avoided);
        Collections.sort(x, new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return x;
    }

    @NonNull
    private static List<Tag> getSet(@NonNull Context context, @StringRes int res){
        Set<String>x= context.getSharedPreferences("ScrapedTags", 0).getStringSet(context.getString(res),new HashSet<String>());
        List<Tag>tags=new ArrayList<>(x.size());
        for(String y:x){
            tags.add(new Tag(y));
        }
        Collections.sort(tags, new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return tags;
    }


    public static void resetAllStatus(@NonNull Context context){
        context.getSharedPreferences("TagPreferences",0).edit().clear().apply();
        avoided.clear();accepted.clear();
    }

    private static boolean updateSharedTagPreferences(Context context){
        Log.i(LOGTAG,context.getSharedPreferences("TagPreferences",0).getAll().toString());
        boolean x=context.getSharedPreferences("TagPreferences",0).edit().clear()
                .putStringSet(context.getString(R.string.key_accepted_tags),Tag.toStringSet(accepted))
                .putStringSet(context.getString(R.string.key_avoided_tags),Tag.toStringSet(avoided)).commit();
        Log.i(LOGTAG,context.getSharedPreferences("TagPreferences",0).getAll().toString());
        return x;

    }
    public static TagStatus updateStatus(@NonNull Context context,Tag tag){
        if(accepted.contains(tag)){
            accepted.remove(tag);
            avoided.add(tag);
            return updateSharedTagPreferences(context)?TagStatus.AVOIDED:TagStatus.ACCEPTED;
        }
        if(avoided.contains(tag)){
            avoided.remove(tag);
            return updateSharedTagPreferences(context)?TagStatus.DEFAULT:TagStatus.AVOIDED;
        }
        if(maxTagReached())return TagStatus.DEFAULT;
        accepted.add(tag);
        return updateSharedTagPreferences(context)?TagStatus.ACCEPTED:TagStatus.DEFAULT;
    }
    public static boolean maxTagReached(){
        return accepted.size()+avoided.size()>=MAXTAGS;
    }





    public static void setPageReachedForType(@NonNull Context context , TagType type, int page){
        context.getSharedPreferences("ScrapedTags",0).edit().putInt(context.getString(getScraperId(type))+"_count",page).apply();
    }
    public static int pageReachedForType(@NonNull Context context ,TagType type){
        return context.getSharedPreferences("ScrapedTags",0).getInt(context.getString(getScraperId(type))+"_count",0);
    }
    public static void removeSet(@NonNull Context context ,TagType type){
        String s=context.getString(getScraperId(type));
        context.getSharedPreferences("ScrapedTags", 0).edit().remove(s).remove(s+"_count").apply();
    }

    public static void updateSet(@NonNull Context context, List<Tag>tags , TagType type){
        Set<String> x =context.getSharedPreferences("ScrapedTags", 0).getStringSet(context.getString(getScraperId(type)),new HashSet<String>());
        for (Tag y : tags) {
            x.add(y.toScrapedString());
        }
        if (!context.getSharedPreferences("ScrapedTags", 0).edit().putStringSet(context.getString(getScraperId(type)), x).commit()) {
            Log.e(LOGTAG, "Error to write set: " + type);
        }else sets[getOrdinalFromTag(type)]=tags;
    }

}
