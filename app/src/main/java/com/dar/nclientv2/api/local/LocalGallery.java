package com.dar.nclientv2.api.local;

import android.content.Context;
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
import com.dar.nclientv2.utility.files.FileObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class LocalGallery extends GenericGallery{
    private static final Pattern FILE_PATTERN=Pattern.compile("^(\\d{3,9})\\.(gif|png|jpg)$",Pattern.CASE_INSENSITIVE);

    @NonNull private final GalleryData galleryData;
    private final int min;
    private final String title;
    @NonNull private final FileObject directory;
    private final boolean valid;
    private boolean hasAdvancedData=true;
    @NonNull
    private Size maxSize=new Size(0,0),minSize=new Size(Integer.MAX_VALUE,Integer.MAX_VALUE);

    @NonNull
    private List<FileObject> retrieveValidImages(){
        FileObject[] files=directory.listFiles();// directory.listFiles((dir, name) -> FILE_PATTERN.matcher(name).matches());
        List<FileObject>f=new ArrayList<>(files.length);
        for(FileObject obj:files){
            if(FILE_PATTERN.matcher(obj.getName()).matches())
                f.add(obj);
        }
        return f;
    }
    private static int getPageFromFile(FileObject f){
        String n=f.getName();
        return Integer.parseInt(n.substring(0,n.indexOf('.')));
    }

    private int oldReadId(Context context){
        FileObject nomedia= directory.getChildFile(".nomedia");
        if(nomedia==null||!nomedia.exists())return SpecialTagIds.INVALID_ID;
        try (BufferedReader br = new BufferedReader(nomedia.getReader(context))){//ID check with nomedia
            return Integer.parseInt(br.readLine());
        }catch (IOException|NumberFormatException ignore){}
        return SpecialTagIds.INVALID_ID;
    }
    public LocalGallery(Context context, @NonNull FileObject file, boolean jumpDataRetrieve){
        directory=file;
        title=file.getName();
        if(jumpDataRetrieve){
            galleryData=GalleryData.fakeData();
        }else {
            galleryData = readGalleryData(context);
            if (galleryData.getId() == SpecialTagIds.INVALID_ID)
                galleryData.setId(oldReadId(context));
        }
        int max=0,min=Integer.MAX_VALUE;
        //Start search pages
        long time1,time2,time3=0,time4=0;
        List<FileObject> files= retrieveValidImages();
        //Find page with max number
        if(files.size() >= 1) {
            //Collections.sort(files, (o1, o2) -> getPageFromFile(o1)-getPageFromFile(o2));
            min=getPageFromFile(files.get(0));
            max=getPageFromFile(files.get(files.size()-1));
        }
        galleryData.setPageCount(max);
        this.min=min;
        valid=files.size()>0;
    }

    public LocalGallery(Context context, @NonNull FileObject file){
        this(context, file,false);
    }
    @NonNull
    private GalleryData readGalleryData(Context context){
        FileObject nomedia = directory.getChildFile( ".nomedia");
        try (JsonReader reader = new JsonReader(nomedia.getReader(context))) {
            return new GalleryData(reader);
        } catch (Exception ignore) {}
        hasAdvancedData=false;
        return GalleryData.fakeData();
    }

    public void calculateSizes(Context context){
        for(FileObject f: retrieveValidImages())
            checkSize(context, f);
    }
    private void checkSize(Context context, FileObject f){
        LogUtility.d("Decoding: "+f);
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        try {
            InputStream stream = f.getInputStream(context);
            BitmapFactory.decodeStream(stream,null,options);
            if(options.outWidth>maxSize.getWidth())maxSize.setWidth(options.outWidth);
            if(options.outWidth<minSize.getWidth())minSize.setWidth(options.outWidth);
            if(options.outHeight>maxSize.getHeight())maxSize.setHeight(options.outHeight);
            if(options.outHeight<minSize.getHeight())minSize.setHeight(options.outHeight);
        }catch (IOException ignore){}
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

    private LocalGallery(Parcel in) {
        galleryData= Objects.requireNonNull(in.readParcelable(GalleryData.class.getClassLoader()));
        maxSize= Objects.requireNonNull(in.readParcelable(Size.class.getClassLoader()));
        minSize= Objects.requireNonNull(in.readParcelable(Size.class.getClassLoader()));
        min = in.readInt();
        title = in.readString();
        directory= in.readParcelable(FileObject.class.getClassLoader());
        hasAdvancedData=in.readByte()==1;
        valid=true;
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
        return galleryData.getId();
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
        FileObject p=getPage(page);
        return p==null?null:p.getUri().toString();
    }

    @NonNull
    public FileObject getDirectory() {
        return directory;
    }
    /**
     * @return null if not found or the file if found
     * */
    public static FileObject getPage(FileObject dir,int page){
        if(dir==null||!dir.exists())return null;
        String pag=String.format(Locale.US,"%03d.",page);
        FileObject x;
        x= dir.getChildFile(pag+"jpg");
        if(x!=null)return x;
        x= dir.getChildFile(pag+"png");
        if(x!=null)return x;
        x= dir.getChildFile(pag+"gif");
        if(x!=null)return x;

        return null;
    }
    @Nullable
    public FileObject getPage(int index){
        return getPage(directory,index);
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
        dest.writeInt(min);
        dest.writeString(title);
        dest.writeParcelable(directory,flags);
        dest.writeByte((byte) (hasAdvancedData?1:0));
    }

    @Override
    public String getUri(FileObject directory, int page) {
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
