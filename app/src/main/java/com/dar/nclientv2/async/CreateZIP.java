package com.dar.nclientv2.async;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CreateZIP extends JobIntentService {
    private int notId;
    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder notification;
    private byte[]buffer=new byte[1024];
    public CreateZIP() {
    }
    public static void startWork(Context context,LocalGallery gallery){
        Intent i=new Intent();
        i.putExtra(context.getPackageName() + ".GALLERY",gallery);
        enqueueWork(context,CreateZIP.class,555,i);
    }
    @Override
    protected void onHandleWork(@Nullable Intent intent) {
        System.gc();
        LocalGallery gallery = intent.getParcelableExtra(getPackageName() + ".GALLERY");
        preExecute(gallery.getDirectory());
        int allPage = gallery.getPageCount();
        try {
            File file = new File(Global.ZIPFOLDER, gallery.getTitle() + ".zip");
            FileOutputStream o = new FileOutputStream(file);
            ZipOutputStream out=new ZipOutputStream(o);
            FileInputStream in;
            File actual;
            int read;
            for (int i = 0; i < allPage; i++) {
                actual=gallery.getPage(i);
                if(actual==null)continue;
                ZipEntry entry=new ZipEntry(actual.getName());
                in=new FileInputStream(actual);
                out.putNextEntry(entry);
                while((read=in.read(buffer))!=-1){
                    out.write(buffer,0,read);
                }
                in.close();
                out.closeEntry();
                notification.setProgress(gallery.getPageCount(),i,false);
                notificationManager.notify(getString(R.string.channel3_name),notId,notification.build());
            }
            out.flush();
            out.close();
            postExecute(true, gallery, null);
        }catch (IOException e){
            LogUtility.e(e.getLocalizedMessage(),e);
            postExecute(false,gallery,e.getLocalizedMessage());
        }

    }

    private void postExecute(boolean success, LocalGallery gallery, String localizedMessage) {
        notification.setProgress(0,0,false)
                    .setContentTitle(success?getString(R.string.created_zip):getString(R.string.failed_zip));
        if(!success){
            notification.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(gallery.getTitle())
                    .setSummaryText(localizedMessage));
        }
        notificationManager.notify(getString(R.string.channel3_name),notId,notification.build());

    }

    private void preExecute(File file) {
        notId = Global.getNotificationId();
        notificationManager=NotificationManagerCompat.from(getApplicationContext());
        notification=new NotificationCompat.Builder(getApplicationContext(), Global.CHANNEL_ID3);
        notification.setSmallIcon(R.drawable.ic_archive)
                .setOnlyAlertOnce(true)
                .setContentText(file.getName())
                .setContentTitle(getString(R.string.channel3_title))
                .setProgress(1,0,false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS);
        notificationManager.notify(getString(R.string.channel3_name),notId,notification.build());
    }
}
