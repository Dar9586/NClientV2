package com.dar.nclientv2.settings;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.utility.LogUtility;

public class Database{
    @NonNull
    private static SQLiteDatabase database;
    @NonNull
    public static SQLiteDatabase getDatabase(){
        return database;
    }

    public static void setDatabase(SQLiteDatabase database){
        Database.database = database;
        LogUtility.d("SETTED database"+database);
        setDBForTables(database);
    }

    private static void setDBForTables(SQLiteDatabase database) {
        Queries.TagTable.setDb(database);
        Queries.BookmarkTable.setDb(database);
        Queries.DebugDatabase.setDb(database);
        Queries.DownloadTable.setDb(database);
        Queries.GalleryTable.setDb(database);
        Queries.HistoryTable.setDb(database);
        Queries.GalleryBridgeTable.setDb(database);
        Queries.FavoriteTable.setDb(database);

    }

}
