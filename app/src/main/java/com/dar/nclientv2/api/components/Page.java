package com.dar.nclientv2.api.components;

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
    Page(boolean jpg, int galleryId, int page) throws IOException {
        super(jpg, galleryId, ImageType.PAGE);
        this.page=page;
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
}
