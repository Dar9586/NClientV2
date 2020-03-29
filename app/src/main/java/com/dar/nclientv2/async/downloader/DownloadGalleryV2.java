package com.dar.nclientv2.async.downloader;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
        if(id<1)return;
        DownloadQueue.add(new GalleryDownloaderManager(context,id));
        startWork(context);
    }

    private static void downloadGallery(Context context, Gallery gallery){
        DownloadQueue.add(new GalleryDownloaderManager(context,gallery));
        startWork(context);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if(intent!=null){
            int id=intent.getIntExtra(getPackageName()+".ID",-1);
            String mode=intent.getStringExtra(getPackageName()+".MODE");
            LogUtility.d(""+mode);
            GalleryDownloaderManager manager=DownloadQueue.managerFromId(id);
            if(manager!=null){
                LogUtility.d("IntentAction: "+mode+" for id "+id);
                assert mode != null;
                switch (mode){
                    case "STOP": DownloadQueue.remove(id,true); break;
                    case "PAUSE":manager.downloader().setStatus(GalleryDownloaderV2.Status.PAUSED); break;
                    case "START":
                        manager.downloader().setStatus(GalleryDownloaderV2.Status.NOT_STARTED);
                        DownloadQueue.givePriority(manager.downloader());
                        startWork(this);
                        break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public static void loadDownloads(Context context) {
        try {
            List<Gallery> g = Queries.DownloadTable.getAllDownloads(Database.getDatabase());
            for(Gallery gg:g)downloadGallery(context,gg);
            DownloadQueue.setAllStatus(GalleryDownloaderV2.Status.PAUSED);
            new PageChecker().start();
            startWork(context);
        }catch (IOException e){
            LogUtility.e(e,e);
        }
    }

    public static void downloadRange(Context context, Gallery gallery, int start, int end) {
        downloadGallery(context,gallery);
    }

    public static void startWork(Context context) {
        enqueueWork(context,DownloadGalleryV2.class,JOB_DOWNLOAD_GALLERY_ID,new Intent());
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        while (true){
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
