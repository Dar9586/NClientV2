package com.dar.nclientv2.components;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.dar.nclientv2.BuildConfig;
import com.dar.nclientv2.R;
import com.dar.nclientv2.async.database.DatabaseHelper;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Favorites;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.annotation.AcraCore;

import java.io.File;


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
        Global.loadThemeAndLanguage(this);
        Global.initStorage(this);
        Global.initFromShared(this);

        Favorites.countFavorite();

        TagV2.initMinCount(this);
        TagV2.initSortByName(this);
        getSharedPreferences("Settings",0).edit().remove(getString(R.string.key_language)).commit();
        String version=Global.getLastVersion(this),actualVersion=Global.getVersionName(this);
        fixUpdateFolder();
        switch (version){//must execute all in order, no break required, for now
            case "0.0.0":updateFolderStructure();
            case "1.9.2":fixUpdateFolder();
        }
        Global.setLastVersion(this);
    }
    private void fixUpdateFolder(){
        if(!Global.hasStoragePermission(this))return;
        boolean executed=true;
        File[]files=Global.DOWNLOADFOLDER.listFiles();
        if(files!=null)
            while(executed) {
                executed=false;
                for (File f : files) {
                    if(f.isDirectory())
                        for (File ff : f.listFiles()) {
                        if (ff.isDirectory()){
                            for(File fff:ff.listFiles())transferFolder(fff, f);
                            executed=true;
                            ff.delete();
                        }
                    }
                }
            }
    }
    private void updateFolderStructure() {
        if(!Global.hasStoragePermission(this))return;
        File[]files=Global.MAINFOLDER.listFiles();
        if(files!=null)for(File f:files){
            if(     f.equals(Global.PDFFOLDER)||
                    f.equals(Global.UPDATEFOLDER)||
                    f.equals(Global.DOWNLOADFOLDER)||
                    f.equals(Global.SCREENFOLDER)
            )continue;
            transferFolder(f,Global.DOWNLOADFOLDER);
        }

        files=Global.OLD_GALLERYFOLDER.listFiles();
        if(files!=null)for(File f:files){
            transferFolder(f,Global.SCREENFOLDER);
        }

        Global.OLD_GALLERYFOLDER.delete();
    }

    private void transferFolder(File from, File dest) {
        Log.d(Global.LOGTAG,"Transfer from "+from+" to "+dest);
        if(!from.exists())return;
        String name=from.getName();
        File newDest=new File(dest,name);
        if(from.isFile()){
            from.renameTo(newDest);
            return;
        }
        newDest.mkdirs();
        for(File ff:from.listFiles())
            transferFolder(ff,newDest);
        from.delete();
    }

    @Override
    protected void attachBaseContext(Context newBase){
        super.attachBaseContext(newBase);
        ACRA.init(this);
        ACRA.getErrorReporter().setEnabled(getSharedPreferences("Settings",0).getBoolean(getString(R.string.key_send_report),true));
    }
}
