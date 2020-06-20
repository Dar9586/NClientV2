package com.dar.nclientv2.api.components;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.components.classes.Size;
import com.dar.nclientv2.utility.Utility;
import com.dar.nclientv2.utility.files.FileObject;

import java.util.Locale;

public abstract class GenericGallery implements Parcelable{

    public enum Type{COMPLETE,LOCAL,SIMPLE}
    public abstract int getId();
    public abstract Type getType();
    public abstract int getPageCount();
    public abstract boolean isValid();
    @NonNull
    public abstract String getTitle();
    public abstract Size getMaxSize();
    public abstract Size getMinSize();
    @Nullable
    public abstract String getPageURI(int page);
    public String sharePageUrl(int i) {
        return String.format(Locale.US,"https://"+ Utility.getHost()+"/g/%d/%d/",getId(),i+1);
    }
    public boolean isLocal(){
        return getType()==Type.LOCAL;
    }

    public abstract boolean hasGalleryData();
    public abstract GalleryData getGalleryData();
    public String getUri(FileObject directory, int page){
        if(directory==null)
            return getPageURI(page);

        FileObject file= LocalGallery.getPage(directory,page);
        if(file!=null)
            return file.getUri().toString();
        return getPageURI(page);
    }
}
