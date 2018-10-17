package com.dar.nclientv2.components;

import android.app.Application;
import android.content.Context;

import com.dar.nclientv2.async.database.DatabaseHelper;
import com.dar.nclientv2.settings.Database;

import org.acra.ACRA;
import org.acra.BuildConfig;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraMailSender;

@AcraCore(buildConfigClass = BuildConfig.class)
@AcraMailSender(mailTo = "atoppi2013@gmail.com")
public class CrashApplication extends Application{
    @Override
    public void onCreate(){
        super.onCreate();
        Database.setHelper(new DatabaseHelper(getApplicationContext()));
        Database.setDatabase(Database.getHelper().getWritableDatabase());
    }

    @Override
    protected void attachBaseContext(Context newBase){
        super.attachBaseContext(newBase);
        ACRA.init(this);
    }
}