package com.dar.nclientv2.api.components;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;

import com.dar.nclientv2.api.enums.ImageType;
import com.dar.nclientv2.components.Size;

import java.io.IOException;

public class Page implements Parcelable {
    public enum ImageExt{PNG,JPG,GIF}

    private int page;
    private ImageExt imageExt;
    private ImageType imageType;
    private Size size;

    private static ImageExt charToExt(int ext){
        switch(ext){
            case 'g':return ImageExt.GIF;
            case 'p':return ImageExt.PNG;
            case 'j':return ImageExt.JPG;
        }
        return null;
    }
    private static ImageExt stringToExt(String ext){
        return charToExt(ext.charAt(0));
    }
    public Page(ImageType type, JsonReader reader)throws IOException{
        size=new Size(0,0);
        this.imageType=type;
        reader.beginObject();
        while (reader.peek()!= JsonToken.END_OBJECT){
            switch (reader.nextName()){
                case "t":imageExt=stringToExt(reader.nextString()); break;
                case "w":size.setWidth(reader.nextInt());  break;
                case "h":size.setHeight(reader.nextInt()); break;
            }
        }
    }

    protected Page(Parcel in) {
        page = in.readInt();
        size = in.readParcelable(Size.class.getClassLoader());
        imageExt =ImageExt.values()[in.readByte()];
        imageType =ImageType.values()[in.readByte()];
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(page);
        dest.writeParcelable(size, flags);
        dest.writeByte((byte) imageExt.ordinal());
        dest.writeByte((byte) imageType.ordinal());
    }

    public int getPage() {
        return page;
    }

    public ImageExt getImageExt() {
        return imageExt;
    }

    public ImageType getImageType() {
        return imageType;
    }

    public Size getSize() {
        return size;
    }
    public static String extToString(ImageExt ext){
        switch(ext){
            case GIF:return "gif";
            case PNG:return "png";
            case JPG:return "jpg";
        }
        return null;
    }
    public static char extToChar(ImageExt ext){
        switch(ext){
            case GIF:return 'g';
            case PNG:return 'p';
            case JPG:return 'j';
        }
        return '\0';
    }
}
