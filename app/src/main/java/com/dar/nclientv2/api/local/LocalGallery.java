package com.dar.nclientv2.api.local;

import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.util.JsonReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dar.nclientv2.api.components.GalleryData;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.enums.SpecialTagIds;
import com.dar.nclientv2.components.classes.Size;
import com.dar.nclientv2.files.GalleryFolder;
import com.dar.nclientv2.files.PageFile;
import com.dar.nclientv2.utility.LogUtility;

import java.io.File;
import java.io.FileReader;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalGallery extends GenericGallery {
    public static final Creator<LocalGallery> CREATOR = new Creator<LocalGallery>() {
        @Override
        public LocalGallery createFromParcel(Parcel in) {
            return new LocalGallery(in);
        }

        @Override
        public LocalGallery[] newArray(int size) {
            return new LocalGallery[size];
        }
    };
    private static final Pattern FILE_PATTERN = Pattern.compile("^(\\d{1,9})\\.(gif|png|jpg)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DUP_PATTERN = Pattern.compile("^(.*)\\.DUP\\d+$");
    private static final Pattern IDFILE_PATTERN = Pattern.compile("^\\.\\d{1,6}$");
    private final GalleryFolder folder;
    @NonNull
    private final GalleryData galleryData;
    private final String title, trueTitle;
    private final boolean valid;
    private boolean hasAdvancedData = true;
    @NonNull
    private Size maxSize = new Size(0, 0), minSize = new Size(Integer.MAX_VALUE, Integer.MAX_VALUE);

    public LocalGallery(@NonNull File file, boolean jumpDataRetrieve) {
        folder = new GalleryFolder(file);
        trueTitle = file.getName();
        title = createTitle(file);
        if (jumpDataRetrieve) {
            galleryData = GalleryData.fakeData();
        } else {
            galleryData = readGalleryData();
            if (galleryData.getId() == SpecialTagIds.INVALID_ID)
                galleryData.setId(getId());
        }
        //Start search pages
        //Find page with max number
        galleryData.setPageCount(folder.getMax());
        valid = folder.getPageCount() > 0;
    }

    public LocalGallery(@NonNull File file) {
        this(file, false);
    }

    private LocalGallery(Parcel in) {
        galleryData = Objects.requireNonNull(in.readParcelable(GalleryData.class.getClassLoader()));
        maxSize = Objects.requireNonNull(in.readParcelable(Size.class.getClassLoader()));
        minSize = Objects.requireNonNull(in.readParcelable(Size.class.getClassLoader()));
        trueTitle = in.readString();
        title = in.readString();
        hasAdvancedData = in.readByte() == 1;
        folder = in.readParcelable(GalleryFolder.class.getClassLoader());
        valid = true;
    }

    private static int getPageFromFile(File f) {
        String n = f.getName();
        return Integer.parseInt(n.substring(0, n.indexOf('.')));
    }

    private static String createTitle(File file) {
        String name = file.getName();
        Matcher matcher = DUP_PATTERN.matcher(name);
        if (!matcher.matches()) return name;
        String title = matcher.group(1);
        return title == null ? name : title;
    }

    /**
     * @return null if not found or the file if found
     */
    public static File getPage(File dir, int page) {
        if (dir == null || !dir.exists()) return null;
        String pag = String.format(Locale.US, "%03d.", page);
        File x;
        x = new File(dir, pag + "jpg");
        if (x.exists()) return x;
        x = new File(dir, pag + "png");
        if (x.exists()) return x;
        x = new File(dir, pag + "gif");
        if (x.exists()) return x;
        return null;
    }

    @Override
    public GalleryFolder getGalleryFolder() {
        return folder;
    }

    @NonNull
    private GalleryData readGalleryData() {
        File nomedia = folder.getGalleryDataFile();
        try (JsonReader reader = new JsonReader(new FileReader(nomedia))) {
            return new GalleryData(reader);
        } catch (Exception ignore) {
        }
        hasAdvancedData = false;
        return GalleryData.fakeData();
    }

    public void calculateSizes() {
        for (PageFile f : folder)
            checkSize(f);
    }

    private void checkSize(File f) {
        LogUtility.d("Decoding: " + f);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), options);
        if (options.outWidth > maxSize.getWidth()) maxSize.setWidth(options.outWidth);
        if (options.outWidth < minSize.getWidth()) minSize.setWidth(options.outWidth);
        if (options.outHeight > maxSize.getHeight()) maxSize.setHeight(options.outHeight);
        if (options.outHeight < minSize.getHeight()) minSize.setHeight(options.outHeight);
    }

    @NonNull
    @Override
    public Size getMaxSize() {
        return maxSize;
    }

    @NonNull
    @Override
    public Size getMinSize() {
        return minSize;
    }

    public String getTrueTitle() {
        return trueTitle;
    }

    @Override
    public boolean hasGalleryData() {
        return hasAdvancedData;
    }

    @Override
    @NonNull
    public GalleryData getGalleryData() {
        return galleryData;
    }

    @Override
    public Type getType() {
        return Type.LOCAL;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public int getId() {
        return folder.getId();
    }

    @Override
    public int getPageCount() {
        return galleryData.getPageCount();
    }

    @Override
    @NonNull
    public String getTitle() {
        return title;
    }

    public int getMin() {
        return folder.getMin();
    }

    @NonNull
    public File getDirectory() {
        return folder.getFolder();
    }

    @Nullable
    public File getPage(int index) {
        return folder.getPage(index);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(galleryData, flags);
        dest.writeParcelable(maxSize, flags);
        dest.writeParcelable(minSize, flags);
        dest.writeString(trueTitle);
        dest.writeString(title);
        dest.writeByte((byte) (hasAdvancedData ? 1 : 0));
        dest.writeParcelable(folder, flags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalGallery gallery = (LocalGallery) o;

        return folder.equals(gallery.folder);
    }

    @Override
    public int hashCode() {
        return folder.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "LocalGallery{" +
            "galleryData=" + galleryData +
            ", title='" + title + '\'' +
            ", folder=" + folder +
            ", valid=" + valid +
            ", maxSize=" + maxSize +
            ", minSize=" + minSize +
            '}';
    }
}
