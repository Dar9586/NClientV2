package com.dar.nclientv2.async.downloader;

public class PageChecker extends Thread {
    @Override
    public void run() {
        for(GalleryDownloaderV2 g:DownloadQueue.getDownloaders())
            if(g.hasData())g.initDownload();
    }
}
