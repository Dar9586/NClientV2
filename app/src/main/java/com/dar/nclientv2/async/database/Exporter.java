package com.dar.nclientv2.async.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Exporter {
    private static final String DB_PATH="/data/data/%s/databases/"+DatabaseHelper.DATABASE_NAME;
    private static final String SETTINGS_PATH="/data/data/%s/shared_prefs/Settings.xml";
    private static final String DB_ZIP_FILE="Database.db";
    private static final String SETTINGS_ZIP_FILE="Settings.xml";

    private static String getDbPath(Context context){
        return String.format(Locale.US,DB_PATH, context.getPackageName());
    }

    public static String getSettingsPath(Context context) {
        return String.format(Locale.US,SETTINGS_PATH, context.getPackageName());
    }

    public static void duplicateDB(Context context, OutputStream outputStream)throws IOException {
        duplicateFile(getDbPath(context),outputStream);
    }
    private static void duplicateSettings(Context context, OutputStream outputStream)throws IOException {
        duplicateFile(getSettingsPath(context),outputStream);
    }
    private static void duplicateFile(InputStream inputStream,String path)throws IOException{
        FileOutputStream outputStream=new FileOutputStream(new File(path));
        duplicateFile(inputStream,outputStream);
        outputStream.close();
    }
    private static void duplicateFile(String path,OutputStream outputStream)throws IOException{
        FileInputStream inputStream=new FileInputStream(new File(path));
        duplicateFile(inputStream,outputStream);
        inputStream.close();
    }
    private static void duplicateFile(InputStream inputStream,OutputStream outputStream)throws IOException{
        byte[]buffer=new byte[2048];
        int read;
        while ((read=inputStream.read(buffer))!=-1)
            outputStream.write(buffer,0,read);
        outputStream.flush();
    }
    public static String exportData(Context context)throws IOException{
        Date actualTime=new Date();
        String date= DateFormat.getDateFormat(context).format(actualTime).replaceAll("[^0-9]*","");
        String time= DateFormat.getTimeFormat(context).format(actualTime).replaceAll("[^0-9]*","");
        String filename= String.format("Backup_%s_%s.zip", date, time);
        File file=new File(Global.BACKUPFOLDER,filename);
        FileOutputStream outputStream=new FileOutputStream(file);
        ZipOutputStream zip=new ZipOutputStream(outputStream);
        zip.setLevel(Deflater.BEST_COMPRESSION);
        ZipEntry dbEntry=new ZipEntry(DB_ZIP_FILE);
        ZipEntry sharedEntry=new ZipEntry(SETTINGS_ZIP_FILE);

        zip.putNextEntry(dbEntry);
        duplicateDB(context, zip);
        zip.closeEntry();

        zip.putNextEntry(sharedEntry);
        duplicateSettings(context, zip);
        zip.closeEntry();

        zip.close();
        return file.getAbsolutePath();
    }

    public static void importData(@NonNull Context context, InputStream stream)throws IOException{
        ZipInputStream inputStream=new ZipInputStream(stream);
        ZipEntry entry;
        while ((entry=inputStream.getNextEntry())!=null){
            String name=entry.getName();
            switch (name){
                case DB_ZIP_FILE:
                    Database.getDatabase().close();
                    duplicateFile(inputStream,getDbPath(context));
                    Database.setDatabase(new DatabaseHelper(context.getApplicationContext()).getWritableDatabase());
                    break;
                case SETTINGS_ZIP_FILE:
                    duplicateFile(inputStream,getSettingsPath(context));
                    //duplicateShared(context,inputStream);
                    break;
            }
            inputStream.closeEntry();
        }
        inputStream.close();
    }

    private static void duplicateShared(Context context, ZipInputStream inputStream)throws IOException {
        File tmpFile=File.createTempFile("Import_",".xml");
        duplicateFile(inputStream,tmpFile.getAbsolutePath());
        SharedPreferences.Editor settings=context.getSharedPreferences("Settings",Context.MODE_PRIVATE).edit();
        SharedPreferences impor=context.getSharedPreferences(tmpFile.getAbsolutePath(),Context.MODE_PRIVATE);
        for(Map.Entry<String, ?> entry:impor.getAll().entrySet()){
            Object clas=entry.getValue();
            if(clas instanceof Long)settings.putLong(entry.getKey(),(Long) entry.getValue());
            else if(clas instanceof Integer)settings.putLong(entry.getKey(),(Integer) entry.getValue());
            else if(clas instanceof String)settings.putString(entry.getKey(),(String) entry.getValue());
            else if(clas instanceof Boolean)settings.putBoolean(entry.getKey(),(Boolean) entry.getValue());
            else if(clas instanceof Float)settings.putFloat(entry.getKey(),(Float) entry.getValue());
            else settings.putStringSet(entry.getKey(),(Set<String>) entry.getValue());
        }

        settings.commit();
        tmpFile.delete();

    }

}
