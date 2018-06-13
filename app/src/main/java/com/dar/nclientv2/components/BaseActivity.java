package com.dar.nclientv2.components;

import android.content.res.Configuration;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

public abstract class BaseActivity extends AppCompatActivity {
    protected RecyclerView recycler;
    protected SwipeRefreshLayout refresher;

    public SwipeRefreshLayout getRefresher() {
        return refresher;
    }

    public RecyclerView getRecycler() {
        return recycler;
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            changeLayout(true);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            changeLayout(false);
        }
    }
    protected void changeLayout(boolean landscape){
        final int count=landscape?4:2;
        RecyclerView.Adapter adapter=recycler.getAdapter();
        GridLayoutManager gridLayoutManager=new GridLayoutManager(this,count);
        recycler.setLayoutManager(gridLayoutManager);
        recycler.setAdapter(adapter);
    }
}
