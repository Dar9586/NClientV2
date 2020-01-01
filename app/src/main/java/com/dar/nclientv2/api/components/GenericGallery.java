package com.dar.nclientv2.api.components;

import android.os.Parcelable;

import com.dar.nclientv2.components.classes.Size;

import java.util.List;
import java.util.Locale;

public abstract class GenericGallery implements Parcelable{
    public abstract int getId();
    public abstract boolean isLocal();
    public abstract int getPageCount();
    public abstract boolean isValid();
    public abstract String getTitle();
    public abstract List<Comment> getComments();
    public abstract Size getMaxSize();
    public abstract Size getMinSize();
    public String sharePageUrl(int i) {
        return String.format(Locale.US,"https://nhentai.net/g/%d/%d/",getId(),i+1);
    }
}
