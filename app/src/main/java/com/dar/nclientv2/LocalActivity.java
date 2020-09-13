package com.dar.nclientv2;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.adapters.LocalAdapter;
import com.dar.nclientv2.api.local.FakeInspector;
import com.dar.nclientv2.async.downloader.DownloadQueue;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.List;

public class LocalActivity extends BaseActivity {
    private LocalAdapter adapter;
    private int colCount;
    private File folder=Global.MAINFOLDER;
    private androidx.appcompat.widget.SearchView searchView;
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
        refresher.setOnRefreshListener(() -> new FakeInspector(folder).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,LocalActivity.this));
        changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
        new FakeInspector(folder).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,this);
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
        menu.findItem(R.id.folder_choose).setVisible(Global.getUsableFolders(this).size()>1);
        changeSortItem(menu.findItem(R.id.sort_by_name));
        searchView=(androidx.appcompat.widget.SearchView) menu.findItem(R.id.search).getActionView();
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
            case R.id.folder_choose:
                showDialogFolderChoose();
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
                if(adapter!=null) {
                    Global.toggleLocalSort(this);
                    adapter.sortChanged();
                    changeSortItem(item);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDialogFolderChoose() {
        List<File>strings=Global.getUsableFolders(this);
        ArrayAdapter adapter=new ArrayAdapter(this,android.R.layout.select_dialog_singlechoice,strings);
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.choose_directory).setIcon(R.drawable.ic_folder);
        builder.setAdapter(adapter, (dialog, which) -> {
            folder=new File(strings.get(which),"NClientV2");
            new FakeInspector(folder).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,LocalActivity.this);
        }).setNegativeButton(R.string.cancel,null).show();
    }

    private void changeSortItem(MenuItem item) {
        boolean sortByName=Global.isLocalSortByName();
        item.setIcon(sortByName?R.drawable.ic_sort_by_alpha:R.drawable.ic_access_time);
        item.setTitle(sortByName?R.string.sort_by_title:R.string.sort_by_latest);
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

    public String getQuery() {
        if(searchView==null)return "";
        CharSequence query=searchView.getQuery();
        return query==null?"":query.toString();
    }
}
