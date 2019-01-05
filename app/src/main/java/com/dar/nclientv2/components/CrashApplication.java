package com.dar.nclientv2.components;

import android.app.Application;
import android.content.Context;

import com.dar.nclientv2.BuildConfig;
import com.dar.nclientv2.R;
import com.dar.nclientv2.async.database.DatabaseHelper;
import com.dar.nclientv2.settings.Database;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;


@AcraCore(buildConfigClass = BuildConfig.class,reportSenderFactoryClasses = MySenderFactory.class)
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
        ACRA.getErrorReporter().setEnabled(getSharedPreferences("Settings",0).getBoolean(getString(R.string.key_send_report),true));
    }
}