package com.dar.nclientv2.components.activities;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dar.nclientv2.components.widgets.CustomGridLayoutManager;
import com.dar.nclientv2.settings.Global;

public abstract class BaseActivity extends GeneralActivity {
    protected RecyclerView recycler;
    protected SwipeRefreshLayout refresher;
    protected ViewGroup masterLayout;

    protected abstract int getPortraitColumnCount();

    protected abstract int getLandscapeColumnCount();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
    }

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
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            changeLayout(true);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            changeLayout(false);
        }
    }

    protected void changeLayout(boolean landscape) {
        CustomGridLayoutManager manager = (CustomGridLayoutManager) recycler.getLayoutManager();
        RecyclerView.Adapter adapter = recycler.getAdapter();
        int count = landscape ? getLandscapeColumnCount() : getPortraitColumnCount();
        int position = 0;

        if (manager != null)
            position = manager.findFirstCompletelyVisibleItemPosition();
        CustomGridLayoutManager gridLayoutManager = new CustomGridLayoutManager(this, count);
        recycler.setLayoutManager(gridLayoutManager);
        recycler.setAdapter(adapter);
        recycler.scrollToPosition(position);
    }
}
