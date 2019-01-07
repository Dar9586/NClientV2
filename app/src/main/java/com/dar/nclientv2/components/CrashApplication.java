package com.dar.nclientv2.components;

import android.app.Application;
import android.content.Context;

import com.dar.nclientv2.BuildConfig;
import com.dar.nclientv2.R;
import com.dar.nclientv2.async.database.DatabaseHelper;
import com.dar.nclientv2.settings.Database;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.annotation.AcraCore;


@AcraCore(buildConfigClass = BuildConfig.class,reportSenderFactoryClasses = MySenderFactory.class,reportContent={
        ReportField.PACKAGE_NAME,
        ReportField.APP_VERSION_CODE,
        ReportField.USER_CRASH_DATE,
        ReportField.BUILD_CONFIG,
        ReportField.APP_VERSION_NAME,
        ReportField.STACK_TRACE,
        ReportField.ANDROID_VERSION,
        ReportField.LOGCAT
})
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