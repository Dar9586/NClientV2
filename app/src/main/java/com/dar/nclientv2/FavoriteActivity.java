package com.dar.nclientv2;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.adapters.FavoriteAdapter;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.async.downloader.DownloadGalleryV2;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.components.views.PageSwitcher;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


public class FavoriteActivity extends BaseActivity {
    private static final int ENTRY_PER_PAGE = 24;
    private FavoriteAdapter adapter = null;
    private boolean sortByTitle = false;
    private PageSwitcher pageSwitcher;
    private SearchView searchView;

    public static int getEntryPerPage() {
        return Global.isInfiniteScrollFavorite() ? Integer.MAX_VALUE : ENTRY_PER_PAGE;
    }

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
        pageSwitcher = findViewById(R.id.page_switcher);
        recycler = findViewById(R.id.recycler);
        refresher = findViewById(R.id.refresher);
        refresher.setRefreshing(true);
        adapter = new FavoriteAdapter(this);


        refresher.setOnRefreshListener(adapter::forceReload);
        changeLayout(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        recycler.setAdapter(adapter);
        pageSwitcher.setPages(1, 1);
        pageSwitcher.setChanger(new PageSwitcher.DefaultPageChanger() {
            @Override
            public void pageChanged(PageSwitcher switcher, int page) {
                if (adapter != null) adapter.changePage();
            }
        });

    }

    public int getActualPage() {
        return pageSwitcher.getActualPage();
    }

    public void changePages(int totalPages, int actualPages) {
        pageSwitcher.setPages(totalPages, actualPages);
    }

    @Override
    protected int getLandscapeColumnCount() {
        return Global.getColLandFavorite();
    }

    @Override
    protected int getPortraitColumnCount() {
        return Global.getColPortFavorite();
    }

    private int calculatePages(@Nullable String text) {
        int perPage = getEntryPerPage();
        int totalEntries = Queries.FavoriteTable.countFavorite(text);
        int div = totalEntries / perPage;
        int mod = totalEntries % perPage;
        return div + (mod == 0 ? 0 : 1);
    }

    @Override
    protected void onResume() {
        refresher.setEnabled(true);
        refresher.setRefreshing(true);
        String query = searchView == null ? null : searchView.getQuery().toString();
        pageSwitcher.setTotalPage(calculatePages(query));
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

        searchView = (androidx.appcompat.widget.SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                pageSwitcher.setTotalPage(calculatePages(newText));
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
        if (item.getItemId() == R.id.open_browser) {
            i = new Intent(Intent.ACTION_VIEW, Uri.parse(Utility.getBaseUrl() + "favorites/"));
            startActivity(i);
        } else if (item.getItemId() == R.id.download_page) {
            if (adapter != null) showDialogDownloadAll();
        } else if (item.getItemId() == R.id.sort_by_name) {
            sortByTitle = !sortByTitle;
            adapter.setSortByTitle(sortByTitle);
            item.setTitle(sortByTitle ? R.string.sort_by_latest : R.string.sort_by_title);
        } else if (item.getItemId() == R.id.random_favorite) {
            adapter.randomGallery();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDialogDownloadAll() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder
            .setTitle(R.string.download_all_galleries_in_this_page)
            .setIcon(R.drawable.ic_file)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok, (dialog, which) -> {
                for (Gallery g : adapter.getAllGalleries())
                    DownloadGalleryV2.downloadGallery(this, g);
            });
        builder.show();
    }
}
