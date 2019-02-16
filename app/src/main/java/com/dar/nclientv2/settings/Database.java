package com.dar.nclientv2.settings;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.Nullable;

public class Database{
    @Nullable private static SQLiteDatabase database;

    public static SQLiteDatabase getDatabase(){
        return database;
    }

    public static void setDatabase(SQLiteDatabase database){
        Database.database = database;
        Log.d(Global.LOGTAG,"SETTED database"+database);
    }

}
