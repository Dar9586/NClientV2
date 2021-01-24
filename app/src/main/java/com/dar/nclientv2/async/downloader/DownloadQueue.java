package com.dar.nclientv2.async.downloader;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DownloadQueue {
    private static final List<GalleryDownloaderManager> downloadQueue = new CopyOnWriteArrayList<>();

    public static void add(GalleryDownloaderManager x) {
        for (GalleryDownloaderManager manager : downloadQueue)
            if (x.downloader().getId() == manager.downloader().getId()) {
                manager.downloader().setStatus(GalleryDownloaderV2.Status.NOT_STARTED);
                givePriority(manager.downloader());
                return;
            }
        downloadQueue.add(x);
    }

    public static GalleryDownloaderV2 fetchForData() {
        for (GalleryDownloaderManager x : downloadQueue)
            if (!x.downloader().hasData()) return x.downloader();
        return null;
    }

    public static GalleryDownloaderManager fetch() {
        for (GalleryDownloaderManager x : downloadQueue)
            if (x.downloader().canBeFetched()) return x;
        return null;
    }

    public static void clear() {
        for (GalleryDownloaderManager x : downloadQueue)
            x.downloader().setStatus(GalleryDownloaderV2.Status.CANCELED);
        downloadQueue.clear();
    }

    public static CopyOnWriteArrayList<GalleryDownloaderV2> getDownloaders() {
        CopyOnWriteArrayList<GalleryDownloaderV2> downloaders = new CopyOnWriteArrayList<>();
        for (GalleryDownloaderManager manager : downloadQueue)
            downloaders.add(manager.downloader());
        return downloaders;
    }

    public static void addObserver(DownloadObserver observer) {
        for (GalleryDownloaderManager manager : downloadQueue)
            manager.downloader().addObserver(observer);
    }

    public static void removeObserver(DownloadObserver observer) {
        for (GalleryDownloaderManager manager : downloadQueue)
            manager.downloader().removeObserver(observer);
    }

    private static GalleryDownloaderManager findManagerFromDownloader(GalleryDownloaderV2 downloader) {
        for (GalleryDownloaderManager manager : downloadQueue)
            if (manager.downloader() == downloader)
                return manager;
        return null;
    }

    public static void remove(int id, boolean cancel) {
        remove(findDownloaderFromId(id), cancel);
    }

    private static GalleryDownloaderV2 findDownloaderFromId(int id) {
        for (GalleryDownloaderManager manager : downloadQueue)
            if (manager.downloader().getId() == id) return manager.downloader();
        return null;
    }

    public static void remove(GalleryDownloaderV2 downloader, boolean cancel) {
        GalleryDownloaderManager manager = findManagerFromDownloader(downloader);
        if (manager == null) return;
        if (cancel)
            downloader.setStatus(GalleryDownloaderV2.Status.CANCELED);
        downloadQueue.remove(manager);
    }

    public static void givePriority(GalleryDownloaderV2 downloader) {
        GalleryDownloaderManager manager = findManagerFromDownloader(downloader);
        if (manager == null) return;
        downloadQueue.remove(manager);
        downloadQueue.add(0, manager);
    }

    public static GalleryDownloaderManager managerFromId(int id) {
        for (GalleryDownloaderManager manager : downloadQueue)
            if (manager.downloader().getId() == id) return manager;
        return null;
    }

    public static boolean isEmpty() {
        return downloadQueue.size() == 0;

    }
}
