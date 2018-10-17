package com.dar.nclientv2.async;

import android.os.AsyncTask;
import android.util.Log;

import com.dar.nclientv2.FavoriteActivity;
import com.dar.nclientv2.adapters.FavoriteAdapter;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;

import java.io.IOException;
import java.util.List;

public class LoadFavorite extends AsyncTask<FavoriteActivity,Gallery,FavoriteActivity> {
    private final FavoriteAdapter adapter;

    public LoadFavorite(FavoriteAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    protected void onPreExecute() {
        adapter.clearGallery();
    }

    @Override
    protected FavoriteActivity doInBackground(FavoriteActivity... voids) {
        FavoriteActivity activity=voids[0];
        List<Gallery> x;
        try {
            x = Queries.GalleryTable.getAllFavorite(Database.getDatabase(),false);
            Log.i(Global.LOGTAG,"SIZE:"+x.size());
            for(Gallery y:x)publishProgress(y);
        }catch (IOException e){
            Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
        }
        return activity;
    }

    @Override
    protected void onProgressUpdate(Gallery... values){
        Log.d(Global.LOGTAG,"G:"+values[0]);
        adapter.addGallery(values[0]);
    }

    @Override
    protected void onPostExecute(FavoriteActivity aVoid) {
        aVoid.getRefresher().setRefreshing(false);
        aVoid.getRefresher().setEnabled(false);
    }
}
