package com.dar.nclientv2.utility.files;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.dar.nclientv2.utility.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class FileObject implements Parcelable {
    private static SparseArray<DocumentFile>filesToParcel=new SparseArray<>();
    private final DocumentFile document;
    private final File file;
    private final boolean isFile;
    FileObject(Object file) {
        if(file instanceof FileObject){
            FileObject obj=(FileObject) file;
            this.isFile=obj.isFile;
            this.document=obj.document;
            this.file=obj.file;
            return;
        }
        isFile=file instanceof File;
        if(isFile){
            this.file=(File)file;
            this.document=null;
        } else {
            this.document=(DocumentFile) file;
            this.file=null;
        }
    }

    /*Getters*/
    public File toFile() {
        return file;
    }
    public DocumentFile toDocument() {
        return document;
    }
    public boolean useFile(){
        return isFile;
    }


    /*I/O*/

    public InputStream getInputStream(Context context)throws IOException{
        if(isFile)return new FileInputStream(toFile());
        return context.getContentResolver().openInputStream(toDocument().getUri());
    }

    public OutputStream getOutputStream(Context context)throws IOException{
        if(isFile)return new FileOutputStream(toFile());
        return context.getContentResolver().openOutputStream(toDocument().getUri());
    }
    public Writer getWriter(Context context)throws IOException{
        return new OutputStreamWriter(getOutputStream(context));
    }

    public Reader getReader(Context context)throws IOException{
        return new InputStreamReader(getInputStream(context));
    }


    public FileObject getChildFile(String name){
        if(isFile){
            File f=new File(toFile(),name);
            return f.exists()&&f.isFile()?createFileObject(f):null;
        }
        return createFileObject(DocumentFileUtility.getFile(toDocument(),name));
    }
    public FileObject getChildDirectory(String name){
        if(isFile){
            File f=new File(toFile(),name);
            return f.exists()&&f.isDirectory()?createFileObject(f):null;
        }
        return createFileObject(DocumentFileUtility.getFolder(toDocument(),name));
    }

    @Nullable
    public FileObject createFile(String name) {
        if(isFile){
            File obj=new File(toFile(),name);
            try {
                obj.createNewFile();
                return createFileObject(obj);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        return createFileObject(DocumentFileUtility.createFile(toDocument(),name));
    }
    @Nullable
    public FileObject createDirectory(String name) {
        if(isFile){
            File file=new File(toFile(),name);
            FileObject obj=createFileObject(file);
            file.mkdirs();
            return obj;
        }
        return createFileObject(DocumentFileUtility.getOrCreateFolder(toDocument(),name));
    }
    private static FileObject createFileObject(Object item){
        if(item instanceof FileObject)throw new ClassCastException("Can't reuse a FileObject");
        return item==null?null:new FileObject(item);
    }
    public FileObject getParent(){
        return isFile?createFileObject(toFile().getParent()):createFileObject(toDocument().getParentFile());
    }
    public void copyTo(Context context, FileObject toPosition)throws IOException{
        if(toPosition.isDirectory()){
            String name=toPosition.getName();
            toPosition.getParent().createDirectory(name);
            return;
        }
        InputStream from=getInputStream(context);
        Utility.writeStreamToFile(context,from,toPosition);
        from.close();
    }


    /*Simple methods*/
    public long lastModified(){
        return isFile?toFile().lastModified():toDocument().lastModified();
    }
    public boolean delete(){
        return isFile?toFile().delete():toDocument().delete();
    }
    public boolean isFile(){
        return isFile? toFile().isFile(): toDocument().isFile();
    }
    public boolean isDirectory(){
        return isFile? toFile().isDirectory(): toDocument().isDirectory();
    }
    public String getName(){
        return isFile? toFile().getName(): toDocument().getName();
    }
    public boolean exists(){
        return isFile? toFile().exists(): toDocument().exists();
    }
    public long length(){
        return isFile? toFile().length(): toDocument().length();
    }
    public Uri getUri(){
        return isFile?Uri.fromFile(toFile()): toDocument().getUri();
    }

    public FileObject[] listFiles(){
        Object[]files;
        if(isFile){
            files= toFile().listFiles();
            if(files==null)files=new File[0];
        }else{
            files= toDocument().listFiles();
        }
        FileObject[]objects=new FileObject[files.length];
        int i=0;
        for(Object f:files)
            objects[i++]=createFileObject(f);
        return objects;
    }


    /*Object default*/

    @NonNull
    @Override
    public String toString() {
        return "FileObject{" +
                "document=" + document +
                ", file=" + file +
                ", isFile=" + isFile +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileObject that = (FileObject) o;
        if (isFile != that.isFile) return false;

        if(isFile)return toFile().equals(that.toFile());
        return toDocument().equals(that.toDocument());
    }

    @Override
    public int hashCode() {
        int result = document != null ? document.hashCode() : 0;
        result = 31 * result + (file != null ? file.hashCode() : 0);
        result = 31 * result + (isFile ? 1 : 0);
        return result;
    }


    /*Parcel*/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isFile ? 1 : 0));
        if(isFile){
            dest.writeString(toFile().getAbsolutePath());
        }else{
            int hash=hashCode();
            dest.writeInt(hash);
            filesToParcel.append(hash, toDocument());
        }
    }

    protected FileObject(Parcel in) {
        isFile = in.readByte() != 0;
        if(isFile){
            file=new File(in.readString());
            document=null;
        }else{
            int hash=in.readInt();
            document=filesToParcel.get(hash);
            filesToParcel.delete(hash);
            file=null;
        }
    }

    public static final Creator<FileObject> CREATOR = new Creator<FileObject>() {
        @Override
        public FileObject createFromParcel(Parcel in) {
            return new FileObject(in);
        }

        @Override
        public FileObject[] newArray(int size) {
            return new FileObject[size];
        }
    };
}


