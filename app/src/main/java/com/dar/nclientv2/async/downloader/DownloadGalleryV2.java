package com.dar.nclientv2.async.downloader;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;

import com.dar.nclientv2.api.SimpleGallery;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import java.io.IOException;
import java.util.List;

public class DownloadGalleryV2 extends JobIntentService {
    private static final Object lock=new Object();
    private static final int JOB_DOWNLOAD_GALLERY_ID=9999;
    public static void downloadGallery(Context context, GenericGallery gallery){
        if(gallery.isValid() && gallery instanceof Gallery)downloadGallery(context,(Gallery) gallery);
        if(gallery.getId()>0){
            if(gallery instanceof SimpleGallery){
                SimpleGallery simple= (SimpleGallery) gallery;
                downloadGallery(context,gallery.getTitle(),simple.getThumbnail(),simple.getId());
            }else downloadGallery(context,null,null,gallery.getId());
        }
    }
    private static void downloadGallery(Context context,String title,String thumbnail, int id){
        if(id<1)return;
        DownloadQueue.add(new GalleryDownloaderManager(context,title,thumbnail,id));
        startWork(context);
    }
    private static void downloadGallery(Context context, Gallery gallery){
        downloadGallery(context,gallery,0,gallery.getPageCount()-1);
    }
    private static void downloadGallery(Context context, Gallery gallery,int start,int end){
        DownloadQueue.add(new GalleryDownloaderManager(context,gallery,start,end));
        startWork(context);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        int startCommand=super.onStartCommand(intent, flags, startId);
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
        return startCommand;
    }

    public static void loadDownloads(Context context) {
        try {
            List<GalleryDownloaderManager> g = Queries.DownloadTable.getAllDownloads(context);
            for(GalleryDownloaderManager gg:g){
                gg.downloader().setStatus(GalleryDownloaderV2.Status.PAUSED);
                DownloadQueue.add(gg);
            }
            new PageChecker().start();
            startWork(context);
        }catch (IOException e){
            LogUtility.e(e,e);
        }
    }

    public static void downloadRange(Context context, Gallery gallery, int start, int end) {
        downloadGallery(context,gallery,start,end);
    }

    public static void startWork(@Nullable Context context) {
        if(context!=null)
            enqueueWork(context,DownloadGalleryV2.class,JOB_DOWNLOAD_GALLERY_ID,new Intent());
        synchronized (lock) {
                lock.notify();
        }
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        for(;;){
            obtainData();
            GalleryDownloaderManager entry = DownloadQueue.fetch();
            if(entry==null){
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                continue;
            }
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
