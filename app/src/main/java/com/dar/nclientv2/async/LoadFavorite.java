package com.dar.nclientv2.async;

import android.os.AsyncTask;
import android.util.Log;

import com.dar.nclientv2.FavoriteActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.adapters.FavoriteAdapter;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.settings.Global;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class LoadFavorite extends AsyncTask<FavoriteActivity,Gallery,FavoriteActivity> {
    FavoriteAdapter adapter;

    public LoadFavorite(FavoriteAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    protected FavoriteActivity doInBackground(FavoriteActivity... voids) {
        FavoriteActivity activity=voids[0];
        Set<String> x=activity.getSharedPreferences("FavoriteList", 0).getStringSet(activity.getString(R.string.key_favorite_list),new HashSet<String>());
        try {
            for(String y:x)publishProgress(new Gallery(y));
        }catch (IOException e){
            Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
        }
        return activity;
    }

    @Override
    protected void onProgressUpdate(Gallery... values) {
        adapter.addGallery(values[0]);
    }

    @Override
    protected void onPostExecute(FavoriteActivity aVoid) {
        aVoid.getRefresher().setRefreshing(false);
        aVoid.getRefresher().setEnabled(false);
    }
}
