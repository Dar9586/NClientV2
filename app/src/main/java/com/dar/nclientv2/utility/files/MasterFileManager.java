package com.dar.nclientv2.utility.files;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class MasterFileManager {
    public static final int ACQUIRED_STORAGE_REQUEST_CODE=9;
    private static FileObject MAIN_FOLDER=null;
    private static FileObject DOWNLOADFOLDER=null;
    private static FileObject SCREENFOLDER =null;
    private static FileObject PDFFOLDER =null;
    private static FileObject UPDATEFOLDER =null;
    private static FileObject ZIPFOLDER =null;

    public static FileObject getMainFolder() {
        return MAIN_FOLDER;
    }

    public static FileObject getDownloadFolder() {
        return DOWNLOADFOLDER;
    }

    public static FileObject getScreenFolder() {
        return SCREENFOLDER;
    }

    public static FileObject getPdfFolder() {
        return PDFFOLDER;
    }

    public static FileObject getUpdateFolder() {
        return UPDATEFOLDER;
    }

    public static FileObject getZipFolder() {
        return ZIPFOLDER;
    }

    public static void generateFolderStructure(DocumentFile mainStorage) {
        generateFolderStructure((Object)mainStorage);
    }
    public static void generateFolderStructure(File mainStorage) {
        generateFolderStructure((Object) mainStorage);
    }
    private static void generateFolderStructure(Object mainStorage) {
        MasterFileManager.MAIN_FOLDER =    new FileObject(mainStorage).createDirectory( "NClientV2");
        MasterFileManager.DOWNLOADFOLDER = MasterFileManager.MAIN_FOLDER.createDirectory("Download");
        MasterFileManager.SCREENFOLDER =   MasterFileManager.MAIN_FOLDER.createDirectory("Screen");
        MasterFileManager.PDFFOLDER =      MasterFileManager.MAIN_FOLDER.createDirectory("PDF");
        MasterFileManager.UPDATEFOLDER =   MasterFileManager.MAIN_FOLDER.createDirectory("Update");
        MasterFileManager.ZIPFOLDER =      MasterFileManager.MAIN_FOLDER.createDirectory("ZIP");
        MasterFileManager.MAIN_FOLDER.createFile(".nomedia");
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void makePersistentStorage(Context context, Uri uri){
        DocumentFile file = DocumentFileUtility.makePersistentStorage(context, uri);
        changeFolder(context,file,uri);
    }

    public static void acquiredStoragePermission(Activity activity){
        File defaultFile;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            defaultFile=activity.getExternalFilesDir(null);
        }else{
            defaultFile=Environment.getExternalStorageDirectory();
        }
        changeFolderHome(activity,null,new FileObject(defaultFile), null);
        DocumentFileUtility.acquiredStoragePermission(activity);
    }
    public static void changeFolder(Context context,File newFolder){
        changeFolderHome(context,MasterFileManager.MAIN_FOLDER,new FileObject(newFolder), null);
    }
    public static void changeFolder(Context context, DocumentFile newFolder, Uri uri){
        changeFolderHome(context,MasterFileManager.MAIN_FOLDER,new FileObject(newFolder),uri);
    }
    private static void changeFolderHome(Context context, @Nullable FileObject oldPosition, FileObject newPosition, Uri uri) {
        generateFolderStructure(newPosition);
        /*if(oldPosition!=null) {
            new Thread(() -> {
                try {
                    recursiveCopy(context,oldPosition,MAIN_FOLDER);
                    Global.recursiveDelete(oldPosition);
                } catch (IOException e) {
                    LogUtility.e(e.getLocalizedMessage(),e);
                }
            }).start();
        }*/
        writeShared(context,newPosition,uri);

    }

    public static String getAbsolutePath(Context context){
        if(MAIN_FOLDER.useFile())return MAIN_FOLDER.toFile().getAbsolutePath();
        else return extractName(context,MAIN_FOLDER.toDocument().getUri());
    }

    public static String extractName(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void initFromShared(Context context){
        SharedPreferences editor=context.getSharedPreferences("Files",Context.MODE_PRIVATE);
        boolean useFile= editor.getBoolean("use_file",true);
        if(useFile)generateFolderStructure(new File(editor.getString("file_path","")));
        else {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    List<UriPermission>permissions=context.getContentResolver().getPersistedUriPermissions();
                    UriPermission permission=permissions.get(0);
                    for(UriPermission p:permissions){
                        if(p.getPersistedTime()>permission.getPersistedTime())permission=p;
                    }
                    generateFolderStructure(DocumentFile.fromTreeUri(context,permission.getUri()));
                }
            }catch (Exception e){
                generateFolderStructure(new File(editor.getString("file_path","")));
            }
        }
    }
    private static void writeShared(Context context, FileObject position, Uri uri) {
        boolean useFile=position.useFile();
        SharedPreferences.Editor editor=context.getSharedPreferences("Files",Context.MODE_PRIVATE).edit()
                .putBoolean("use_file",position.useFile());
        if(useFile)editor.putString("file_path",position.toFile().getAbsolutePath());
        else editor.putString("file_uri",uri.toString());
        editor.apply();
    }

    /**
     * Copies a folder into another
     * */
    /*oldPosition and newPosition are always folder*/
    private static void recursiveCopy(Context context,FileObject oldPosition,FileObject newPosition)throws IOException {
        for(FileObject object:oldPosition.listFiles()){
            if(object.isDirectory()){
                recursiveCopy(context, object,newPosition.createDirectory(object.getName()));
            }else{
                object.copyTo(context,newPosition.createFile(object.getName()));
            }
            //object.delete();
        }
    }

    public static FileObject getCacheDir(Context context){
        return new FileObject(context.getCacheDir());
    }
}
