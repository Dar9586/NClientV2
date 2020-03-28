package com.dar.nclientv2.async;

import android.os.Parcel;
import android.os.Parcelable;

import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Global;

import java.io.IOException;
@Deprecated
public class GalleryDownloader implements Parcelable {
    protected GalleryDownloader(Parcel in) {
        gallery = in.readParcelable(GenericGallery.class.getClassLoader());
        progress = in.readInt();
        downloaded = in.readByte() != 0;
        id = in.readInt();
        start = in.readInt();
        count = in.readInt();
    }

    public static final Creator<GalleryDownloader> CREATOR = new Creator<GalleryDownloader>() {
        @Override
        public GalleryDownloader createFromParcel(Parcel in) {
            return new GalleryDownloader(in);
        }

        @Override
        public GalleryDownloader[] newArray(int size) {
            return new GalleryDownloader[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(gallery, flags);
        dest.writeInt(progress);
        dest.writeByte((byte) (downloaded ? 1 : 0));
        dest.writeInt(id);
        dest.writeInt(start);
        dest.writeInt(count);
    }

    public enum Status{NOT_STARTED,DOWNLOADING,PAUSED,FINISHED}
    private GenericGallery gallery;
    private Status status;
    private int progress;
    private boolean downloaded;
    private final int id;
    public final int notificationId=Global.getNotificationId();
    private int start=0,count=-1;
    public GalleryDownloader(GenericGallery gallery, Status status) {
        this.gallery = gallery;
        id=gallery.getId();
        this.status=status;
        progress=0;
        count=gallery.getPageCount();
        setDownloaded(gallery.getType()== GenericGallery.Type.COMPLETE);
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
        GenericGallery gallery=this.gallery;
        if(!downloaded) {
            try {
                gallery=Gallery.galleryFromId(id);
                if(gallery==null)return null;
                setDownloaded(true);
            } catch (IOException ignore) { }
        }
        return (Gallery) gallery;
    }
    public Gallery completeGallery()throws IOException{
        if(downloaded)return (Gallery) gallery;
        Gallery gallery=Gallery.galleryFromId(id);
        if(gallery==null)throw new IOException();
        this.gallery=gallery;
        setDownloaded(true);
        return gallery;
    }
    public Status getStatus() {
        return status;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
        if(downloaded){
            Queries.DownloadTable.addGallery((Gallery) gallery);
            if(count==-1)count=gallery.getPageCount();
        }
    }

    public void setStatus(Status status) {
        this.status = status;
        if(status==Status.FINISHED)Queries.DownloadTable.removeGallery(id);
    }
    public String getPathTitle(){
        if(gallery!=null)return ((Gallery)gallery).getPathTitle();
        return "";
    }
    public int getPercentage(){
        if(!downloaded)return 0;
        count=Math.max(1,count);
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
