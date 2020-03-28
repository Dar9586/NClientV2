package com.dar.nclientv2.async.downloader;

import java.util.ArrayList;
import java.util.List;

public class DownloadQueue {
    private static final ArrayList<GalleryDownloaderManager>downloadQueue=new ArrayList<>();
    public static void add(GalleryDownloaderManager x){
        for(GalleryDownloaderManager manager:downloadQueue)
            if(x.downloader().getId()==manager.downloader().getId())
                return;
        downloadQueue.add(x);
    }
    public static boolean finished(){
        for(GalleryDownloaderManager x:downloadQueue)
            if(x.downloader().canBeFetched())return false;
        return true;
    }
    public static GalleryDownloaderV2 fetchForData(){
        for(GalleryDownloaderManager x:downloadQueue)
            if(!x.downloader().hasData())return x.downloader();
        return null;
    }
    public static GalleryDownloaderManager fetch() {
        for(GalleryDownloaderManager x:downloadQueue)
            if(x.downloader().canBeFetched())return x;
        return null;
    }

    public static void clear() {
        for(GalleryDownloaderManager x:downloadQueue)
            x.downloader().setStatus(GalleryDownloaderV2.Status.CANCELED);
        downloadQueue.clear();
    }

    public static List<GalleryDownloaderV2> getDownloaders() {
        List<GalleryDownloaderV2>downloaders=new ArrayList<>(downloadQueue.size());
        for(GalleryDownloaderManager manager:downloadQueue)
            downloaders.add(manager.downloader());
        return downloaders;
    }

    public static void addObserver(DownloadObserver observer) {
        for(GalleryDownloaderManager manager:downloadQueue)
            manager.downloader().addObserver(observer);
    }
    public static void removeObserver(DownloadObserver observer) {
        for(GalleryDownloaderManager manager:downloadQueue)
            manager.downloader().removeObserver(observer);
    }
    private static GalleryDownloaderManager findManagerFromDownloader(GalleryDownloaderV2 downloader){
        for(GalleryDownloaderManager manager:downloadQueue)
            if (manager.downloader() == downloader)
                return manager;
        return null;
    }
    public static void remove(GalleryDownloaderV2 downloader) {
        GalleryDownloaderManager manager=findManagerFromDownloader(downloader);
        if(manager==null)return;
        downloader.setStatus(GalleryDownloaderV2.Status.CANCELED);
        downloadQueue.remove(manager);

    }

    public static void givePriority(GalleryDownloaderV2 downloader) {
        GalleryDownloaderManager manager=findManagerFromDownloader(downloader);
        if(manager==null)return;
        downloadQueue.remove(manager);
        downloadQueue.add(0,manager);
    }
    public static void setAllStatus(GalleryDownloaderV2.Status status){
        for(GalleryDownloaderManager manager:downloadQueue)manager.downloader().setStatus(status);
    }
}
