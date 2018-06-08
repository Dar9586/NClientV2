package com.dar.nclientv2.api.local;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.dar.nclientv2.LocalActivity;
import com.dar.nclientv2.adapters.LocalAdapter;
import com.dar.nclientv2.settings.Global;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class FakeInspector extends AsyncTask<LocalActivity,LocalActivity,LocalActivity>{
    private final ArrayList<LocalGallery> galleries;
    private final ArrayList<String> invalidPaths;
    public FakeInspector(){
        galleries=new ArrayList<>();
        invalidPaths=new ArrayList<>();
    }


    @Override
    protected LocalActivity doInBackground(LocalActivity... voids) {
        publishProgress(voids[0]);
        File parent=new File(Environment.getExternalStorageDirectory(),"NClientV2");
        parent.mkdirs();
        File[] directories=parent.listFiles();
        for (File f:directories)createGallery(f);
        for (String x:invalidPaths) Log.d(Global.LOGTAG,"Invalid path: "+x);
        return voids[0];
    }

    @Override
    protected void onProgressUpdate(LocalActivity... values) {
        values[0].getRefresher().setRefreshing(true);
    }

    @Override
    protected void onPostExecute(LocalActivity aVoid) {
        aVoid.getRefresher().setRefreshing(false);
        aVoid.getRecycler().setAdapter(new LocalAdapter(aVoid,galleries));
    }

    private void createGallery(final File file) {
        try {//controllo l'id tramite il file nomedia
            BufferedReader br = new BufferedReader(new FileReader(new File(file,".nomedia")));
            String h = br.readLine();
            br.close();
            LocalGallery lg=new LocalGallery(file,Integer.parseInt(h));
            if(lg.isValid()) galleries.add(lg);
            else invalidPaths.add(file.getAbsolutePath());
        } catch (Exception e) {
            invalidPaths.add(file.getAbsolutePath());
            e.printStackTrace();
        }
    }
}
