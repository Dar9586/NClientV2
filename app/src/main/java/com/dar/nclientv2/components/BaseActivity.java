package com.dar.nclientv2.components;

import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dar.nclientv2.settings.Global;

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
        final int count=landscape? Global.isFourColumn()?4:3:2;
        int first=recycler.getLayoutManager()==null?0:((GridLayoutManager)recycler.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        RecyclerView.Adapter adapter=recycler.getAdapter();
        GridLayoutManager gridLayoutManager=new GridLayoutManager(this,count);
        recycler.setLayoutManager(gridLayoutManager);
        recycler.setAdapter(adapter);
        recycler.scrollToPosition(first);
    }

}
