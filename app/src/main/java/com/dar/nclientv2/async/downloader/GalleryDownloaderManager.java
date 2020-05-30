package com.dar.nclientv2.async.downloader;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.NotificationSettings;

import java.util.Locale;

public class GalleryDownloaderManager {
    private final int notificationId= NotificationSettings.getNotificationId();
    private NotificationCompat.Builder notification;
    private final GalleryDownloaderV2 downloaderV2;
    private final Context context;
    private Gallery gallery;

    private final DownloadObserver observer=new DownloadObserver() {
        @Override
        public void triggerStartDownload(GalleryDownloaderV2 downloader) {
            gallery=downloader.getGallery();
            prepareNotification();
            addActionToNotification(false);
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
            addClickListener();
            notificationUpdate();
            DownloadQueue.remove(downloader,false);
        }

        @Override
        public void triggerStopDownlaod(GalleryDownloaderV2 downloader) {
            cancelNotification();
            Global.recursiveDelete(downloader.getFolder());
        }

        @Override
        public void triggerPauseDownload(GalleryDownloaderV2 downloader) {
            addActionToNotification(true);
            notificationUpdate();
        }
    };

    private void cancelNotification() {
        NotificationSettings.cancel(context.getString(R.string.channel1_name),notificationId);
    }

    private void addClickListener() {
        Intent notifyIntent = new Intent(context, GalleryActivity.class);
        notifyIntent.putExtra(context.getPackageName()+ ".GALLERY",downloaderV2.localGallery());
        notifyIntent.putExtra(context.getPackageName()+ ".ISLOCAL",true);
        // Create the PendingIntent
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        notification.setContentIntent(notifyPendingIntent);
    }

    public GalleryDownloaderV2 downloader() {
        return downloaderV2;
    }

    public GalleryDownloaderManager(Context context, Gallery gallery,int start,int end) {
        this.context = context;
        this.gallery = gallery;
        this.downloaderV2=new GalleryDownloaderV2(context,gallery,start,end);
        this.downloaderV2.addObserver(observer);
    }
    public GalleryDownloaderManager(Context context,int id){
        this.context=context;
        this.downloaderV2=new GalleryDownloaderV2(context,id);
        this.downloaderV2.addObserver(observer);
    }

    private void endNotification() {
        //notification=new NotificationCompat.Builder(context.getApplicationContext(), Global.CHANNEL_ID1);
        //notification.setOnlyAlertOnce(true).setSmallIcon(R.drawable.ic_check).setAutoCancel(true);
        clearNotificationAction();
        hidePercentage();
        if(downloaderV2.getStatus()!= GalleryDownloaderV2.Status.CANCELED){
            notification.setSmallIcon(R.drawable.ic_check);
            notification.setContentTitle(String.format(Locale.US,context.getString(R.string.completed_format),gallery.getTitle()));
        }
        else {
            notification.setSmallIcon(R.drawable.ic_close);
            notification.setContentTitle(String.format(Locale.US,context.getString(R.string.cancelled_format),gallery.getTitle()));
        }
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

    @SuppressLint("RestrictedApi")
    private void clearNotificationAction(){
        notification.mActions.clear();
    }
    private void addActionToNotification(boolean pauseMode) {
        clearNotificationAction();
        Intent startIntent=new Intent(context,DownloadGalleryV2.class);
        Intent stopIntent =new Intent(context,DownloadGalleryV2.class);
        Intent pauseIntent=new Intent(context,DownloadGalleryV2.class);

        //stopIntent.setAction("STOP");
        //startIntent.setAction("START");
        //pauseIntent.setAction("PAUSE");

        stopIntent .putExtra(context.getPackageName()+".ID",downloaderV2.getId());
        pauseIntent.putExtra(context.getPackageName()+".ID",downloaderV2.getId());
        startIntent.putExtra(context.getPackageName()+".ID",downloaderV2.getId());

        stopIntent .putExtra(context.getPackageName()+".MODE","STOP" );
        pauseIntent.putExtra(context.getPackageName()+".MODE","PAUSE");
        startIntent.putExtra(context.getPackageName()+".MODE","START");

        PendingIntent pStop  = PendingIntent.getService(context,0,stopIntent ,PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pPause = PendingIntent.getService(context,1,pauseIntent,PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pStart = PendingIntent.getService(context,2,startIntent,PendingIntent.FLAG_CANCEL_CURRENT);
        if(pauseMode)notification.addAction(R.drawable.ic_play  ,context.getString(R.string.resume),pStart);
        else notification.addAction(R.drawable.ic_pause ,context.getString(R.string.pause) ,pPause);
        notification.addAction(R.drawable.ic_close ,context.getString(R.string.cancel),pStop );
    }


    private void notificationUpdate() {
        NotificationSettings.notify(context.getString(R.string.channel1_name),notificationId,notification.build());
    }

}
