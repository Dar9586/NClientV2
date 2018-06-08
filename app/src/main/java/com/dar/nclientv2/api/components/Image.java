package com.dar.nclientv2.api.components;
import android.util.JsonReader;
import android.util.JsonToken;

import com.dar.nclientv2.api.enums.ImageType;

import java.io.IOException;
import java.util.Locale;

public class Image {
    boolean jpg;
    final ImageType type;
    final int galleryId;
    Image(JsonReader jr, int galleryId, ImageType type) throws IOException {
        this.galleryId=galleryId;
        this.type=type;
        jr.beginObject();
        while (jr.peek()!= JsonToken.END_OBJECT){
            if(!jr.nextName().equals("t"))jr.skipValue();
            else jpg=jr.nextString().startsWith("j");
        }
        jr.endObject();
    }
    public String getUrl() {
        return type==ImageType.COVER?
                String.format(Locale.US,"https://t.nhentai.net/galleries/%d/cover.%s",galleryId,jpg?"jpg":"png"):
                String.format(Locale.US,"https://t.nhentai.net/galleries/%d/thumb.%s",galleryId,jpg?"jpg":"png");
    }

    @Override
    public String toString() {
        return "Image{" +
                "jpg=" + jpg +
                ", type=" + type +
                ", galleryId=" + galleryId +
                ",URL="+getUrl()+
                '}';
    }
}