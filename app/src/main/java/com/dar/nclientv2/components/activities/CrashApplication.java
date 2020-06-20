package com.dar.nclientv2.components.activities;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import androidx.appcompat.app.AppCompatDelegate;

import com.dar.nclientv2.BuildConfig;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.SortType;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.async.ScrapeTags;
import com.dar.nclientv2.async.database.DatabaseHelper;
import com.dar.nclientv2.async.downloader.DownloadGalleryV2;
import com.dar.nclientv2.components.classes.MySenderFactory;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Favorites;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.files.FileObject;
import com.dar.nclientv2.utility.files.MasterFileManager;
import com.dar.nclientv2.utility.network.NetworkUtil;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.annotation.AcraCore;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


@AcraCore(buildConfigClass = BuildConfig.class,reportSenderFactoryClasses = MySenderFactory.class,reportContent={
        ReportField.PACKAGE_NAME,
        ReportField.BUILD_CONFIG,
        ReportField.APP_VERSION_CODE,
        ReportField.STACK_TRACE,
        ReportField.ANDROID_VERSION,
        ReportField.LOGCAT
})
public class CrashApplication extends Application{
    private static final String SIGNATURE_GITHUB="ce96fdbcc89991f083320140c148db5f";
    @Override
    public void onCreate(){
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Global.initStorage(this);
        Database.setDatabase(new DatabaseHelper(getApplicationContext()).getWritableDatabase());
        String version=Global.getLastVersion(this),actualVersion=Global.getVersionName(this);
        SharedPreferences preferences=getSharedPreferences("Settings", Context.MODE_PRIVATE);
        if(!actualVersion.equals(version))
            afterUpdateChecks(preferences,version,actualVersion);

        Global.initFromShared(this);
        Favorites.countFavorite();
        NetworkUtil.initConnectivity(this);
        TagV2.initMinCount(this);
        TagV2.initSortByName(this);
        DownloadGalleryV2.loadDownloads(this);
    }

    private boolean signatureCheck(){
        try {
            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(), PackageManager.GET_SIGNATURES);
            //note sample just checks the first signature

            for (Signature signature : packageInfo.signatures) {
                // MD5 is used because it is not a secure data
                MessageDigest m = MessageDigest.getInstance("MD5");
                m.update(signature.toByteArray());
                String hash = new BigInteger(1, m.digest()).toString(16);
                LogUtility.d("Find signature: " + hash);
                if(SIGNATURE_GITHUB.equals(hash))return true;
            }
        }catch (NullPointerException|PackageManager.NameNotFoundException| NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        return false;
    }
    private void afterUpdateChecks(SharedPreferences preferences, String oldVersion, String actualVersion){
        SharedPreferences.Editor editor=preferences.edit();
        removeOldUpdates();
        //update tags
        ScrapeTags.startWork(this);
        //add ALL type for languages and replace null
        int val = preferences.getInt(getString(R.string.key_only_language), Language.ALL.ordinal());
        if (val == -1) val = Language.ALL.ordinal();
        editor.putInt(getString((R.string.key_only_language)), val);
        if("0.0.0".equals(oldVersion))
            editor.putBoolean(getString(R.string.key_check_update),signatureCheck());
        changeByPopularType(preferences,editor);
        editor.apply();
        createIdHiddenFiles();
        Global.setLastVersion(this);
    }

    private void changeByPopularType(SharedPreferences preferences, SharedPreferences.Editor editor) {
        String key=getString(R.string.key_by_popular);
        try{
            boolean x=preferences.getBoolean(key,false);
            editor.remove(key);
            editor.putInt(key,x? SortType.POPULAR_ALL_TIME.ordinal():SortType.RECENT_ALL_TIME.ordinal());
        }catch (ClassCastException ignore){}
    }

    private void createIdHiddenFile(FileObject folder){
        LocalGallery gallery=new LocalGallery(this,folder);
        if(gallery.getId()<0)return;
        FileObject hiddenFile= folder.createFile("."+gallery.getId());
    }

    private void createIdHiddenFiles() {
        if(!Global.hasStoragePermission(this))return;
        FileObject[]files= MasterFileManager.getDownloadFolder().listFiles();
        for(FileObject f:files){
            if(f.isDirectory())
                createIdHiddenFile(f);
        }
    }

    private void removeOldUpdates() {
        if(!Global.hasStoragePermission(this))return;
        for(FileObject file: MasterFileManager.getUpdateFolder().listFiles())
            Global.recursiveDelete(file);
    }

    @Override
    protected void attachBaseContext(Context newBase){
        super.attachBaseContext(newBase);
        ACRA.init(this);
        ACRA.getErrorReporter().setEnabled(getSharedPreferences("Settings",Context.MODE_PRIVATE).getBoolean(getString(R.string.key_send_report),true));
    }
}
