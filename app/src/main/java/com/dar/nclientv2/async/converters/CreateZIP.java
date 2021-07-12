package com.dar.nclientv2.async.converters;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.NotificationSettings;
import com.dar.nclientv2.utility.LogUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CreateZIP extends JobIntentService {
    // TODO: 11/04/20 REFACTOR CREATE ZIP AND PDF

    private final byte[] buffer = new byte[1024];
    private int notId;
    private NotificationCompat.Builder notification;

    public CreateZIP() {
    }

    public static void startWork(Context context, LocalGallery gallery) {
        Intent i = new Intent();
        i.putExtra(context.getPackageName() + ".GALLERY", gallery);
        enqueueWork(context, CreateZIP.class, 555, i);
    }

    @Override
    protected void onHandleWork(@Nullable Intent intent) {
        System.gc();
        LocalGallery gallery = intent.getParcelableExtra(getPackageName() + ".GALLERY");
        if (gallery == null) return;
        preExecute(gallery.getDirectory());
        try {
            File file = new File(Global.ZIPFOLDER, gallery.getTitle() + ".zip");
            FileOutputStream o = new FileOutputStream(file);
            ZipOutputStream out = new ZipOutputStream(o);
            out.setLevel(Deflater.BEST_COMPRESSION);
            FileInputStream in;
            File actual;
            int read;
            for (int i = 1; i <= gallery.getPageCount(); i++) {
                actual = gallery.getPage(i);
                if (actual == null) continue;
                ZipEntry entry = new ZipEntry(actual.getName());
                in = new FileInputStream(actual);
                out.putNextEntry(entry);
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.closeEntry();
                notification.setProgress(gallery.getPageCount(), i, false);
                NotificationSettings.notify(getString(R.string.channel3_name), notId, notification.build());
            }
            out.flush();
            out.close();
            postExecute(true, gallery, null, file);
        } catch (IOException e) {
            LogUtility.e(e.getLocalizedMessage(), e);
            postExecute(false, gallery, e.getLocalizedMessage(), null);
        }

    }

    private void postExecute(boolean success, LocalGallery gallery, String localizedMessage, File file) {
        notification.setProgress(0, 0, false)
            .setContentTitle(success ? getString(R.string.created_zip) : getString(R.string.failed_zip));
        if (!success) {
            notification.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(gallery.getTitle())
                .setSummaryText(localizedMessage));
        } else {
            createIntentOpen(file);
        }
        NotificationSettings.notify(getString(R.string.channel3_name), notId, notification.build());

    }

    private void createIntentOpen(File finalPath) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            Uri apkURI = FileProvider.getUriForFile(
                getApplicationContext(), getPackageName() + ".provider", finalPath);
            i.setDataAndType(apkURI, "application/zip");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            List<ResolveInfo> resInfoList = getApplicationContext().getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                getApplicationContext().grantUriPermission(packageName, apkURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            notification.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, i, 0));
            LogUtility.d(apkURI.toString());
        } catch (IllegalArgumentException ignore) {//sometimes the uri isn't available

        }
    }

    private void preExecute(File file) {
        notId = NotificationSettings.getNotificationId();
        notification = new NotificationCompat.Builder(getApplicationContext(), Global.CHANNEL_ID3);
        notification.setSmallIcon(R.drawable.ic_archive)
            .setOnlyAlertOnce(true)
            .setContentText(file.getName())
            .setContentTitle(getString(R.string.channel3_title))
            .setProgress(1, 0, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS);
        NotificationSettings.notify(getString(R.string.channel3_name), notId, notification.build());
    }
}
