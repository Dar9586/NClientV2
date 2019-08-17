package com.dar.nclientv2.components;

import android.app.Application;
import android.content.Context;

import com.dar.nclientv2.BuildConfig;
import com.dar.nclientv2.R;
import com.dar.nclientv2.async.database.DatabaseHelper;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.annotation.AcraCore;

import androidx.appcompat.app.AppCompatDelegate;


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
        Global.loadTheme(this);
        Global.initHttpClient(this);
        Global.initRemoveAvoidedGalleries(this);
        Global.initHighRes(this);
        Global.initOnlyTag(this);
        Global.initByPopular(this);
        Global.initLoadImages(this);
        Global.initOnlyLanguage(this);
        Global.initMaxId(this);
        Global.initInfiniteScroll(this);
        com.dar.nclientv2.settings.Login.initUseAccountTag(this);
    }

    @Override
    protected void attachBaseContext(Context newBase){
        super.attachBaseContext(newBase);
        ACRA.init(this);
        ACRA.getErrorReporter().setEnabled(getSharedPreferences("Settings",0).getBoolean(getString(R.string.key_send_report),true));
    }
}