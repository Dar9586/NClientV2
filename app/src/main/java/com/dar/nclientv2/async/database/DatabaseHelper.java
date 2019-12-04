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
    private static final int DATABASE_VERSION = 7;
    private static final String DATABASE_NAME = "Entries.db";
    public DatabaseHelper(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(Queries.GalleryTable.CREATE_TABLE);
        db.execSQL(Queries.TagTable.CREATE_TABLE);
        db.execSQL(Queries.GalleryBridgeTable.CREATE_TABLE);
        db.execSQL(Queries.BookmarkTable.CREATE_TABLE);
        db.execSQL(Queries.DownloadTable.CREATE_TABLE);
        createLanguageTags(db);
        createCategoryTags(db);
        //Queries.DebugDatabase.dumpDatabase(db);
    }
    private void createCategoryTags(SQLiteDatabase db){
        Tag[] languages = {
                new Tag("doujinshi", 0, 33172, TagType.CATEGORY, TagStatus.DEFAULT),
                new Tag("manga", 0, 33173, TagType.CATEGORY, TagStatus.DEFAULT),
                new Tag("misc", 0, 97152, TagType.CATEGORY, TagStatus.DEFAULT),
                new Tag("western", 0, 34125, TagType.CATEGORY, TagStatus.DEFAULT),
                new Tag("non-h", 0, 34065, TagType.CATEGORY, TagStatus.DEFAULT),
                new Tag("artistcg", 0, 36320, TagType.CATEGORY, TagStatus.DEFAULT),
        };
        for(Tag t:languages)Queries.TagTable.insert(db,t);
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
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        if(oldVersion==2)createLanguageTags(db);
        if(oldVersion<=3)createCategoryTags(db);
        if(oldVersion<=4)db.execSQL(Queries.BookmarkTable.CREATE_TABLE);
        if(oldVersion<=5)updateGalleryWithSizes(db);
        if(oldVersion<=6)db.execSQL(Queries.DownloadTable.CREATE_TABLE);
    }

    private void updateGalleryWithSizes(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Gallery ADD COLUMN `maxW` INT NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE Gallery ADD COLUMN `maxH` INT NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE Gallery ADD COLUMN `minW` INT NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE Gallery ADD COLUMN `minH` INT NOT NULL DEFAULT 0");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        Log.d(Global.LOGTAG,"Downgrading database from "+oldVersion+" to "+newVersion);
        onCreate(db);
    }
}
