package com.dar.nclientv2.files;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.dar.nclientv2.api.components.Page;
import com.dar.nclientv2.api.enums.ImageExt;
import com.dar.nclientv2.settings.Global;

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageFile extends File implements Parcelable {
    public static final Creator<PageFile> CREATOR = new Creator<PageFile>() {
        @Override
        public PageFile createFromParcel(Parcel in) {
            return new PageFile(in);
        }

        @Override
        public PageFile[] newArray(int size) {
            return new PageFile[size];
        }
    };
    private static final Pattern DEFAULT_THUMBNAIL = Pattern.compile("^0*1\\.(gif|png|jpg)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_PAGE = Pattern.compile("^0*(\\d+)\\.(gif|png|jpg)$", Pattern.CASE_INSENSITIVE);
    private final ImageExt ext;
    private final int page;

    public PageFile(ImageExt ext, File file, int page) {
        super(file.getAbsolutePath());
        this.ext = ext;
        this.page = page;
    }

    protected PageFile(Parcel in) {
        super(in.readString());
        page = in.readInt();
        ext = ImageExt.values()[in.readByte()];
    }

    /**
     * This only works with app-created files with format %03d.%s
     */
    private static @Nullable
    PageFile fastThumbnail(File folder) {
        for (ImageExt ext : ImageExt.values()) {
            String name = "001." + ext.getName();
            File file = new File(folder, name);
            if (file.exists()) return new PageFile(ext, file, 1);
        }
        return null;
    }

    public static @Nullable
    PageFile getThumbnail(Context context, int id) {
        File file = Global.findGalleryFolder(context, id);
        if (file == null) return null;
        PageFile pageFile = fastThumbnail(file);
        if (pageFile != null) return pageFile;
        File[] files = file.listFiles();
        if (files == null) return null;
        for (File f : files) {
            Matcher m = DEFAULT_THUMBNAIL.matcher(f.getName());
            if (!m.matches()) continue;
            ImageExt ext = Page.charToExt(Objects.requireNonNull(m.group(1)).charAt(0));
            return new PageFile(ext, f, 1);
        }
        return null;
    }

    public Uri toUri() {
        return Uri.fromFile(this);
    }

    public ImageExt getExt() {
        return ext;
    }

    public int getPage() {
        return page;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.getAbsolutePath());
        dest.writeInt(page);
        dest.writeByte((byte) ext.ordinal());
    }


}
