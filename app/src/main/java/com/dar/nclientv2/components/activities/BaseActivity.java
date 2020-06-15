package com.dar.nclientv2.components.activities;

import android.content.res.Configuration;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dar.nclientv2.components.widgets.CustomGridLayoutManager;

public abstract class BaseActivity extends AppCompatActivity {
    protected RecyclerView recycler;
    protected SwipeRefreshLayout refresher;
    protected ViewGroup masterLayout;
    public SwipeRefreshLayout getRefresher() {
        return refresher;
    }

    public RecyclerView getRecycler() {
        return recycler;
    }

    public ViewGroup getMasterLayout() {
        return masterLayout;
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
    protected abstract int getPortCount();
    protected abstract int getLandCount();
    protected void changeLayout(boolean landscape){
        final int count=landscape? getLandCount():getPortCount();
        int first=recycler.getLayoutManager()==null?0:((CustomGridLayoutManager)recycler.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        RecyclerView.Adapter adapter=recycler.getAdapter();
        CustomGridLayoutManager gridLayoutManager=new CustomGridLayoutManager(this,count);
        recycler.setLayoutManager(gridLayoutManager);
        recycler.setAdapter(adapter);
        recycler.scrollToPosition(first);
    }

}
