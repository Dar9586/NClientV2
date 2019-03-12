package com.dar.nclientv2.api.components;

import android.os.Parcelable;

import java.util.List;

public abstract class GenericGallery implements Parcelable{
    public abstract int getId();
    public abstract boolean isLocal();
    public abstract int getPageCount();
    public abstract boolean isValid();
    public abstract String getTitle();
    public abstract List<Comment> getComments();
}
