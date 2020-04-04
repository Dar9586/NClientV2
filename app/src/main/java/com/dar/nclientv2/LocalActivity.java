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
import com.dar.nclientv2.async.downloader.DownloadQueue;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.settings.Global;

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
        refresher.setOnRefreshListener(() -> new FakeInspector().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,LocalActivity.this));
        changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
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
        Global.setTint(menu.findItem(R.id.pauseAll).getIcon());
        Global.setTint(menu.findItem(R.id.cancelAll).getIcon());
        Global.setTint(menu.findItem(R.id.startAll).getIcon());
        Global.setTint(menu.findItem(R.id.search).getIcon());
        Global.setTint(menu.findItem(R.id.random_favorite).getIcon());

        if(DownloadQueue.isEmpty()){
            menu.findItem(R.id.pauseAll).setVisible(false);
            menu.findItem(R.id.cancelAll).setVisible(false);
            menu.findItem(R.id.startAll).setVisible(false);
        }

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
        return true;
    }

    @Override
    protected void onDestroy() {
        adapter.removeObserver();
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
                adapter.pauseAll();
                break;
            case R.id.startAll:
                adapter.startAll();
                break;
            case R.id.cancelAll:
                adapter.cancellAll();
                break;
            case R.id.random_favorite:
                adapter.viewRandom();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getPortCount() {
        return Global.getColPortDownload();
    }

    @Override
    protected int getLandCount() {
        return Global.getColLandDownload();
    }
}
