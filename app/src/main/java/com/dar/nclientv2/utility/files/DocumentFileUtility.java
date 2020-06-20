package com.dar.nclientv2.utility.files;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

class DocumentFileUtility {
    public static final int ACQUIRED_STORAGE_REQUEST_CODE=9;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static DocumentFile makePersistentStorage(Context context, Uri uri){
        DocumentFile file = DocumentFile.fromTreeUri(context, uri);
        context.grantUriPermission(context.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return file;
    }

    /**
     * Get a folder with a given name, doesn't create it
     * @return null if the directory doesn't exits, the folder otherwise
     * */
    @Nullable
    public static DocumentFile getFolder(DocumentFile parent,String name){
        DocumentFile file=parent.findFile(name);
        if(file==null)return null;
        return file.isDirectory()?file:null;
    }
    /**
     * Return the current folder or create a new folder if not exists
     * */
    public static DocumentFile getOrCreateFolder(DocumentFile parent,String name){
        DocumentFile documentFile=getFolder(parent, name);
        if(documentFile==null)
            return parent.createDirectory(name);
        return documentFile;
    }

    public static DocumentFile getFile(DocumentFile parent, String name){
        DocumentFile file=parent.findFile(name);
        if(file==null)return null;
        return file.isFile()?file:null;
    }
    @Nullable
    public static DocumentFile createFile(DocumentFile parent,String name){
        DocumentFile file=getFile(parent, name);
        if(file!=null)return file;
        file = parent.createFile("text/plain",name);
        if(file==null)return null;
        if(file.renameTo(name))return file;
        file.delete();
        return null;
    }
    public static OutputStream writeToFile(Context context, DocumentFile file)throws IOException {
        return context.getContentResolver().openOutputStream(file.getUri());
    }

    public static InputStream readFile(Context context, DocumentFile file)throws IOException {
        return context.getContentResolver().openInputStream(file.getUri());
    }

    public static Writer createWriter(Context context,DocumentFile file) throws IOException{
        return new OutputStreamWriter(writeToFile(context,file));
    }

    public static Reader createReader(Context context, DocumentFile file) throws IOException{
        return new InputStreamReader(readFile(context,file));
    }




    public static void acquiredStoragePermission(Activity activity) {
        File mainFolder;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){

            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivityForResult(i,ACQUIRED_STORAGE_REQUEST_CODE);
        }else{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            mainFolder=new File(Environment.getExternalStorageDirectory(),"NClientV2");
            MasterFileManager.generateFolderStructure(mainFolder);
        }

    }
}
/*
private static void initFilesTree(Context context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            MAINFOLDER=new File(context.getExternalFilesDir(null),"NClientV2");
        }else{
            MAINFOLDER=new File(Environment.getExternalStorageDirectory(),"NClientV2");
        }
        OLD_GALLERYFOLDER=new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"NClientV2");
        DOWNLOADFOLDER=new File(MAINFOLDER,"Download");
        SCREENFOLDER =new File(MAINFOLDER,"Screen");
        PDFFOLDER =new File(MAINFOLDER,"PDF");
        UPDATEFOLDER =new File(MAINFOLDER,"Update");
        ZIPFOLDER =new File(MAINFOLDER,"ZIP");
    }
* */
