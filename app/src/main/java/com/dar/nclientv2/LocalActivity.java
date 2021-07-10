package com.dar.nclientv2;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.adapters.LocalAdapter;
import com.dar.nclientv2.api.local.FakeInspector;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.api.local.LocalSortType;
import com.dar.nclientv2.async.converters.CreatePDF;
import com.dar.nclientv2.async.downloader.GalleryDownloaderV2;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.components.classes.MultichoiceAdapter;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.util.List;

public class LocalActivity extends BaseActivity {
    private Menu optionMenu;
    private LocalAdapter adapter;
    private final MultichoiceAdapter.MultichoiceListener listener = new MultichoiceAdapter.DefaultMultichoiceListener() {

        @Override
        public void choiceChanged() {
            setMenuVisibility(optionMenu);
        }
    };
    private Toolbar toolbar;
    private int colCount;
    private int idGalleryPosition = -1;
    private File folder = Global.MAINFOLDER;
    private androidx.appcompat.widget.SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Global.initActivity(this);
        setContentView(R.layout.app_bar_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.downloaded_manga);
        findViewById(R.id.page_switcher).setVisibility(View.GONE);
        recycler = findViewById(R.id.recycler);
        refresher = findViewById(R.id.refresher);
        refresher.setOnRefreshListener(() -> new FakeInspector(folder).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, LocalActivity.this));
        changeLayout(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        new FakeInspector(folder).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
    }

    public void setAdapter(LocalAdapter adapter) {
        this.adapter = adapter;
        this.adapter.addListener(listener);
        recycler.setAdapter(adapter);
    }

    public void setIdGalleryPosition(int idGalleryPosition) {
        this.idGalleryPosition = idGalleryPosition;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.download, menu);
        getMenuInflater().inflate(R.menu.local_multichoice, menu);
        this.optionMenu = menu;
        setMenuVisibility(menu);
        searchView = (androidx.appcompat.widget.SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (recycler.getAdapter() != null)
                    ((LocalAdapter) recycler.getAdapter()).getFilter().filter(newText);
                return true;
            }
        });

        Utility.tintMenu(menu);

        return true;
    }

    private void setMenuVisibility(Menu menu) {
        if (menu == null) return;
        MultichoiceAdapter.Mode mode = adapter == null ? MultichoiceAdapter.Mode.NORMAL : adapter.getMode();
        boolean hasGallery = false;
        boolean hasDownloads = false;
        if (mode == MultichoiceAdapter.Mode.SELECTING) {
            hasGallery = adapter.hasSelectedClass(LocalGallery.class);
            hasDownloads = adapter.hasSelectedClass(GalleryDownloaderV2.class);
        }

        menu.findItem(R.id.search).setVisible(mode == MultichoiceAdapter.Mode.NORMAL);
        menu.findItem(R.id.sort_by_name).setVisible(mode == MultichoiceAdapter.Mode.NORMAL);
        menu.findItem(R.id.folder_choose).setVisible(mode == MultichoiceAdapter.Mode.NORMAL && Global.getUsableFolders(this).size() > 1);
        menu.findItem(R.id.random_favorite).setVisible(mode == MultichoiceAdapter.Mode.NORMAL);

        menu.findItem(R.id.delete_all).setVisible(mode == MultichoiceAdapter.Mode.SELECTING);
        menu.findItem(R.id.select_all).setVisible(mode == MultichoiceAdapter.Mode.SELECTING);
        menu.findItem(R.id.pause_all).setVisible(mode == MultichoiceAdapter.Mode.SELECTING && !hasGallery && hasDownloads);
        menu.findItem(R.id.start_all).setVisible(mode == MultichoiceAdapter.Mode.SELECTING && !hasGallery && hasDownloads);
        menu.findItem(R.id.pdf_all).setVisible(mode == MultichoiceAdapter.Mode.SELECTING && hasGallery && !hasDownloads && CreatePDF.hasPDFCapabilities());
        menu.findItem(R.id.zip_all).setVisible(mode == MultichoiceAdapter.Mode.SELECTING && hasGallery && !hasDownloads);
    }

    @Override
    protected void onDestroy() {
        if (adapter != null) adapter.removeObserver();
        super.onDestroy();
    }

    @Override
    protected void changeLayout(boolean landscape) {
        colCount = (landscape ? getLandscapeColumnCount() : getPortraitColumnCount());
        if (adapter != null) adapter.setColCount(colCount);
        super.changeLayout(landscape);
    }

    public int getColCount() {
        return colCount;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (idGalleryPosition != -1) {
            adapter.updateColor(idGalleryPosition);
            idGalleryPosition = -1;
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.pause_all) {
            adapter.pauseSelected();
        } else if (item.getItemId() == R.id.start_all) {
            adapter.startSelected();
        } else if (item.getItemId() == R.id.delete_all) {
            adapter.deleteSelected();
        } else if (item.getItemId() == R.id.pdf_all) {
            adapter.pdfSelected();
        } else if (item.getItemId() == R.id.zip_all) {
            adapter.zipSelected();
        } else if (item.getItemId() == R.id.select_all) {
            adapter.selectAll();
        } else if (item.getItemId() == R.id.folder_choose) {
            showDialogFolderChoose();
        } else if (item.getItemId() == R.id.random_favorite) {
            if (adapter != null) adapter.viewRandom();
        } else if (item.getItemId() == R.id.sort_by_name) {
            dialogSortType();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (adapter != null && adapter.getMode() == MultichoiceAdapter.Mode.SELECTING)
            adapter.deselectAll();
        else
            super.onBackPressed();
    }

    private void showDialogFolderChoose() {
        List<File> strings = Global.getUsableFolders(this);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice, strings);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.choose_directory).setIcon(R.drawable.ic_folder);
        builder.setAdapter(adapter, (dialog, which) -> {
            folder = new File(strings.get(which), "NClientV2");
            new FakeInspector(folder).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, LocalActivity.this);
        }).setNegativeButton(R.string.cancel, null).show();
    }

    private void dialogSortType() {
        LocalSortType sortType = Global.getLocalSortType();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LinearLayout view = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.local_sort_type, toolbar, false);
        ChipGroup group = view.findViewById(R.id.chip_group);
        SwitchMaterial switchMaterial = view.findViewById(R.id.ascending);
        group.check(group.getChildAt(sortType.type.ordinal()).getId());
        switchMaterial.setChecked(sortType.descending);
        builder.setView(view);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            int typeSelectedIndex = group.indexOfChild(group.findViewById(group.getCheckedChipId()));
            LocalSortType.Type typeSelected = LocalSortType.Type.values()[typeSelectedIndex];
            boolean descending = switchMaterial.isChecked();
            LocalSortType newSortType = new LocalSortType(typeSelected, descending);
            if (sortType.equals(newSortType)) return;
            Global.setLocalSortType(LocalActivity.this, newSortType);
            if (adapter != null) adapter.sortChanged();
        })
            .setNeutralButton(R.string.cancel, null)
            .setTitle(R.string.sort_select_type)
            .show();


       /* boolean sortByName=Global.isLocalSortByName();
        item.setIcon(sortByName?R.drawable.ic_sort_by_alpha:R.drawable.ic_access_time);
        item.setTitle(sortByName?R.string.sort_by_title:R.string.sort_by_latest);
        Global.setTint(item.getIcon());*/
    }

    @Override
    protected int getPortraitColumnCount() {
        return Global.getColPortDownload();
    }

    @Override
    protected int getLandscapeColumnCount() {
        return Global.getColLandDownload();
    }

    public String getQuery() {
        if (searchView == null) return "";
        CharSequence query = searchView.getQuery();
        return query == null ? "" : query.toString();
    }
}
