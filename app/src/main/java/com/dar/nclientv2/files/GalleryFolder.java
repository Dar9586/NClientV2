package com.dar.nclientv2.files;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;

import com.dar.nclientv2.api.components.Page;
import com.dar.nclientv2.api.enums.ImageExt;
import com.dar.nclientv2.api.enums.SpecialTagIds;
import com.dar.nclientv2.settings.Global;

import java.io.File;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryFolder implements Parcelable,Iterable<PageFile> {

    private static final Pattern FILE_PATTERN=Pattern.compile("^0*(\\d{1,9})\\.(gif|png|jpg)$",Pattern.CASE_INSENSITIVE);
    private static final Pattern IDFILE_PATTERN=Pattern.compile("^\\.(\\d{1,6})$");
    private static final String NOMEDIA_FILE=".nomedia";

    private final SparseArrayCompat<PageFile> pageArray= new SparseArrayCompat<>();
    private final File folder;
    private int id=SpecialTagIds.INVALID_ID;
    private int max=-1;
    private int min=Integer.MAX_VALUE;
    private File nomedia;

    public GalleryFolder(@NonNull String child) {
        this(Global.DOWNLOADFOLDER, child);
    }

    public GalleryFolder(@Nullable File parent, @NonNull String child) {
        this(new File(parent, child));

    }

    public GalleryFolder(File file) {
        folder=file;
        if(!folder.isDirectory())
            throw new IllegalArgumentException("File is not a folder");
        parseFiles();
    }
    public static @Nullable GalleryFolder fromId(@Nullable Context context, int id){
        File f=Global.findGalleryFolder(context,id);
        if(f==null)return null;
        return new GalleryFolder(f);
    }


    private void parseFiles() {
        File[]files=folder.listFiles();
        if(files==null)return;
        for(File f:files){
            elaborateFile(f);
        }
    }
    private void elaborateFile(File f){
        String name=f.getName();

        Matcher matcher=FILE_PATTERN.matcher(name);
        if(matcher.matches())elaboratePage(f,matcher);

        if(id == SpecialTagIds.INVALID_ID) {
            matcher = IDFILE_PATTERN.matcher(name);
            if (matcher.matches()) id = elaborateId(matcher);
        }

        if(nomedia==null && name.equals(NOMEDIA_FILE))nomedia=f;
    }

    private int elaborateId(Matcher matcher) {
        return Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
    }
    public int getPageCount(){
        return pageArray.size();
    }

    public File getGalleryDataFile() {
        return nomedia;
    }

    public PageFile[] getPages(){
        PageFile[]files=new PageFile[pageArray.size()];
        for(int i=0;i<pageArray.size();i++){
            files[i]=pageArray.valueAt(i);
        }
        return files;
    }

    public File getFolder() {
        return folder;
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    private void elaboratePage(File f, Matcher matcher) {
        int page=Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
        ImageExt ext= Page.charToExt(Objects.requireNonNull(matcher.group(2)).charAt(0));
        pageArray.append(page,new PageFile(ext,f,page));
        if(page>max)max=page;
        if(page<min)min=page;
    }

    public PageFile getPage(int page){
        return pageArray.get(page);
    }

    public static final Creator<GalleryFolder> CREATOR = new Creator<GalleryFolder>() {
        @Override
        public GalleryFolder createFromParcel(Parcel in) {
            return new GalleryFolder(in);
        }

        @Override
        public GalleryFolder[] newArray(int size) {
            return new GalleryFolder[size];
        }
    };

    public int getId() {
        return id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(folder.getAbsolutePath());
        dest.writeInt(id);
        dest.writeInt(min);
        dest.writeInt(max);
        dest.writeInt(pageArray.size());
        for(int i=0;i<pageArray.size();i++){
            dest.writeInt(pageArray.keyAt(i));
            dest.writeParcelable(pageArray.valueAt(i), flags);
        }
    }
    protected GalleryFolder(Parcel in) {
        folder=new File(Objects.requireNonNull(in.readString()));
        id = in.readInt();
        min = in.readInt();
        max = in.readInt();
        int pageCount = in.readInt();
        for(int i=0;i<pageCount;i++){
            int k=in.readInt();
            PageFile f=in.readParcelable(PageFile.class.getClassLoader());
            pageArray.put(k,f);
        }
    }

    public static class PageFileIterator implements Iterator<PageFile>{
        private final SparseArrayCompat<PageFile>files;
        private int reach=0;

        public PageFileIterator(SparseArrayCompat<PageFile> files) {
            this.files = files;
        }


        @Override
        public boolean hasNext() {
            return reach<files.size();
        }

        @Override
        public PageFile next() {
            PageFile f=files.valueAt(reach);
            reach++;
            return f;
        }
    }

    @NonNull
    @Override
    public Iterator<PageFile> iterator() {
        return new PageFileIterator(pageArray);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GalleryFolder pageFiles = (GalleryFolder) o;

        return folder.equals(pageFiles.folder);
    }

    @Override
    public int hashCode() {
        return folder.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "GalleryFolder{" +
                "pageArray=" + pageArray +
                ", folder=" + folder +
                ", id=" + id +
                ", max=" + max +
                ", min=" + min +
                ", nomedia=" + nomedia +
                '}';
    }
}
