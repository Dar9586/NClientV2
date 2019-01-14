package com.dar.nclientv2.async.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.settings.Global;

@SuppressWarnings("deprecation")
public class DatabaseHelper extends SQLiteOpenHelper{
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "Entries.db";
    public DatabaseHelper(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(Queries.GalleryTable.CREATE_TABLE);
        db.execSQL(Queries.TagTable.CREATE_TABLE);
        db.execSQL(Queries.BridgeTable.CREATE_TABLE);
        createLanguageTags(db);
        //Queries.DebugDatabase.dumpDatabase(db);
    }

    private void createLanguageTags(SQLiteDatabase db){
        Tag[] languages = {
            new Tag("english", 0, 12227, TagType.LANGUAGE, TagStatus.DEFAULT),
            new Tag("japanese", 0, 6346, TagType.LANGUAGE, TagStatus.DEFAULT),
            new Tag("chinese", 0, 29963, TagType.LANGUAGE, TagStatus.DEFAULT),
        };
        for(Tag t:languages)Queries.TagTable.insert(db,t);
    }

    @Override
    public void onOpen(SQLiteDatabase db){
        super.onOpen(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        if(oldVersion==2&&newVersion==3)createLanguageTags(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        Log.d(Global.LOGTAG,"Downgrading database from "+oldVersion+" to "+newVersion);
        onCreate(db);
    }

}
