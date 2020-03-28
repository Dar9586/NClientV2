package com.dar.nclientv2.api.local;

import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.dar.nclientv2.api.components.Comment;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.components.classes.Size;
import com.dar.nclientv2.utility.LogUtility;

import java.io.File;
import java.util.List;

public class LocalGallery extends GenericGallery{
    private final int id,min,max;
    private final String title;
    private final File directory;
    private final boolean valid;
    private Size maxSize=new Size(0,0),minSize=new Size(Integer.MAX_VALUE,Integer.MAX_VALUE);
    public LocalGallery(File file, int id){
        directory=file;
        title=file.getName();
        this.id=id;
        int max=0,min=9999;
        SparseArray<Size>sizes=new SparseArray<>();
        //Inizio ricerca pagine
        File[] files=file.listFiles((dir, name) -> (name.endsWith(".jpg")||name.endsWith(".png")||name.endsWith(".gif"))&&name.length()==7);
        //Find page with max number
        if(files!=null) {
            if (files.length < 1) LogUtility.e( "FILE INESISTENTI");
            for (File f : files) {
                try {
                    int x = Integer.parseInt(f.getName().substring(0, 3));
                    if (x > max) max = x;
                    if (x < min) min = x;
                    checkSize(x,sizes,f);
                } catch (NumberFormatException e) {
                    LogUtility.e( e.getLocalizedMessage(), e);
                }
            }
        }
        this.max=max;
        this.min=min;
        valid=max<9999&&min>0&&id>0;
    }
    private void checkSize(int x, SparseArray<Size> sizes, File f){
        LogUtility.d("Decoding: "+f);
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(f.getAbsolutePath(),options);
        if(options.outWidth>maxSize.getWidth())maxSize.setWidth(options.outWidth);
        if(options.outWidth<minSize.getWidth())minSize.setWidth(options.outWidth);
        if(options.outHeight>maxSize.getHeight())maxSize.setHeight(options.outHeight);
        if(options.outHeight<minSize.getHeight())minSize.setHeight(options.outHeight);
        sizes.append(x,new Size(options.outWidth,options.outHeight));
    }
    @Override
    public List<Comment> getComments() {
        return null;
    }
    @Override
    public Size getMaxSize() {
        return maxSize;
    }

    @Override
    public Size getMinSize() {
        return minSize;
    }

    private LocalGallery(Parcel in) {
        maxSize=in.readParcelable(Size.class.getClassLoader());
        minSize=in.readParcelable(Size.class.getClassLoader());
        id = in.readInt();
        min = in.readInt();
        max = in.readInt();
        title = in.readString();
        directory=new File(in.readString());
        valid=true;
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
    public String getThumbnail() {
        return null;
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
        return max;
    }
    @Override
    public String getTitle() {
        return title;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public File getDirectory() {
        return directory;
    }
    public static File getPage(File dir,int page){
        if(dir==null)return null;
        File x;
        x=new File(dir,("000"+page+".jpg").substring(Integer.toString(page).length()));
        if(x.exists())return x;
        x=new File(dir,("000"+page+".png").substring(Integer.toString(page).length()));
        if(x.exists())return x;
        x=new File(dir,("000"+page+".gif").substring(Integer.toString(page).length()));
        if(x.exists())return x;
        return null;
    }
    @Nullable
    public File getPage(int index){
        return getPage(directory,index);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(maxSize, flags);
        dest.writeParcelable(minSize, flags);
        dest.writeInt(id);
        dest.writeInt(min);
        dest.writeInt(max);
        dest.writeString(title);
        dest.writeString(directory.getAbsolutePath());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalGallery gallery = (LocalGallery) o;

        if (id != gallery.id) return false;
        if (!title.equals(gallery.title)) return false;
        return directory.equals(gallery.directory);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + title.hashCode();
        result = 31 * result + directory.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "LocalGallery{" +
                "id=" + id +
                ", min=" + min +
                ", max=" + max +
                ", title='" + title + '\'' +
                ", directory=" + directory +
                ", valid=" + valid +
                '}';
    }
}
