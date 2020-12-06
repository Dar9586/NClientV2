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
import com.dar.nclientv2.api.local.LocalSortType;
import com.dar.nclientv2.async.downloader.DownloadQueue;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.util.List;

public class LocalActivity extends BaseActivity {
    private LocalAdapter adapter;
    private Toolbar toolbar;
    private int colCount;
    private int openedGalleryPosition=-1;
    private File folder=Global.MAINFOLDER;
    private androidx.appcompat.widget.SearchView searchView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.app_bar_main);
        toolbar=findViewById(R.id.toolbar);
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

    public void setOpenedGalleryPosition(int openedGalleryPosition) {
        this.openedGalleryPosition = openedGalleryPosition;
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
        colCount=(landscape? getLandscapeColumnCount(): getPortraitColumnCount());
        if(adapter!=null)adapter.setColCount(colCount);
        super.changeLayout(landscape);
    }

    public int getColCount() {
        return colCount;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(openedGalleryPosition!=-1){
            adapter.updateColor(openedGalleryPosition);
            openedGalleryPosition=-1;
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            finish();
            return true;
        }else if(item.getItemId()==R.id.pauseAll){
            if(adapter!=null)adapter.pauseAll();
        }else if(item.getItemId()==R.id.folder_choose){
            showDialogFolderChoose();
        }else if(item.getItemId()==R.id.startAll){
            if(adapter!=null)adapter.startAll();
        }else if(item.getItemId()==R.id.cancelAll){
            if(adapter!=null)adapter.cancellAll();
        }else if(item.getItemId()==R.id.random_favorite){
            if(adapter!=null)adapter.viewRandom();
        }else if(item.getItemId()==R.id.sort_by_name){
            dialogSortType();
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

    private void dialogSortType() {
        LocalSortType sortType=Global.getLocalSortType();
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(this);
        LinearLayout view= (LinearLayout) LayoutInflater.from(this).inflate(R.layout.local_sort_type,toolbar,false);
        ChipGroup group=view.findViewById(R.id.chip_group);
        SwitchMaterial switchMaterial=view.findViewById(R.id.ascending);
        group.check(group.getChildAt(sortType.type.ordinal()).getId());
        switchMaterial.setChecked(sortType.descending);
        builder.setView(view);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            int typeSelectedIndex=group.indexOfChild(group.findViewById(group.getCheckedChipId()));
            LocalSortType.Type typeSelected=LocalSortType.Type.values()[typeSelectedIndex];
            boolean descending=switchMaterial.isChecked();
            LocalSortType newSortType=new LocalSortType(typeSelected,descending);
            if(sortType.equals(newSortType))return;
            Global.setLocalSortType(LocalActivity.this,newSortType);
            if(adapter!=null) adapter.sortChanged();
        })
                .setNeutralButton(R.string.cancel,null)
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
        if(searchView==null)return "";
        CharSequence query=searchView.getQuery();
        return query==null?"":query.toString();
    }
}
