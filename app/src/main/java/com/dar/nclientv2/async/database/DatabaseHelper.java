package com.dar.nclientv2.async.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("deprecation")
public class DatabaseHelper extends SQLiteOpenHelper{
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "Entries.db";
    private final Context context;
    public DatabaseHelper(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
        this.context=context;
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(Queries.GalleryTable.CREATE_TABLE);
        db.execSQL(Queries.TagTable.CREATE_TABLE);
        db.execSQL(Queries.BridgeTable.CREATE_TABLE);
        passOldSets(db);
        passOldFavorite(db);
        passOldFilters(db);
        passOldOnlineFavorite(db);
        //Queries.DebugDatabase.dumpDatabase(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db){
        super.onOpen(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        Log.d(Global.LOGTAG,"Upgrading database from "+oldVersion+" to "+newVersion);
        List<Gallery>galleries=new ArrayList<>();
        try{
            galleries=Queries.GalleryTable.getAll(Database.getDatabase());
        }catch(IOException e){
            e.printStackTrace();
        }
        db.execSQL(Queries.BridgeTable.DROP_TABLE);
        db.execSQL(Queries.BridgeTable.CREATE_TABLE);
        db.execSQL(Queries.GalleryTable.DROP_TABLE);
        db.execSQL(Queries.GalleryTable.CREATE_TABLE);
        for(Gallery g:galleries)Queries.GalleryTable.insert(db,g);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        Log.d(Global.LOGTAG,"Downgrading database from "+oldVersion+" to "+newVersion);
        onCreate(db);
    }
    private void passOldOnlineFavorite(SQLiteDatabase db){
        Map<String,?> x= context.getSharedPreferences("OnlineFavorite",0).getAll();
        Set<String>y= x.keySet();
        for(String z:y){
            String z1=context.getSharedPreferences("OnlineFavorite",0).getString(z,null);
            Gallery g;
            try{
                g = new Gallery(z1);
                Queries.GalleryTable.addFavorite(db,g,true);
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        context.getSharedPreferences("OnlineFavorite",0).edit().clear().apply();
    }
    private void passOldFavorite(SQLiteDatabase db){
        Set<String> x=context.getSharedPreferences("FavoriteList", 0).getStringSet(context.getString(R.string.key_favorite_list), new HashSet<>());
        try {
            for(String y:x){
                Gallery g=new Gallery(y);
                Queries.GalleryTable.insert(db,g);
                Queries.GalleryTable.addFavorite(db,g,false);
            }
        }catch (IOException e){
            Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
        }
        context.getSharedPreferences("FavoriteList",0).edit().clear().apply();
    }

    private void passOldSets(SQLiteDatabase db){
        Tags.initTagSets(context);
        for(TagType type:TagType.values()){
            switch(type){
                case LANGUAGE:case CATEGORY:case UNKNOWN:break;
                default: passTags(db,Tags.getTagSet(type));Tags.removeSet(context,type);break;
            }
        }

    }
    private void passOldFilters(SQLiteDatabase db){
        Tags.initTagPreferencesSets(context);
        for(Tag t:Tags.getAccepted()) Queries.TagTable.updateStatus(db,t.getId(),TagStatus.ACCEPTED);
        for(Tag t:Tags.getAvoided()) Queries.TagTable.updateStatus(db,t.getId(),TagStatus.AVOIDED);
        Tags.resetAllStatus(context);
    }

    private void passTags(SQLiteDatabase db, List<Tag>tags){
        if(tags==null)return;
        for(Tag t:tags){
            Queries.TagTable.insert(db,t);
        }
        SharedPreferences preferences=context.getSharedPreferences("ScrapedTags",0);
        SharedPreferences.Editor editor=preferences.edit();
        //PARODY,CHARACTER,TAG,ARTIST,GROUP
        editor.putInt(TagType.PARODY+"_page",preferences.getInt(context.getString(Tags.getScraperId(TagType.PARODY))+"_count",0));
        editor.putInt(TagType.CHARACTER+"_page",preferences.getInt(context.getString(Tags.getScraperId(TagType.CHARACTER))+"_count",0));
        editor.putInt(TagType.TAG+"_page",preferences.getInt(context.getString(Tags.getScraperId(TagType.TAG))+"_count",0));
        editor.putInt(TagType.ARTIST+"_page",preferences.getInt(context.getString(Tags.getScraperId(TagType.ARTIST))+"_count",0));
        editor.putInt(TagType.GROUP+"_page",preferences.getInt(context.getString(Tags.getScraperId(TagType.GROUP))+"_count",0));
        editor.apply();
    }
}
