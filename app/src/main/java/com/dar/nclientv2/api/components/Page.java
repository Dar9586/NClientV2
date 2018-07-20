package com.dar.nclientv2.api.components;

import android.os.Parcel;
import android.util.JsonReader;

import com.dar.nclientv2.api.enums.ImageType;

import java.io.IOException;
import java.util.Locale;

public class Page extends Image {
    private int page;
    Page(JsonReader jr, int galleryId, int page) throws IOException {
        super(jr, galleryId, ImageType.PAGE);
        this.page=page;
    }
    Page(boolean jpg, int galleryId, int page) {
        super(jpg, galleryId, ImageType.PAGE);
        this.page=page;
    }
    public String getLowUrl(){
        return String.format(Locale.US,"https://t.nhentai.net/galleries/%d/%dt.%s",galleryId,page,jpg?"jpg":"png");
    }
    @Override
    public String getUrl() {
        return String.format(Locale.US,"https://i.nhentai.net/galleries/%d/%d.%s",galleryId,page,jpg?"jpg":"png");
    }

    @Override
    public String toString() {
        return "Page{" +
                "page=" + page +
                ", jpg=" + jpg +
                ", type=" + type +
                ", galleryId=" + galleryId +
                ", URL="+getUrl()+
                '}';
    }
    public static final Creator<Page> CREATOR = new Creator<Page>() {
        @Override
        public Page createFromParcel(Parcel in) {
            return new Page(in);
        }

        @Override
        public Page[] newArray(int size) {
            return new Page[size];
        }
    };
    protected Page(Parcel in) {
        super(in);
        page=in.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(page);
    }
}
