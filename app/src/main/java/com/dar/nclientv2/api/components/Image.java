package com.dar.nclientv2.api.components;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;

import com.dar.nclientv2.api.enums.ImageType;

import java.io.IOException;
import java.util.Locale;

public class Image implements Parcelable{
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
    Image(boolean jpg, int galleryId, ImageType type) {
        this.galleryId=galleryId;
        this.type=type;
        this.jpg=jpg;
    }

    protected Image(Parcel in) {
        jpg = in.readByte() != 0;
        galleryId = in.readInt();
        type=ImageType.values()[in.readInt()];
    }

    public static final Creator<Image> CREATOR = new Creator<Image>() {
        @Override
        public Image createFromParcel(Parcel in) {
            return new Image(in);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (jpg ? 1 : 0));
        parcel.writeInt(galleryId);
        parcel.writeInt(type.ordinal());
    }
}