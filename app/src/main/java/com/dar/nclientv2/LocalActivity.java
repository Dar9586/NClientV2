package com.dar.nclientv2;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

import com.dar.nclientv2.adapters.LocalAdapter;
import com.dar.nclientv2.api.local.FakeInspector;
import com.dar.nclientv2.components.BaseActivity;
import com.dar.nclientv2.settings.Global;

public class LocalActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        setContentView(R.layout.app_bar_main);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.downloaded_manga);
        findViewById(R.id.page_switcher).setVisibility(View.GONE);
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        refresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new FakeInspector().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,LocalActivity.this);
            }
        });
        changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
        new FakeInspector().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,this);
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
    private void changeLayout(boolean landscape){
        final int count=landscape?4:2;
        RecyclerView.Adapter adapter=recycler.getAdapter();
        GridLayoutManager gridLayoutManager=new GridLayoutManager(this,count);
        recycler.setLayoutManager(gridLayoutManager);
        recycler.setAdapter(adapter);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.random).setVisible(false);
        final android.support.v7.widget.SearchView searchView=(android.support.v7.widget.SearchView)menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(recycler.getAdapter()!=null)((LocalAdapter)recycler.getAdapter()).filter(newText);
                return true;
            }
        });
        return true;
    }
}
