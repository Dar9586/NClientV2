package com.dar.nclientv2;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.adapters.LocalAdapter;
import com.dar.nclientv2.api.local.FakeInspector;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.async.downloader.DownloadQueue;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.Utility;

import java.util.ArrayList;

public class LocalActivity extends BaseActivity {
    private LocalAdapter adapter;
    private int colCount;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.app_bar_main);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.downloaded_manga);
        findViewById(R.id.page_switcher).setVisibility(View.GONE);
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        refresher.setOnRefreshListener(() -> {
            setAdapter(new LocalAdapter(this,new ArrayList<>()));
            new FakeInspector().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,LocalActivity.this);
        });
        changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
        setAdapter(new LocalAdapter(this,new ArrayList<>()));
        new FakeInspector().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,this);
    }

    public void setAdapter(LocalAdapter adapter) {
        this.adapter = adapter;
        recycler.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.download, menu);
        if(DownloadQueue.isEmpty()){
            menu.findItem(R.id.pauseAll).setVisible(false);
            menu.findItem(R.id.cancelAll).setVisible(false);
            menu.findItem(R.id.startAll).setVisible(false);
        }
        changeSortItem(menu.findItem(R.id.sort_by_name));
        final androidx.appcompat.widget.SearchView searchView=(androidx.appcompat.widget.SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(recycler.getAdapter()!=null)((LocalAdapter)recycler.getAdapter()).getFilter().filter(newText);
                return true;
            }
        });

        Utility.tintMenu(menu);

        return true;
    }

    @Override
    protected void onDestroy() {
        if(adapter!=null)adapter.removeObserver();
        super.onDestroy();
    }

    @Override
    protected void changeLayout(boolean landscape) {
        colCount=(landscape?getLandCount():getPortCount());
        if(adapter!=null)adapter.setColCount(colCount);
        super.changeLayout(landscape);
    }

    public int getColCount() {
        return colCount;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.pauseAll:
                if(adapter!=null)adapter.pauseAll();
                break;
            case R.id.startAll:
                if(adapter!=null)adapter.startAll();
                break;
            case R.id.cancelAll:
                if(adapter!=null)adapter.cancellAll();
                break;
            case R.id.random_favorite:
                if(adapter!=null)adapter.viewRandom();
                break;
            case R.id.sort_by_name:
                Global.toggleLocalSort(this);
                adapter.sortChanged();
                changeSortItem(item);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void changeSortItem(MenuItem item) {
        boolean sortByName=Global.isLocalSortByName();
        item.setIcon(sortByName?R.drawable.ic_sort_by_alpha:R.drawable.ic_access_time);
        item.setTitle(sortByName?R.string.sort_by_latest:R.string.sort_by_title);
        Global.setTint(item.getIcon());
    }

    @Override
    protected int getPortCount() {
        return Global.getColPortDownload();
    }

    @Override
    protected int getLandCount() {
        return Global.getColLandDownload();
    }

    public void addGallery(LocalGallery gallery) {
        adapter.addGallery(gallery);
    }
}
