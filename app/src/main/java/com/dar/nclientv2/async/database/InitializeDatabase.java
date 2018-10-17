package com.dar.nclientv2.async.database;

import android.app.IntentService;
import android.content.Intent;

import com.dar.nclientv2.settings.Database;

import androidx.annotation.Nullable;

public class InitializeDatabase extends IntentService{
    private static boolean started=false;
    public InitializeDatabase(){
        super("DBINIT");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent){
        if(started)return;
        started=true;
        Database.setHelper(new DatabaseHelper(getApplicationContext()));
        Database.setDatabase(Database.getHelper().getWritableDatabase());
    }
}
