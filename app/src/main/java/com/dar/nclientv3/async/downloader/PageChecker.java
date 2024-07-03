package com.dar.nclientv3.async.downloader;

public class PageChecker extends Thread {
    @Override
    public void run() {
        for (GalleryDownloaderV2 g : DownloadQueue.getDownloaders())
            if (g.hasData()) g.initDownload();
    }
}
