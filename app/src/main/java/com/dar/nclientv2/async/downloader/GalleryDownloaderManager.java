package com.dar.nclientv2.async.downloader;

import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.settings.Global;

import java.util.Locale;

public class GalleryDownloaderManager {
    private final int notificationId=Global.getNotificationId();
    private NotificationCompat.Builder notification;
    private GalleryDownloaderV2 downloaderV2;
    private Context context;
    private Gallery gallery;
    private final NotificationManagerCompat notificationManager;
    private DownloadObserver observer=new DownloadObserver() {
        @Override
        public void triggerStartDownload(GalleryDownloaderV2 downloader) {
            gallery=downloader.getGallery();
            prepareNotification();
            notificationUpdate();
        }

        @Override
        public void triggerUpdateProgress(GalleryDownloaderV2 downloader, int reach, int total) {
            setPercentage(reach, total);
            notificationUpdate();
        }

        @Override
        public void triggerEndDownload(GalleryDownloaderV2 downloader) {
            endNotification();
            notificationUpdate();
            DownloadQueue.remove(downloader);
        }
    };

    public GalleryDownloaderV2 downloader() {
        return downloaderV2;
    }

    public GalleryDownloaderManager(Context context, Gallery gallery) {
        this.notificationManager= NotificationManagerCompat.from(context);
        this.context = context;
        this.gallery = gallery;
        this.downloaderV2=new GalleryDownloaderV2(context,gallery);
        this.downloaderV2.addObserver(observer);
    }
    public GalleryDownloaderManager(Context context,int id){
        this.notificationManager= NotificationManagerCompat.from(context);
        this.context=context;
        this.downloaderV2=new GalleryDownloaderV2(context,id);
        this.downloaderV2.addObserver(observer);
    }

    private void endNotification() {
        hidePercentage();
        if(downloaderV2.getStatus()!= GalleryDownloaderV2.Status.CANCELED)notification.setContentTitle(String.format(Locale.US,context.getString(R.string.completed_format),gallery.getTitle()));
        else notification.setContentTitle(String.format(Locale.US,context.getString(R.string.cancelled_format),gallery.getTitle()));
    }

    private void hidePercentage(){
        setPercentage(0,0);
    }
    private void setPercentage(int reach,int total){
        notification.setProgress(total,reach,false);
    }
    private void prepareNotification() {
        notification=new NotificationCompat.Builder(context.getApplicationContext(), Global.CHANNEL_ID1);
        notification.setOnlyAlertOnce(true)
                .setContentTitle(String.format(Locale.US,context.getString(R.string.downloading_format),gallery.getTitle()))
                .setProgress(gallery.getPageCount(),0,false)
                .setSmallIcon(R.drawable.ic_file);
        setPercentage(0,1);
    }



    private void notificationUpdate() {
        notificationManager.notify(context.getString(R.string.channel1_name),notificationId,notification.build());
    }
}
