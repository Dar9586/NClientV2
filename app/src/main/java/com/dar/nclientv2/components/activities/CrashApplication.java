package com.dar.nclientv2.components.activities;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.dar.nclientv2.BuildConfig;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.async.DownloadGallery;
import com.dar.nclientv2.async.database.DatabaseHelper;
import com.dar.nclientv2.components.classes.MySenderFactory;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Favorites;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.annotation.AcraCore;


@AcraCore(buildConfigClass = BuildConfig.class,reportSenderFactoryClasses = MySenderFactory.class,reportContent={
        ReportField.PACKAGE_NAME,
        ReportField.BUILD_CONFIG,
        ReportField.APP_VERSION_CODE,
        ReportField.STACK_TRACE,
        ReportField.ANDROID_VERSION,
        ReportField.LOGCAT
})
public class CrashApplication extends Application{
    @Override
    public void onCreate(){
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Database.setDatabase(new DatabaseHelper(getApplicationContext()).getWritableDatabase());
        Global.initStorage(this);
        Global.initFromShared(this);
        Favorites.countFavorite();

        TagV2.initMinCount(this);
        TagV2.initSortByName(this);
        String version=Global.getLastVersion(this),actualVersion=Global.getVersionName(this);
        SharedPreferences preferences=getSharedPreferences("Settings", 0);
        switch (version){//must execute all in order, no break required, for now
            case "1.9.7"://add default chinese translation
                if(getResources().getConfiguration().locale.getLanguage().equals("zh"))
                    preferences.edit().putString(getString(R.string.key_language),"zh").apply();
            case "2.1.1"://add ALL type for languages
                int val=preferences.getInt(getString(R.string.key_only_language), Language.ALL.ordinal());
                if(val==-1)val=Language.ALL.ordinal();
                preferences.edit().putInt(getString((R.string.key_only_language)),val).apply();
                Global.initFromShared(this);
        }
        Global.setLastVersion(this);
        DownloadGallery.loadDownloads(this);
    }

    @Override
    protected void attachBaseContext(Context newBase){
        super.attachBaseContext(newBase);
        ACRA.init(this);
        ACRA.getErrorReporter().setEnabled(getSharedPreferences("Settings",0).getBoolean(getString(R.string.key_send_report),true));
    }
}
