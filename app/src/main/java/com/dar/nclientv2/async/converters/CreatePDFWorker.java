package com.dar.nclientv2.async.converters;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.NotificationSettings;
import com.dar.nclientv2.utility.LogUtility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class CreatePDFWorker extends Worker {
    private final Context context;
    private int notId;
    private int totalPage;
    private NotificationCompat.Builder notification;

    public CreatePDFWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = getApplicationContext();
    }


    public static void startWork(Context context, LocalGallery gallery) {
        WorkManager manager = WorkManager.getInstance(context);
        Data data = new Data.Builder()
            .putString("GALLERY_FOLDER", gallery.getGalleryFolder().getFolder().getAbsolutePath())
            .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CreatePDFWorker.class).setInputData(data).build();
        manager.enqueue(request);
    }

    public static boolean hasPDFCapabilities() {
        try {
            Class.forName("android.graphics.pdf.PdfDocument");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!hasPDFCapabilities()) {
            return Result.failure();
        }
        File folder = new File(getInputData().getString("GALLERY_FOLDER"));
        LocalGallery gallery = new LocalGallery(folder, true);
        ;

        notId = NotificationSettings.getNotificationId();
        System.gc();
        totalPage = gallery.getPageCount();
        preExecute(gallery.getDirectory());
        PdfDocument document = new PdfDocument();
        File page;
        for (int a = 1; a <= gallery.getPageCount(); a++) {
            page = gallery.getPage(a);
            if (page == null) continue;
            Bitmap bitmap = BitmapFactory.decodeFile(page.getAbsolutePath());
            if (bitmap != null) {
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), a).create();
                PdfDocument.Page p = document.startPage(info);
                p.getCanvas().drawBitmap(bitmap, 0f, 0f, null);
                document.finishPage(p);
                bitmap.recycle();
            }
            notification.setProgress(totalPage - 1, a + 1, false);
            NotificationSettings.notify(context.getString(R.string.channel2_name), notId, notification.build());

        }
        notification.setContentText(context.getString(R.string.writing_pdf));
        notification.setProgress(totalPage, 0, true);
        NotificationSettings.notify(context.getString(R.string.channel2_name), notId, notification.build());
        try {

            File finalPath = Global.PDFFOLDER;
            finalPath.mkdirs();
            finalPath = new File(finalPath, gallery.getTitle() + ".pdf");
            finalPath.createNewFile();
            LogUtility.d("Generating PDF at: " + finalPath);
            FileOutputStream out = new FileOutputStream(finalPath);
            document.writeTo(out);
            out.close();
            document.close();
            notification.setProgress(0, 0, false);
            notification.setContentTitle(context.getString(R.string.created_pdf));
            notification.setContentText(gallery.getTitle());
            createIntentOpen(finalPath);
            NotificationSettings.notify(context.getString(R.string.channel2_name), notId, notification.build());
            LogUtility.d(finalPath.getAbsolutePath());
        } catch (IOException e) {
            notification.setContentTitle(context.getString(R.string.error_pdf));
            notification.setContentText(context.getString(R.string.failed));
            notification.setProgress(0, 0, false);
            NotificationSettings.notify(context.getString(R.string.channel2_name), notId, notification.build());
            throw new RuntimeException("Error generating file", e);
        } finally {
            document.close();
        }

        return Result.success();
    }

    private void createIntentOpen(File finalPath) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            Uri apkURI = FileProvider.getUriForFile(
                getApplicationContext(), context.getPackageName() + ".provider", finalPath);
            i.setDataAndType(apkURI, "application/pdf");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            List<ResolveInfo> resInfoList = getApplicationContext().getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                getApplicationContext().grantUriPermission(packageName, apkURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                notification.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, i, PendingIntent.FLAG_MUTABLE));
            } else {
                notification.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, i, 0));
            }
            LogUtility.d(apkURI.toString());
        } catch (IllegalArgumentException ignore) {//sometimes the uri isn't available

        }
    }

    private void preExecute(File file) {
        notification = new NotificationCompat.Builder(getApplicationContext(), Global.CHANNEL_ID2);
        notification.setSmallIcon(R.drawable.ic_pdf)
            .setOnlyAlertOnce(true)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(file.getName()))
            .setContentTitle(context.getString(R.string.channel2_title))
            .setContentText(context.getString(R.string.parsing_pages))
            .setProgress(totalPage - 1, 0, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS);
        NotificationSettings.notify(context.getString(R.string.channel2_name), notId, notification.build());
    }


}
