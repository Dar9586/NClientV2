package com.dar.nclientv2.api.local;

import android.os.AsyncTask;

import com.dar.nclientv2.LocalActivity;
import com.dar.nclientv2.adapters.LocalAdapter;
import com.dar.nclientv2.utility.LogUtility;

import java.io.File;
import java.util.ArrayList;

public class FakeInspector extends AsyncTask<LocalActivity,LocalActivity,LocalActivity>{
    private final ArrayList<LocalGallery> galleries;
    private final ArrayList<String> invalidPaths;
    private final File folder;
    public FakeInspector(File folder){
        this.folder=new File(folder,"Download");
        galleries= new ArrayList<>();
        invalidPaths=new ArrayList<>();
    }


    @Override
    protected LocalActivity doInBackground(LocalActivity... voids) {
        if(!this.folder.exists())return voids[0];
        publishProgress(voids[0]);
        File parent=this.folder;
        parent.mkdirs();
        File[]files=parent.listFiles();
        if(files==null)return voids[0];
        for (File f:files)createGallery(f);
        for (String x:invalidPaths) LogUtility.d("Invalid path: "+x);
        return voids[0];
    }

    @Override
    protected void onProgressUpdate(LocalActivity... values) {
        values[0].getRefresher().setRefreshing(true);
    }

    @Override
    protected void onPostExecute(LocalActivity aVoid) {
        aVoid.getRefresher().setRefreshing(false);
        aVoid.setAdapter(new LocalAdapter(aVoid,galleries));
    }

    private void createGallery(final File file) {
        LocalGallery lg=new LocalGallery(file,true);
        if(lg.isValid()) {
            galleries.add(lg);
        } else {
            LogUtility.e(lg);
            invalidPaths.add(file.getAbsolutePath());
        }
    }
}
