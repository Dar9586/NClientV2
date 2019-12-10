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
import com.dar.nclientv2.components.BaseActivity;
import com.dar.nclientv2.settings.Global;

public class FavoriteActivity extends BaseActivity {
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
        final FavoriteAdapter adapter = new FavoriteAdapter(this);

        findViewById(R.id.page_switcher).setVisibility(View.GONE);

        refresher.setOnRefreshListener(adapter::forceReload);
        changeLayout(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        recycler.setAdapter(adapter);


    }

    @Override
    protected int getLandCount() {
        return Global.getColLandFavorite();
    }

    @Override
    protected int getPortCount() {
        return Global.getColPortFavorite();
    }

    @Override
    protected void onResume() {
        refresher.setEnabled(true);
        refresher.setRefreshing(true);
        ((FavoriteAdapter) recycler.getAdapter()).forceReload();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.download_page).setVisible(false);
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
                if (recycler.getAdapter() != null)
                    ((FavoriteAdapter) recycler.getAdapter()).getFilter().filter(newText);
                return true;
            }
        });
        Global.setTint(menu.findItem(R.id.open_browser).getIcon());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case R.id.open_browser:
                i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://nhentai.net/favorites/"));
                startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }
}
