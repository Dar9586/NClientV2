package com.dar.nclientv2;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.dar.nclientv2.adapters.ManagerAdapter;
import com.dar.nclientv2.async.DownloadGallery;
import com.dar.nclientv2.components.BaseActivity;
import com.dar.nclientv2.settings.Global;

public class DownloadManagerActivity extends BaseActivity {
    ManagerAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadThemeAndLanguage(this);
        setContentView(R.layout.app_bar_main);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.download_manager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        refresher.setEnabled(false);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter=new ManagerAdapter(this);
        recycler.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        DownloadGallery.removeObserver();
        super.onDestroy();
    }

    @Override
    protected void changeLayout(boolean landscape) {}
    @Override
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.download, menu);
        Global.setTint(menu.findItem(R.id.startAll).getIcon());
        Global.setTint(menu.findItem(R.id.pauseAll).getIcon());
        Global.setTint(menu.findItem(R.id.cancelAll).getIcon());
        return super.onCreateOptionsMenu(menu);
    }
}
