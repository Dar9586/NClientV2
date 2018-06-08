package com.dar.nclientv2.components;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
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
}
