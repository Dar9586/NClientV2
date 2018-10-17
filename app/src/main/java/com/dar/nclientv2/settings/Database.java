package com.dar.nclientv2.settings;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.dar.nclientv2.async.database.DatabaseHelper;

import androidx.annotation.Nullable;

public class Database{
    @Nullable private static DatabaseHelper helper;
    @Nullable private static SQLiteDatabase database;

    public static DatabaseHelper getHelper(){
        return helper;
    }

    public static void setHelper(DatabaseHelper helper){
        Database.helper = helper;
    }

    public static SQLiteDatabase getDatabase(){
        return database;
    }

    public static void setDatabase(SQLiteDatabase database){
        Database.database = database;
        Log.d(Global.LOGTAG,"SETTED database"+database);
    }

}
