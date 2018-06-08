package com.dar.nclientv2.async;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DownloadPage extends Thread {
    private final InputStream input;
    private final File file;
    public DownloadPage(InputStream input, File file){
        this.input=input;
        this.file=file;
    }


    @Override
    public void run() {
        try {
            file.createNewFile();
            Bitmap bitmap= BitmapFactory.decodeStream(input);
            FileOutputStream ostream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
            ostream.flush();
            ostream.close();
        } catch (IOException e) {
            Log.e("IOException", e.getLocalizedMessage()); }
        }

}
