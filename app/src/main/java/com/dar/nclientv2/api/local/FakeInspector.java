package com.dar.nclientv2.api.local;

import android.os.AsyncTask;

import com.dar.nclientv2.LocalActivity;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.files.FileObject;
import com.dar.nclientv2.utility.files.MasterFileManager;

import java.util.ArrayList;

public class FakeInspector extends AsyncTask<LocalActivity,Object,LocalActivity>{
    private final ArrayList<LocalGallery> galleries;
    private final ArrayList<String> invalidPaths;
    public FakeInspector(){
        galleries= new ArrayList<>();
        invalidPaths=new ArrayList<>();
    }


    @Override
    protected LocalActivity doInBackground(LocalActivity... voids) {
        publishProgress(voids[0],null);
        FileObject parent= MasterFileManager.getDownloadFolder();
        FileObject[]files=parent.listFiles();
        for (FileObject f:files){
            LocalGallery gallery=createGallery(voids[0],f);
            if(gallery!=null)
                publishProgress(voids[0],gallery);
        }
        for (String x:invalidPaths) LogUtility.d("Invalid path: "+x);
        return voids[0];
    }

    @Override
    protected void onProgressUpdate(Object... values) {
        LocalActivity activity=(LocalActivity) values[0];
        activity.getRefresher().setRefreshing(true);
        if(values[1]==null)return;
        LocalGallery gallery=(LocalGallery) values[1];
        activity.addGallery(gallery);
    }

    @Override
    protected void onPostExecute(LocalActivity aVoid) {
        aVoid.getRefresher().setRefreshing(false);
    }

    private LocalGallery createGallery(LocalActivity aVoid, final FileObject file) {
        LocalGallery lg=new LocalGallery(aVoid,file,true);
        if(lg.isValid()) {
            galleries.add(lg);
            return lg;
        } else {
            LogUtility.e(lg);
            invalidPaths.add(file.toString());
        }
        return null;
    }
}
