package com.dar.nclientv2;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.dar.nclientv2.adapters.FavoriteAdapter;
import com.dar.nclientv2.components.BaseActivity;
import com.dar.nclientv2.settings.Global;

import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class FavoriteActivity extends BaseActivity {
    private boolean online=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        Global.initHttpClient(this);
        Global.initLoadImages(this);
        Global.initHighRes(this);
        setContentView(R.layout.app_bar_main);
        if(getIntent().getExtras()!=null)online=getIntent().getExtras().getBoolean(getPackageName()+".ONLINE",false);
        if(online||(getIntent().getData() != null &&Global.isLogged()))online=true;
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(online?R.string.favorite_online_manga:R.string.favorite_manga);
        final FavoriteAdapter adapter=new FavoriteAdapter(this);
        findViewById(R.id.page_switcher).setVisibility(View.GONE);
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        refresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(online)adapter.reloadOnline();
            }
        });
        changeLayout(getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE);
        recycler.setAdapter(adapter);


    }

    @Override
    protected void onResume() {
        refresher.setEnabled(true);
        refresher.setRefreshing(true);
        if(!online) {
            Global.loadFavorites(this, (FavoriteAdapter) recycler.getAdapter());
        }else ((FavoriteAdapter)recycler.getAdapter()).reloadOnline();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_login).setVisible(false);
        menu.findItem(R.id.random).setVisible(false);
        if(online||Global.isLogged())menu.findItem(R.id.online_favorite).setVisible(true);
        menu.findItem(R.id.online_favorite).setTitle(online?R.string.offline_favorites:R.string.online_favorites);
        final androidx.appcompat.widget.SearchView searchView=(androidx.appcompat.widget.SearchView)menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(recycler.getAdapter()!=null)((FavoriteAdapter)recycler.getAdapter()).getFilter().filter(newText);
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()){
            case R.id.online_favorite:
                i=new Intent(this,FavoriteActivity.class);
                i.putExtra(getPackageName()+".ONLINE",!online);
                startActivity(i);
                break;
            case R.id.open_browser:
                i=new Intent(Intent.ACTION_VIEW, Uri.parse("https://nhentai.net/favorites/"));
                startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }
}
