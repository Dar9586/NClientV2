package com.dar.nclientv2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.dar.nclientv2.api.Inspector;
import com.dar.nclientv2.api.enums.ApiRequestType;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.components.BaseActivity;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;

import java.util.Locale;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initTitleType(this);
        Global.initByPopular(this);
        Global.initLoadImages(this);
        Global.initOnlyLanguage(this);
        Global.initColumnCount(this);
        Global.initTagSets(this);
        Global.initTagPreferencesSets(this);
        Global.initTagOrder(this);
        Global.initMinTagCount(this);
        Global.initMaxId(this);
        Global.loadTheme(this);
        Global.loadNotificationChannel(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.app_name);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        changeNavigationImage(navigationView);
        navigationView.setNavigationItemSelectedListener(this);
        switch (Global.getTitleType()){
            case PRETTY:navigationView.setCheckedItem(R.id.pretty_title);break;
            case ENGLISH:navigationView.setCheckedItem(R.id.english_title);break;
            case JAPANESE:navigationView.setCheckedItem(R.id.japanese_title);break;
        }
        navigationView.getMenu().findItem(R.id.by_popular).setIcon(Global.isByPopular()?R.drawable.ic_check:R.drawable.ic_close);
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        prepareUpdateIcon();
        updateLanguageIcon(navigationView.getMenu().findItem(R.id.only_language),false);
        recycler.setHasFixedSize(true);

        refresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Inspector(MainActivity.this,Inspector.getActualPage(),Inspector.getActualQuery(),Inspector.getActualRequestType());
            }
        });
        findViewById(R.id.prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actualPage > 1)
                    new Inspector(MainActivity.this, actualPage - 1, Inspector.getActualQuery(), Inspector.getActualRequestType());
            }
        });
        findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actualPage < totalPage)
                    new Inspector(MainActivity.this, actualPage + 1, Inspector.getActualQuery(), Inspector.getActualRequestType());

            }
        });
        findViewById(R.id.page_index).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDialog();
            }
        });

        changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
        String tag;
        try {
            tag = getIntent().getExtras().getString(getPackageName()+".TAG", "");
        }catch (NullPointerException e){
            tag="";
        }
        if(tag.equals("")) new Inspector(this,1,"",ApiRequestType.BYALL);
        else new Inspector(this,1,tag,ApiRequestType.BYTAG);
    }

    private void changeNavigationImage(NavigationView navigationView) {
        switch (Global.getTheme()){
            case BLACK:navigationView.getHeaderView(0).findViewById(R.id.layout_header).setBackgroundResource(android.R.color.black);break;
            default:navigationView.getHeaderView(0).findViewById(R.id.layout_header).setBackgroundResource(R.drawable.side_nav_bar);break;
        }
    }

    private void prepareUpdateIcon(){
        if(Global.getOnlyLanguage()==null)Global.updateOnlyLanguage(this,Language.UNKNOWN);
        else{
            switch (Global.getOnlyLanguage()){
                case ENGLISH:Global.updateOnlyLanguage(this,null);break;
                case JAPANESE:Global.updateOnlyLanguage(this,Language.ENGLISH);break;
                case CHINESE:Global.updateOnlyLanguage(this,Language.JAPANESE);break;
                case UNKNOWN:Global.updateOnlyLanguage(this,Language.CHINESE);break;
            }
        }
    }
    private void updateLanguageIcon(MenuItem item,boolean update){
        //ALL,ENGLISH;JAPANESE;CHINESE;OTHER

        if(Global.getOnlyLanguage()==null){
            Global.updateOnlyLanguage(this,Language.ENGLISH);
            item.setTitle(R.string.only_english);
            item.setIcon(R.drawable.ic_gbbw);
        }
        else
            switch (Global.getOnlyLanguage()){
                case ENGLISH:Global.updateOnlyLanguage(this, Language.JAPANESE);item.setTitle(R.string.only_japanese);item.setIcon(R.drawable.ic_jpbw);break;
                case JAPANESE:Global.updateOnlyLanguage(this, Language.CHINESE);item.setTitle(R.string.only_chinese);item.setIcon(R.drawable.ic_cnbw);break;
                case CHINESE:Global.updateOnlyLanguage(this, Language.UNKNOWN);item.setTitle(R.string.only_other);item.setIcon(R.drawable.ic_help);break;
                case UNKNOWN:Global.updateOnlyLanguage(this, null);item.setTitle(R.string.all_languages);item.setIcon(R.drawable.ic_world);break;
            }
            if(update)new Inspector(MainActivity.this,Inspector.getActualPage(),Inspector.getActualQuery(),Inspector.getActualRequestType());
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    public void hidePageSwitcher(){
        findViewById(R.id.page_switcher).setVisibility(View.GONE);
    }
    public void showPageSwitcher(final int actualPage,final int totalPage){
        findViewById(R.id.page_switcher).setVisibility(totalPage==1?View.GONE:View.VISIBLE);
        this.actualPage=actualPage;
        this.totalPage=totalPage;
        EditText text=findViewById(R.id.page_index);
        text.setText(String.format(Locale.US, "%d/%d", actualPage, totalPage));
    }
    private int actualPage,totalPage;
    private void loadDialog(){
        DefaultDialogs.pageChangerDialog(
                new DefaultDialogs.Builder(this).setActual(actualPage).setMax(totalPage).setDialogs(new DefaultDialogs.DialogResults() {
                    @Override
                    public void positive(int actual) {
                        new Inspector(MainActivity.this,actual,Inspector.getActualQuery(),Inspector.getActualRequestType());
                    }
                    @Override
                    public void negative() {}
                }).setTitle(R.string.change_page).setDrawable(R.drawable.ic_find_in_page)
        );
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            changeLayout(true);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            changeLayout(false);
        }
    }
    private void changeLayout(boolean landscape){
        final int count=landscape?4:2;
        RecyclerView.Adapter adapter=recycler.getAdapter();
        GridLayoutManager gridLayoutManager=new GridLayoutManager(this,count);

        recycler.setLayoutManager(gridLayoutManager);
        recycler.setAdapter(adapter);
    }
    private Setting setting=null;
    private class Setting{
        Global.ThemeScheme theme;
        boolean loadImages;
        public Setting() {
            this.theme = Global.getTheme();
            this.loadImages = Global.isLoadImages();
        }
    }
    @Override
    protected void onPause() {
        setting=new Setting();
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(setting!=null){
            if(Global.initLoadImages(this)!=setting.loadImages) recycler.getAdapter().notifyDataSetChanged();
            if(Global.getTheme()!=setting.theme)recreate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        Global.setTint(menu.findItem(R.id.search).getIcon());
        Global.setTint(menu.findItem(R.id.random).getIcon());

        final SearchView searchView=(SearchView)menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                new Inspector(MainActivity.this,1,query,ApiRequestType.BYSEARCH);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
        ImageView closeButton = searchView.findViewById(R.id.search_close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setQuery("",false);
                new Inspector(MainActivity.this,1,"",ApiRequestType.BYALL);
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent i;
        switch (id){
            case R.id.action_settings:
                i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
            case R.id.random:
                i = new Intent(this, RandomActivity.class);
                startActivity(i);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id){
            case R.id.pretty_title:Global.updateTitleType(this, TitleType.PRETTY);recycler.getAdapter().notifyDataSetChanged();break;
            case R.id.english_title:Global.updateTitleType(this, TitleType.ENGLISH);recycler.getAdapter().notifyDataSetChanged();break;
            case R.id.japanese_title:Global.updateTitleType(this, TitleType.JAPANESE);recycler.getAdapter().notifyDataSetChanged();break;
            case R.id.by_popular:item.setIcon(Global.updateByPopular(this,!Global.isByPopular())?R.drawable.ic_check:R.drawable.ic_close);new Inspector(this,1,Inspector.getActualQuery(),Inspector.getActualRequestType());break;
            case R.id.only_language:updateLanguageIcon(item,true);break;
            case R.id.downloaded:if(Global.hasStoragePermission(this))startLocalActivity();else requestStorage();break;
            case R.id.tag_manager:
                Intent intent=new Intent(this,TagFilter.class);
                startActivity(intent);
                break;
        }
        //DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        //drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    @TargetApi(23)
    private void requestStorage(){
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==1&&grantResults.length >0&&grantResults[0]== PackageManager.PERMISSION_GRANTED)
            startLocalActivity();
    }

    private void startLocalActivity(){
        Intent i=new Intent(this,LocalActivity.class);
        startActivity(i);
    }
}
