package com.dar.nclientv2;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.adapters.FavoriteAdapter;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.async.downloader.DownloadGalleryV2;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class FavoriteActivity extends BaseActivity {
    private FavoriteAdapter adapter=null;
    private boolean sortByTitle=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);

        setContentView(R.layout.app_bar_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.favorite_manga);
        recycler = findViewById(R.id.recycler);
        refresher = findViewById(R.id.refresher);
        refresher.setRefreshing(true);
        adapter = new FavoriteAdapter(this);

        findViewById(R.id.page_switcher).setVisibility(View.GONE);

        refresher.setOnRefreshListener(adapter::forceReload);
        changeLayout(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        recycler.setAdapter(adapter);


    }

    @Override
    protected int getLandscapeColumnCount() {
        return Global.getColLandFavorite();
    }

    @Override
    protected int getPortraitColumnCount() {
        return Global.getColPortFavorite();
    }

    @Override
    protected void onResume() {
        refresher.setEnabled(true);
        refresher.setRefreshing(true);
        adapter.forceReload();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.download_page).setVisible(true);
        menu.findItem(R.id.sort_by_name).setVisible(true);
        menu.findItem(R.id.by_popular).setVisible(false);
        menu.findItem(R.id.only_language).setVisible(false);
        menu.findItem(R.id.add_bookmark).setVisible(false);

        final androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null)
                    adapter.getFilter().filter(newText);
                return true;
            }
        });
        Utility.tintMenu(menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case R.id.open_browser:
                i = new Intent(Intent.ACTION_VIEW, Uri.parse(Utility.getBaseUrl()+"favorites/"));
                startActivity(i);
                break;
            case R.id.download_page:
                if(adapter!=null)showDialogDownloadAll();
                break;
            case R.id.sort_by_name:
                sortByTitle=!sortByTitle;
                adapter.setSortByTitle(sortByTitle);
                item.setTitle(sortByTitle?R.string.sort_by_latest:R.string.sort_by_title);
                break;

        }
        return super.onOptionsItemSelected(item);
    }
    private void showDialogDownloadAll() {
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(this);
        builder
                .setTitle(R.string.download_all_galleries_in_this_page)
                .setIcon(R.drawable.ic_file)
                .setNegativeButton(R.string.cancel,null)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    for(Gallery g:adapter.getAllGalleries())
                        DownloadGalleryV2.downloadGallery(this, g);
                });
        builder.show();
    }
}
