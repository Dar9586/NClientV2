package com.dar.nclientv2.components.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

import com.dar.nclientv2.BuildConfig;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.async.ScrapeTags;
import com.dar.nclientv2.async.database.DatabaseHelper;
import com.dar.nclientv2.async.downloader.DownloadGalleryV2;
import com.dar.nclientv2.components.classes.MySenderFactory;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.network.NetworkUtil;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.annotation.AcraCore;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


@AcraCore(buildConfigClass = BuildConfig.class, reportSenderFactoryClasses = MySenderFactory.class, reportContent = {
    ReportField.PACKAGE_NAME,
    ReportField.BUILD_CONFIG,
    ReportField.APP_VERSION_CODE,
    ReportField.STACK_TRACE,
    ReportField.ANDROID_VERSION,
    ReportField.LOGCAT
})
public class CrashApplication extends MultiDexApplication {
    private static final String SIGNATURE_GITHUB = "ce96fdbcc89991f083320140c148db5f";

    @Override
    public void onCreate() {
        super.onCreate();
        Global.initLanguage(this);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Global.initStorage(this);
        Database.setDatabase(new DatabaseHelper(getApplicationContext()).getWritableDatabase());
        String version = Global.getLastVersion(this), actualVersion = Global.getVersionName(this);
        SharedPreferences preferences = getSharedPreferences("Settings", 0);
        if (!actualVersion.equals(version))
            afterUpdateChecks(preferences, version, actualVersion);

        Global.initFromShared(this);
        NetworkUtil.initConnectivity(this);
        TagV2.initMinCount(this);
        TagV2.initSortByName(this);
        DownloadGalleryV2.loadDownloads(this);
    }

    private boolean signatureCheck() {
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
                if (SIGNATURE_GITHUB.equals(hash)) return true;
            }
        } catch (NullPointerException | PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void afterUpdateChecks(SharedPreferences preferences, String oldVersion, String actualVersion) {
        SharedPreferences.Editor editor = preferences.edit();
        removeOldUpdates();
        //update tags
        ScrapeTags.startWork(this);
        if ("0.0.0".equals(oldVersion))
            editor.putBoolean(getString(R.string.key_check_update), signatureCheck());
        editor.apply();
        Global.setLastVersion(this);
    }


    private void createIdHiddenFile(File folder) {
        LocalGallery gallery = new LocalGallery(folder);
        if (gallery.getId() < 0) return;
        File hiddenFile = new File(folder, "." + gallery.getId());
        try {
            hiddenFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createIdHiddenFiles() {
        if (!Global.hasStoragePermission(this)) return;
        File[] files = Global.DOWNLOADFOLDER.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory())
                createIdHiddenFile(f);
        }
    }

    private void removeOldUpdates() {
        if (!Global.hasStoragePermission(this)) return;
        Global.recursiveDelete(Global.UPDATEFOLDER);
        Global.UPDATEFOLDER.mkdir();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        ACRA.init(this);
        ACRA.getErrorReporter().setEnabled(getSharedPreferences("Settings", 0).getBoolean(getString(R.string.key_send_report), false));
    }
}
