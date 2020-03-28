package com.dar.nclientv2.async.downloader;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import java.io.IOException;
import java.util.List;

public class DownloadGalleryV2 extends JobIntentService {
    private static final int JOB_DOWNLOAD_GALLERY_ID=9999;
    public static void downloadGallery(Context context, GenericGallery gallery){
        if(gallery.isValid() && gallery instanceof Gallery)downloadGallery(context,(Gallery) gallery);
        if(gallery.getId()>0)downloadGallery(context,gallery.getId());
    }
    public static void downloadGallery(Context context, int id){
        DownloadQueue.add(new GalleryDownloaderManager(context,id));
        enqueueWork(context,DownloadGalleryV2.class,JOB_DOWNLOAD_GALLERY_ID,new Intent());
    }

    private static void downloadGallery(Context context, Gallery gallery){
        DownloadQueue.add(new GalleryDownloaderManager(context,gallery));
        enqueueWork(context,DownloadGalleryV2.class,JOB_DOWNLOAD_GALLERY_ID,new Intent());
    }

    public static void loadDownloads(Context context) {
        try {
            List<Gallery> g = Queries.DownloadTable.getAllDownloads(Database.getDatabase());
            for(Gallery gg:g)downloadGallery(context,gg);
            DownloadQueue.setAllStatus(GalleryDownloaderV2.Status.PAUSED);
        }catch (IOException e){
            LogUtility.e(e,e);
        }
    }

    public static void downloadRange(Context context, Gallery gallery, int start, int end) {
        downloadGallery(context,gallery);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        while (!DownloadQueue.finished()){
            obtainData();
            GalleryDownloaderManager entry = DownloadQueue.fetch();
            if(entry==null)return;
            LogUtility.d("Downloading: "+entry.downloader().getId());
            if(entry.downloader().downloadGalleryData()){
                entry.downloader().download();
            }
            Utility.threadSleep(1000);
        }
    }

    private void obtainData() {
        GalleryDownloaderV2 downloader=DownloadQueue.fetchForData();
        while (downloader!=null){
            downloader.downloadGalleryData();
            Utility.threadSleep(100);
            downloader=DownloadQueue.fetchForData();
        }
    }


}
