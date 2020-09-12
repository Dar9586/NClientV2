package com.dar.nclientv2.async.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.SpecialTagIds;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.utility.LogUtility;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@SuppressWarnings("deprecation")
public class DatabaseHelper extends SQLiteOpenHelper{
    private static final int DATABASE_VERSION = 12;
    private static final String DATABASE_NAME = "Entries.db";
    public DatabaseHelper(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        createAllTables(db);
        Database.setDatabase(db);
        insertLanguageTags();
        insertCategoryTags();
        //Queries.DebugDatabase.dumpDatabase(db);
    }

    private void createAllTables(SQLiteDatabase db) {
        db.execSQL(Queries.GalleryTable.CREATE_TABLE);
        db.execSQL(Queries.TagTable.CREATE_TABLE);
        db.execSQL(Queries.GalleryBridgeTable.CREATE_TABLE);
        db.execSQL(Queries.BookmarkTable.CREATE_TABLE);
        db.execSQL(Queries.DownloadTable.CREATE_TABLE);
        db.execSQL(Queries.HistoryTable.CREATE_TABLE);
        db.execSQL(Queries.FavoriteTable.CREATE_TABLE);
        db.execSQL(Queries.ResumeTable.CREATE_TABLE);
    }

    private void insertCategoryTags(){
        Tag[] types = {
                new Tag("doujinshi", 0, 33172, TagType.CATEGORY, TagStatus.DEFAULT),
                new Tag("manga", 0, 33173, TagType.CATEGORY, TagStatus.DEFAULT),
                new Tag("misc", 0, 97152, TagType.CATEGORY, TagStatus.DEFAULT),
                new Tag("western", 0, 34125, TagType.CATEGORY, TagStatus.DEFAULT),
                new Tag("non-h", 0, 34065, TagType.CATEGORY, TagStatus.DEFAULT),
                new Tag("artistcg", 0, 36320, TagType.CATEGORY, TagStatus.DEFAULT),
        };
        for(Tag t:types)Queries.TagTable.insert(t);
    }
    private void insertLanguageTags(){
        Tag[] languages = {
            new Tag("english", 0, SpecialTagIds.LANGUAGE_ENGLISH, TagType.LANGUAGE, TagStatus.DEFAULT),
            new Tag("japanese", 0, SpecialTagIds.LANGUAGE_JAPANESE, TagType.LANGUAGE, TagStatus.DEFAULT),
            new Tag("chinese", 0, SpecialTagIds.LANGUAGE_CHINESE, TagType.LANGUAGE, TagStatus.DEFAULT),
        };
        for(Tag t:languages)Queries.TagTable.insert(t);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        Database.setDatabase(db);
        if(oldVersion==2) insertLanguageTags();
        if(oldVersion<=3) insertCategoryTags();
        if(oldVersion<=4)db.execSQL(Queries.BookmarkTable.CREATE_TABLE);
        if(oldVersion<=5)updateGalleryWithSizes(db);
        if(oldVersion<=6)db.execSQL(Queries.DownloadTable.CREATE_TABLE);
        if(oldVersion<=7)db.execSQL(Queries.HistoryTable.CREATE_TABLE);
        if(oldVersion<=8)insertFavorite(db);
        if(oldVersion<=9)addRangeColumn(db);
        if(oldVersion<=10)db.execSQL(Queries.ResumeTable.CREATE_TABLE);
        if(oldVersion<=11)updateFavoriteTable(db);
    }

    private void updateFavoriteTable(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Favorite ADD COLUMN `time` INT NOT NULL DEFAULT "+new Date().getTime());
    }

    private void addRangeColumn(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Downloads ADD COLUMN `range_start` INT NOT NULL DEFAULT -1");
        db.execSQL("ALTER TABLE Downloads ADD COLUMN `range_end`   INT NOT NULL DEFAULT -1");
    }

    /**
     * Add all item which are favorite into the favorite table
     * */
    private int[] getAllFavoriteIndex() {
        Cursor c=Queries.GalleryTable.getAllFavoriteCursorDeprecated("%",false);
        int[]favorites=new int[c.getCount()];
        int i=0;
        if(c.moveToFirst()){
            do{
                favorites[i++]=c.getInt(c.getColumnIndex(Queries.GalleryTable.IDGALLERY));
            }while(c.moveToNext());
        }
        c.close();
        return favorites;
    }
    /**
     * Create favorite table
     * Get all id of favorite gallery
     * save all galleries
     * delete and recreate table without favorite column
     * insert all galleries again
     * populate favorite
     * */
    private void insertFavorite(SQLiteDatabase db) {
        Database.setDatabase(db);
        db.execSQL(Queries.FavoriteTable.CREATE_TABLE);
        try {
            int[]favorites=getAllFavoriteIndex();
            List<Gallery> allGalleries= Queries.GalleryTable.getAllGalleries();
            db.execSQL(Queries.GalleryTable.DROP_TABLE);
            db.execSQL(Queries.GalleryTable.CREATE_TABLE);
            for(Gallery g:allGalleries)Queries.GalleryTable.insert(g);
            for(int i:favorites)Queries.FavoriteTable.insert(i);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add the columns which contains the sizes of the images
     * */
    private void updateGalleryWithSizes(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Gallery ADD COLUMN `maxW` INT NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE Gallery ADD COLUMN `maxH` INT NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE Gallery ADD COLUMN `minW` INT NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE Gallery ADD COLUMN `minH` INT NOT NULL DEFAULT 0");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        LogUtility.d("Downgrading database from "+oldVersion+" to "+newVersion);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        Database.setDatabase(db);
        Queries.GalleryTable.clearGalleries();
    }
}
