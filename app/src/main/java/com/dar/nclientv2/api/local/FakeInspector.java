package com.dar.nclientv2.api.local;

import android.os.AsyncTask;

import com.dar.nclientv2.LocalActivity;
import com.dar.nclientv2.adapters.LocalAdapter;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;

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
        File parent=Global.DOWNLOADFOLDER;
        parent.mkdirs();
        for (File f:parent.listFiles())createGallery(f);
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
        File nomedia=new File(file,".nomedia");
        String h="-1";
        try {//controllo l'id tramite il file nomedia
            if(nomedia.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(new File(file, ".nomedia")));
                h = br.readLine();
                br.close();
            }
            LocalGallery lg=new LocalGallery(file,Integer.parseInt(h));
            if(lg.isValid()) galleries.add(lg);
            else {
                LogUtility.e(lg);
                invalidPaths.add(file.getAbsolutePath());
            }
        } catch (Exception e) {
            LocalGallery lg=new LocalGallery(file,-1);
            if(lg.isValid()) galleries.add(lg);
            else if(lg.getPageCount()>0){
                LogUtility.e(lg);
                invalidPaths.add(file.getAbsolutePath());
            }
            LogUtility.e("WTF: "+e.getLocalizedMessage(),e);
        }
    }
}
