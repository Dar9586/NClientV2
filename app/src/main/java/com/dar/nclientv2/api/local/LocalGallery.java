package com.dar.nclientv2.api.local;

import android.os.Parcel;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.settings.Global;

import java.io.File;
import java.io.FilenameFilter;

public class LocalGallery extends GenericGallery{
    private final int id,min,max;
    private final String title;
    private final File directory;
    private final boolean valid;
    public LocalGallery(File file, int id){
        directory=file;
        title=file.getName();
        this.id=id;
        int max=0,min=9999;
        //Inizio ricerca pagine
        File[] files=file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {return name.endsWith(".jpg")&&name.length()==7;}
        });
        //trova la pagina col numero piu grande
        if(files.length<1) Log.e(Global.LOGTAG,"FILE INESISTENTI");
        for(File f:files){
            try {
                int x = Integer.parseInt(f.getName().substring(0, 3));
                if(x>max)max=x;
                if(x<min)min=x;
            }catch (NumberFormatException e){Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);}
        }

        this.max=max;
        this.min=min;
        valid=max<1000&&min>0&&id!=-1;
    }

    private LocalGallery(Parcel in) {
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
    public boolean isLocal() {
        return true;
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
        return max-min+1;
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
    @Nullable
    public File getPage(int index){
        File x=new File(directory,("000"+index+".jpg").substring(Integer.toString(index).length()));
        return x.exists()?x:null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
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
