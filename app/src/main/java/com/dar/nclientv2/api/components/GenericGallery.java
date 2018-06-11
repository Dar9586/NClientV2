package com.dar.nclientv2.api.components;

import android.os.Parcelable;

public abstract class GenericGallery implements Parcelable{
    public abstract int getId();
    public abstract boolean isLocal();
    public abstract int getPageCount();
    public abstract boolean isValid();
    public abstract String getTitle();
}
