package com.dar.nclientv3.async.downloader;

public interface DownloadObserver {
    void triggerStartDownload(GalleryDownloaderV2 downloader);

    void triggerUpdateProgress(GalleryDownloaderV2 downloader, int reach, int total);

    void triggerEndDownload(GalleryDownloaderV2 downloader);

    void triggerCancelDownload(GalleryDownloaderV2 downloader);

    void triggerPauseDownload(GalleryDownloaderV2 downloader);
}
