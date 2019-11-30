package com.dar.nclientv2.components;

import android.os.Parcel;
import android.os.Parcelable;

public class Size implements Parcelable {
    private int width,height;
    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    protected Size(Parcel in) {
        width = in.readInt();
        height = in.readInt();
    }

    public static final Creator<Size> CREATOR = new Creator<Size>() {
        @Override
        public Size createFromParcel(Parcel in) {
            return new Size(in);
        }

        @Override
        public Size[] newArray(int size) {
            return new Size[size];
        }
    };

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(width);
        dest.writeInt(height);
    }

    public static Size maxSize(Size s1,Size s2){
        return new Size(Math.max(s1.getWidth(),s2.getWidth()),Math.max(s2.getHeight(),s2.getWidth()));
    }

    @Override
    public String toString() {
        return "Size{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}
