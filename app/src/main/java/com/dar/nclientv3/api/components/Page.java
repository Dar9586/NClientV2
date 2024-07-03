package com.dar.nclientv3.api.components;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;

import androidx.annotation.NonNull;

import com.dar.nclientv3.api.enums.ImageExt;
import com.dar.nclientv3.api.enums.ImageType;
import com.dar.nclientv3.components.classes.Size;

import java.io.IOException;

public class Page implements Parcelable {
    public static final Creator<Page> CREATOR = new Creator<>() {
        @Override
        public Page createFromParcel(Parcel in) {
            return new Page(in);
        }

        @Override
        public Page[] newArray(int size) {
            return new Page[size];
        }
    };

    private final int page;
    private final ImageType imageType;
    private ImageExt imageExt;
    private final Size size;

    public Page() {
        this.imageType = ImageType.PAGE;
        this.imageExt = ImageExt.JPG;
        this.page = 0;
        this.size = new Size(0, 0);
    }

    public Page(ImageType type, JsonReader reader) throws IOException {
        this(type, reader, 0);
    }

    public Page(ImageType type, ImageExt ext) {
        this(type, ext, 0);
    }

    public Page(ImageType type, ImageExt ext, int page) {
        this.imageType = type;
        this.imageExt = ext;
        this.page = page;
        this.size = new Size(0, 0);
    }

    public Page(ImageType type, JsonReader reader, int page) throws IOException {
        this.imageType = type;
        this.page = page;
        this.size = new Size(0, 0);
        reader.beginObject();
        while (reader.peek() != JsonToken.END_OBJECT) {
            switch (reader.nextName()) {
                case "t":
                    imageExt = stringToExt(reader.nextString());
                    break;
                case "w":
                    size.setWidth(reader.nextInt());
                    break;
                case "h":
                    size.setHeight(reader.nextInt());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
    }

    protected Page(Parcel in) {
        page = in.readInt();
        size = in.readParcelable(Size.class.getClassLoader());
        imageExt = ImageExt.values()[in.readByte()];
        imageType = ImageType.values()[in.readByte()];
    }

    private static ImageExt stringToExt(String ext) {
        return charToExt(ext.charAt(0));
    }

    public static String extToString(ImageExt ext) {
        return switch (ext) {
            case GIF -> "gif";
            case PNG -> "png";
            case JPG -> "jpg";
        };
    }

    public static char extToChar(ImageExt imageExt) {
        return switch (imageExt) {
            case GIF -> 'g';
            case PNG -> 'p';
            case JPG -> 'j';
        };
    }

    public static ImageExt charToExt(char c) {
        return switch (c) {
            case 'g' -> ImageExt.GIF;
            case 'p' -> ImageExt.PNG;
            case 'j' -> ImageExt.JPG;
            default -> throw new IllegalArgumentException("Unsupported ImageExt value");
        };
    }

    public String extToString() {
        return extToString(imageExt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(page);
        dest.writeParcelable(size, flags);
        dest.writeByte((byte) (imageExt == null ? ImageExt.JPG.ordinal() : imageExt.ordinal()));
        dest.writeByte((byte) (imageType == null ? ImageType.PAGE.ordinal() : imageType.ordinal()));
    }

    public int getPage() {
        return page;
    }

    public ImageExt getImageExt() {
        return imageExt;
    }

    public char getImageExtChar() {
        return extToChar(imageExt);
    }

    public ImageType getImageType() {
        return imageType;
    }

    public Size getSize() {
        return size;
    }

    @NonNull
    @Override
    public String toString() {
        return "Page{" +
            "page=" + page +
            ", imageExt=" + imageExt +
            ", imageType=" + imageType +
            ", size=" + size +
            '}';
    }
}
