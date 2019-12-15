package com.dar.nclientv2.async;

import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;

import java.io.IOException;

public class GalleryDownloader {
    public enum Status{NOT_STARTED,DOWNLOADING,PAUSED,FINISHED}
    private Gallery gallery;
    private Status status;
    private int progress;
    private boolean downloaded;
    private final int id;
    public final int notificationId=Global.getNotificationId();
    private int start=0,count=-1;
    public GalleryDownloader(Gallery gallery,Status status) {
        this.gallery = gallery;
        id=gallery.getId();
        this.status=status;
        progress=0;
        count=gallery.getPageCount();
        setDownloaded(gallery.isComplete());
    }
    public GalleryDownloader(int id,Status status) {
        this.id = id;
        this.gallery = null;
        this.status=status;
        progress=0;
        setDownloaded(false);
    }

    public int getId() {
        return id;
    }

    public int getProgress() {
        return progress+start-1;
    }
    public int getPureProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
    public int incrementProgress(){
        return ++progress+start-1;
    }

    public String getCover(){
        if(gallery!=null)return gallery.getThumbnail();
        return null;
    }

    public int getCount() {
        return count;
    }

    public Gallery getGallery() {
        if(!downloaded) {
            try {
                gallery=Gallery.galleryFromId(id);
                setDownloaded(true);
            } catch (IOException ignore) { }
        }
        return gallery;
    }
    public Gallery completeGallery()throws IOException{
        if(downloaded)return gallery;
        gallery=Gallery.galleryFromId(id);
        setDownloaded(true);
        return gallery;
    }
    public Status getStatus() {
        return status;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
        if(downloaded){
            Queries.DownloadTable.addGallery(Database.getDatabase(),gallery);
            if(count==-1)count=gallery.getPageCount();
        }
    }

    public void setStatus(Status status) {
        this.status = status;
        if(status==Status.FINISHED)Queries.DownloadTable.removeGallery(Database.getDatabase(),id);
    }
    public String getPathTitle(){
        if(gallery!=null)return gallery.getPathTitle();
        return "";
    }
    public int getPercentage(){
        if(!downloaded)return 0;
        return Math.min(100,(progress*100)/count);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GalleryDownloader that = (GalleryDownloader) o;

        return id == that.id;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
