package com.dar.nclientv2.async.downloader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DownloadQueue {
    private static final List<GalleryDownloaderManager>downloadQueue= Collections.synchronizedList(new ArrayList<>());
    public static void add(GalleryDownloaderManager x){
        synchronized (downloadQueue) {
            for (GalleryDownloaderManager manager : downloadQueue)
                if (x.downloader().getId() == manager.downloader().getId()) {
                    manager.downloader().setStatus(GalleryDownloaderV2.Status.NOT_STARTED);
                    givePriority(manager.downloader());
                    return;
                }
            downloadQueue.add(x);
        }
    }

    public static GalleryDownloaderV2 fetchForData(){
        synchronized (downloadQueue) {
            for (GalleryDownloaderManager x : downloadQueue)
                if (!x.downloader().hasData()) return x.downloader();
            return null;
        }
    }
    public static GalleryDownloaderManager fetch() {
        synchronized (downloadQueue) {
            for (GalleryDownloaderManager x : downloadQueue)
                if (x.downloader().canBeFetched()) return x;
            return null;
        }
    }

    public static void clear() {
        synchronized (downloadQueue) {
            for (GalleryDownloaderManager x : downloadQueue)
                x.downloader().setStatus(GalleryDownloaderV2.Status.CANCELED);
            downloadQueue.clear();
        }
    }

    public static List<GalleryDownloaderV2> getDownloaders() {
        synchronized (downloadQueue) {
            List<GalleryDownloaderV2> downloaders = new ArrayList<>(downloadQueue.size());
            for (GalleryDownloaderManager manager : downloadQueue)
                downloaders.add(manager.downloader());
            return downloaders;
        }
    }

    public static void addObserver(DownloadObserver observer) {
        synchronized (downloadQueue) {
            for (GalleryDownloaderManager manager : downloadQueue)
                manager.downloader().addObserver(observer);
        }
    }
    public static void removeObserver(DownloadObserver observer) {
        synchronized (downloadQueue) {
            for (GalleryDownloaderManager manager : downloadQueue)
                manager.downloader().removeObserver(observer);
        }
    }
    private static GalleryDownloaderManager findManagerFromDownloader(GalleryDownloaderV2 downloader){
        synchronized (downloadQueue) {
            for (GalleryDownloaderManager manager : downloadQueue)
                if (manager.downloader() == downloader)
                    return manager;
            return null;
        }
    }
    public static void remove(int id,boolean cancel) {
        remove(findDownloaderFromId(id),cancel);
    }

    private static GalleryDownloaderV2 findDownloaderFromId(int id) {
        synchronized (downloadQueue) {
            for (GalleryDownloaderManager manager : downloadQueue)
                if (manager.downloader().getId() == id) return manager.downloader();
            return null;
        }
    }

    public static void remove(GalleryDownloaderV2 downloader,boolean cancel) {
        GalleryDownloaderManager manager=findManagerFromDownloader(downloader);
        if(manager==null)return;
        if(cancel) downloader.setStatus(GalleryDownloaderV2.Status.CANCELED);
        synchronized (downloadQueue) {
            downloadQueue.remove(manager);
        }
    }

    public static void givePriority(GalleryDownloaderV2 downloader) {
        GalleryDownloaderManager manager=findManagerFromDownloader(downloader);
        if(manager==null)return;
        synchronized (downloadQueue) {
            downloadQueue.remove(manager);
            downloadQueue.add(0, manager);
        }
    }

    public static GalleryDownloaderManager managerFromId(int id) {
        synchronized (downloadQueue) {
            for (GalleryDownloaderManager manager : downloadQueue)
                if (manager.downloader().getId() == id) return manager;
            return null;
        }
    }

    public static boolean isEmpty() {
        synchronized (downloadQueue) {
            return downloadQueue.size() == 0;
        }
    }
}
