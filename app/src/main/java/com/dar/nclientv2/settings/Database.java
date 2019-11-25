package com.dar.nclientv2.settings;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

public class Database{
    @NonNull
    private static SQLiteDatabase database;
    @NonNull
    public static SQLiteDatabase getDatabase(){
        return database;
    }

    public static void setDatabase(SQLiteDatabase database){
        Database.database = database;
        Log.d(Global.LOGTAG,"SETTED database"+database);
    }

}
