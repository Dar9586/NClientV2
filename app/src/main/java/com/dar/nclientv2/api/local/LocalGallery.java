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
import com.dar.nclientv2.utility.LogUtility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
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
    @NonNull
    private final GalleryData galleryData;
    private final int min;
    private final String title, trueTitle;
    @NonNull
    private final File directory;
    private final boolean valid;
    private final int id;
    private boolean hasAdvancedData = true;
    @NonNull
    private Size maxSize = new Size(0, 0), minSize = new Size(Integer.MAX_VALUE, Integer.MAX_VALUE);

    public LocalGallery(@NonNull File file, boolean jumpDataRetrieve) {
        directory = file;
        trueTitle = file.getName();
        title = createTitle(file);
        if (jumpDataRetrieve) {
            galleryData = GalleryData.fakeData();
            id = readIdFile();
        } else {
            galleryData = readGalleryData();
            if (galleryData.getId() == SpecialTagIds.INVALID_ID)
                galleryData.setId(oldReadId());
            id = galleryData.getId();
        }
        int max = 0, min = Integer.MAX_VALUE;
        //Start search pages
        File[] files = retrieveValidImages();
        //Find page with max number
        if (files.length >= 1) {
            Arrays.sort(files, (o1, o2) -> getPageFromFile(o1) - getPageFromFile(o2));
            min = getPageFromFile(files[0]);
            max = getPageFromFile(files[files.length - 1]);
        }
        galleryData.setPageCount(max);
        this.min = min;
        valid = files.length > 0;
    }

    public LocalGallery(@NonNull File file) {
        this(file, false);
    }

    private LocalGallery(Parcel in) {
        id = in.readInt();
        galleryData = Objects.requireNonNull(in.readParcelable(GalleryData.class.getClassLoader()));
        maxSize = Objects.requireNonNull(in.readParcelable(Size.class.getClassLoader()));
        minSize = Objects.requireNonNull(in.readParcelable(Size.class.getClassLoader()));
        min = in.readInt();
        trueTitle = in.readString();
        title = in.readString();
        directory = new File(Objects.requireNonNull(in.readString()));
        hasAdvancedData = in.readByte() == 1;
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

    @NonNull
    private File[] retrieveValidImages() {
        File[] files = directory.listFiles((dir, name) -> FILE_PATTERN.matcher(name).matches());
        return files == null ? new File[0] : files;
    }

    private int oldReadId() {
        File nomedia = new File(directory, ".nomedia");
        if (!nomedia.exists()) return SpecialTagIds.INVALID_ID;
        try (BufferedReader br = new BufferedReader(new FileReader(nomedia))) {//ID check with nomedia
            return Integer.parseInt(br.readLine());
        } catch (IOException | NumberFormatException ignore) {
        }
        return SpecialTagIds.INVALID_ID;
    }

    private int readIdFile() {
        File[] files = directory.listFiles((dir, name) -> IDFILE_PATTERN.matcher(name).matches());
        if (files == null || files.length == 0) return -1;
        return Integer.parseInt(files[0].getName().substring(1));
    }

    @NonNull
    private GalleryData readGalleryData() {
        File nomedia = new File(directory, ".nomedia");
        try (JsonReader reader = new JsonReader(new FileReader(nomedia))) {
            return new GalleryData(reader);
        } catch (Exception ignore) {
        }
        hasAdvancedData = false;
        return GalleryData.fakeData();
    }

    public void calculateSizes() {
        for (File f : retrieveValidImages())
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
        return id;
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
        return min;
    }

    @Nullable
    @Override
    public String getPageURI(int page) {
        File p = getPage(page);
        return p == null ? null : p.getAbsolutePath();
    }

    @NonNull
    public File getDirectory() {
        return directory;
    }

    @Nullable
    public File getPage(int index) {
        return getPage(directory, index);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeParcelable(galleryData, flags);
        dest.writeParcelable(maxSize, flags);
        dest.writeParcelable(minSize, flags);
        dest.writeInt(min);
        dest.writeString(trueTitle);
        dest.writeString(title);
        dest.writeString(directory.getAbsolutePath());
        dest.writeByte((byte) (hasAdvancedData ? 1 : 0));
    }

    @Override
    public String getUri(File directory, int page) {
        return super.getUri(directory, page);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalGallery gallery = (LocalGallery) o;

        return directory.equals(gallery.directory);
    }

    @Override
    public int hashCode() {
        return directory.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "LocalGallery{" +
                "galleryData=" + galleryData +
                ", min=" + min +
                ", title='" + title + '\'' +
                ", directory=" + directory +
                ", valid=" + valid +
                ", maxSize=" + maxSize +
                ", minSize=" + minSize +
                '}';
    }
}
