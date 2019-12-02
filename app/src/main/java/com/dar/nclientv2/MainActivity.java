package com.dar.nclientv2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.adapters.ListAdapter;
import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.ScrapeTags;
import com.dar.nclientv2.async.VersionChecker;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.BaseActivity;
import com.dar.nclientv2.loginapi.Login;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;
import com.google.android.material.navigation.NavigationView;

import org.acra.ACRA;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private enum MainStatus{UNKNOWN,NORMAL,TAG,FAVORITE,SEARCH,BOOKMARK}
    private InspectorV3 inspector=null;
    private NavigationView navigationView;
    private static boolean firstTime=true;
    public MenuItem loginItem;
    private MainStatus status=MainStatus.UNKNOWN;
    private int actualPage=1,totalPage;
    private boolean inspecting=false;
    public void setInspector(InspectorV3 inspector) {
        this.inspector = inspector;
    }
    public ListAdapter adapter;
    InspectorV3.InspectorResponse resetDataset=new InspectorV3.DefaultInspectorResponse() {
        @Override
        public void onSuccess(List<Gallery> galleries) {
            adapter.restartDataset(galleries);
            showPageSwitcher(inspector.getPage(),inspector.getPageCount());
            runOnUiThread(()->recycler.smoothScrollToPosition(0));
        }

        @Override
        public void onStart() {
            runOnUiThread(()->refresher.setRefreshing(true));
        }

        @Override
        public void onEnd() {
            runOnUiThread(()->refresher.setRefreshing(false));
        }
    },addDataset=new InspectorV3.DefaultInspectorResponse() {
        @Override
        public void onSuccess(List<Gallery> galleries) {
            adapter.addGalleries(galleries);
        }
        @Override
        public void onStart() {
            runOnUiThread(()->refresher.setRefreshing(true));
        }

        @Override
        public void onEnd() {
            runOnUiThread(()->refresher.setRefreshing(false));
            inspecting=false;
        }
    },startGallery=new InspectorV3.DefaultInspectorResponse() {
        @Override
        public void onSuccess(List<Gallery> galleries) {
            Intent intent=new Intent(MainActivity.this, GalleryActivity.class);
            Log.d(Global.LOGTAG,galleries.get(0).toString());
            intent.putExtra(getPackageName()+".GALLERY",galleries.get(0));
            runOnUiThread(()->{
                startActivity(intent);
                finish();
            });
            Log.d(Global.LOGTAG,"STARTED");
        }
        @Override
        public void onStart() {
            runOnUiThread(()->refresher.setRefreshing(true));
        }

        @Override
        public void onEnd() {
            runOnUiThread(()->refresher.setRefreshing(false));
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Global.loadThemeAndLanguage(this);
        if(Global.hasStoragePermission(this)){//delete older APK
            final File f=new File(Global.UPDATEFOLDER,"NClientV2_"+Global.getVersionName(this)+".apk");
            Log.d(Global.LOGTAG,f.getAbsolutePath());
            if(f.exists())f.delete();
        }
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.app_name);

        Uri data=getIntent().getData();
        if(getIntent().getBooleanExtra(getPackageName()+".ISBYTAG",false)){//TAG FROM OTHER ACTIVITY
            Tag t=getIntent().getParcelableExtra(getPackageName()+".TAG");
            inspector=InspectorV3.searchInspector(this,null,new HashSet<>(Collections.singleton(t)),1,Global.isByPopular(),resetDataset);
            status=MainStatus.TAG;
        }else if(getIntent().getBooleanExtra(getPackageName()+".SEARCHSTART",false)){//Search
            String query=getIntent().getStringExtra(getPackageName()+".QUERY");
            boolean ok=false;
            status = MainStatus.SEARCH;
            try {
                int id=Integer.parseInt(query);
                if(id>0&&id<=Global.getMaxId()){
                    inspector= InspectorV3.galleryInspector(this, id, startGallery );
                    ok=true;
                }
            }catch (NumberFormatException ignore){}
            if(!ok) {
                if (query != null) query = query.trim();
                boolean advanced = getIntent().getBooleanExtra(getPackageName() + ".ADVANCED", false);
                HashSet<Tag> tags = null;
                if (advanced) {
                    tags = new HashSet<>(getIntent().getParcelableArrayListExtra(getPackageName() + ".TAGS"));
                }
                inspector = InspectorV3.searchInspector(this, query, tags, 1, Global.isByPopular(), resetDataset);
            }
        } else if(getIntent().getBooleanExtra(getPackageName()+".FAVORITE",false)){//Online favorite
            inspector=InspectorV3.favoriteInspector(this,null,1,resetDataset);
            status=MainStatus.FAVORITE;
        } else if(getIntent().getBooleanExtra(getPackageName()+".BYBOOKMARK",false)){//Bookmark
            inspector=getIntent().getParcelableExtra(getPackageName()+".INSPECTOR");
            inspector.initialize(this,resetDataset);
            status=MainStatus.BOOKMARK;
            switch (inspector.getRequestType()){
                case BYTAG:status=MainStatus.TAG;break;
                case BYALL:status=MainStatus.NORMAL;break;
                case BYSEARCH:status=MainStatus.SEARCH;break;
            }
        }else if(data!=null){//normal,search or tag by url
            status=manageDataStart(data);
        }else{//normal
            inspector=InspectorV3.searchInspector(this,null,null,1,Global.isByPopular(),resetDataset);
            status=MainStatus.NORMAL;
        }


        Log.d(Global.LOGTAG,"Main started with status "+status);

        navigationView = findViewById(R.id.nav_view);
        changeNavigationImage(navigationView);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().findItem(R.id.online_favorite_manager).setVisible(com.dar.nclientv2.settings.Login.isLogged());
        loginItem=navigationView.getMenu().findItem(R.id.action_login);
        loadStringLogin();
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        adapter=new ListAdapter(this);
        recycler.setAdapter(adapter);
        recycler.setHasFixedSize(true);
        recycler.setItemViewCacheSize(24);
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
                if(Global.isInfiniteScroll()&&!refresher.isRefreshing()){
                    GridLayoutManager manager = (GridLayoutManager)recycler.getLayoutManager();
                    if(actualPage < totalPage && manager.findLastVisibleItemPosition() >= (recycler.getAdapter().getItemCount()-1-manager.getSpanCount())) {
                        if(inspecting)return;
                        inspecting=true;
                        inspector = inspector.cloneInspector(MainActivity.this,addDataset);
                        inspector.setPage(inspector.getPage()+1);
                        inspector.start();
                    }

                }

            }
        });
        refresher.setOnRefreshListener(() -> {
            inspector = inspector.cloneInspector(MainActivity.this,resetDataset);
            if(Global.isInfiniteScroll())inspector.setPage(1);
            inspector.start();
        });
        findViewById(R.id.prev).setOnClickListener(v -> {
            if (actualPage > 1){
                    inspector=inspector.cloneInspector(MainActivity.this,resetDataset);
                    inspector.setPage(inspector.getPage()-1);
                    inspector.start();
            }

        });
        findViewById(R.id.next).setOnClickListener(v -> {
            if (actualPage < totalPage){
                inspector=inspector.cloneInspector(MainActivity.this,resetDataset);
                inspector.setPage(inspector.getPage()+1);
                inspector.start();
            }

        });
        findViewById(R.id.page_index).setOnClickListener(v -> loadDialog());
        changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);

        if(status!=MainStatus.NORMAL){
            //drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            //toggle.setDrawerIndicatorEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }else{
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();
        }

        switch (status){
            case FAVORITE:  getSupportActionBar().setTitle(R.string.favorite_online_manga);break;
            case SEARCH:    getSupportActionBar().setTitle(inspector.getQuery());break;
            case TAG:       getSupportActionBar().setTitle(inspector.getTag().getName());break;
            case NORMAL:    getSupportActionBar().setTitle(R.string.app_name);break;
            default:        getSupportActionBar().setTitle("WTF");break;
        }



        if(firstTime){
            if(Global.shouldCheckForUpdates(this))new VersionChecker(this,true);
            Intent i=new Intent(this,ScrapeTags.class);
            startService(i);
            firstTime=false;
        }
        inspector.start();

    }

    //FIND AND INIT INSPECTOR
    private MainStatus manageDataStart(Uri data) {
        List<String>datas=data.getPathSegments();
        Log.d(Global.LOGTAG,"Datas: "+datas);
        if(datas.size()==0){
            inspector=InspectorV3.searchInspector(this,null,null,1,Global.isByPopular(),resetDataset);
            return MainStatus.NORMAL;
        }
        TagType dataType=null;
        String query;
        int page=1;
        boolean byPop=false;
        switch (datas.get(0)){
            case "parody":dataType=TagType.PARODY; break;
            case "character":dataType=TagType.CHARACTER; break;
            case "tag":dataType=TagType.TAG; break;
            case "artist":dataType=TagType.ARTIST; break;
            case "group":dataType=TagType.GROUP; break;
            case "language":dataType=TagType.LANGUAGE; break;
            case "category":dataType=TagType.CATEGORY; break;
        }
        if(dataType!=null){
            query=datas.get(1);
            Tag tag=Queries.TagTable.getTagFromTagName(Database.getDatabase(),query);
            if(tag==null) tag=new Tag(query,-1,-1 ,dataType,TagStatus.DEFAULT);
            byPop=datas.size()==3;
            inspector=InspectorV3.searchInspector(this,null,new HashSet<>(Collections.singleton(tag)),1,byPop,resetDataset);
            return MainStatus.TAG;

        } else{
            query=data.getQueryParameter("page");
            if(query!=null)page=Integer.parseInt(query);

            if("favorites".equals(datas.get(0))){
                if(com.dar.nclientv2.settings.Login.isLogged()) {
                    inspector = InspectorV3.favoriteInspector(this, null, page, resetDataset);
                    return MainStatus.FAVORITE;
                }else{
                    Intent intent=new Intent(this,FavoriteActivity.class);
                    startActivity(intent);
                    finish();
                    return MainStatus.NORMAL;
                }
            }

            byPop="popular".equals(data.getQueryParameter("sort"));
            query=data.getQueryParameter("q");
            inspector=InspectorV3.searchInspector(this,query,null,page,byPop,resetDataset);
            return MainStatus.SEARCH;
        }
    }

    private void loadStringLogin() {
        if(loginItem==null)return;
        if(com.dar.nclientv2.settings.Login.getUser()!=null)loginItem.setTitle(getString(R.string.login_formatted, com.dar.nclientv2.settings.Login.getUser().getUsername()));
        else loginItem.setTitle(com.dar.nclientv2.settings.Login.isLogged()?R.string.logout :R.string.login);

    }

    private void changeNavigationImage(NavigationView navigationView) {
        switch (Global.getTheme()){
            case BLACK:Global.loadImage(R.drawable.ic_logo,navigationView.getHeaderView(0).findViewById(R.id.imageView));navigationView.getHeaderView(0).findViewById(R.id.layout_header).setBackgroundResource(android.R.color.black);break;
            default:Global.loadImage(R.mipmap.ic_launcher,navigationView.getHeaderView(0).findViewById(R.id.imageView));navigationView.getHeaderView(0).findViewById(R.id.layout_header).setBackgroundResource(R.drawable.side_nav_bar);break;
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    private void updateLanguageIcon(MenuItem item){
        //ALL,ENGLISH;JAPANESE;CHINESE;OTHER

        if(Global.getOnlyLanguage()==null) Global.updateOnlyLanguage(this,Language.ENGLISH);
        else
            switch (Global.getOnlyLanguage()){
                case ENGLISH:Global.updateOnlyLanguage(this, Language.JAPANESE);break;
                case JAPANESE:Global.updateOnlyLanguage(this, Language.CHINESE);break;
                case CHINESE:Global.updateOnlyLanguage(this, Language.UNKNOWN);break;
                case UNKNOWN:Global.updateOnlyLanguage(this, null);break;
            }
        inspector=InspectorV3.searchInspector(this,null,null,1,Global.isByPopular(),resetDataset);
        inspector.setPage(1);
        inspector.start();
        Global.setTint(item.getIcon());
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
        this.actualPage=actualPage;
        this.totalPage=totalPage;
        if(Global.isInfiniteScroll())return;
        runOnUiThread(() -> {
            findViewById(R.id.page_switcher).setVisibility(totalPage<=1?View.GONE:View.VISIBLE);
            findViewById(R.id.prev).setAlpha(actualPage>1?1f:.5f);
            findViewById(R.id.prev).setEnabled(actualPage>1);
            findViewById(R.id.next).setAlpha(actualPage<totalPage?1f:.5f);
            findViewById(R.id.next).setEnabled(actualPage<totalPage);
            EditText text=findViewById(R.id.page_index);
            text.setText(String.format(Locale.US, "%d/%d", actualPage, totalPage));
        });

    }


    private void loadDialog(){
        DefaultDialogs.pageChangerDialog(
                new DefaultDialogs.Builder(this).setActual(actualPage).setMin(1).setMax(totalPage).setDialogs(new DefaultDialogs.DialogResults() {
                    @Override
                    public void positive(int actual) {
                        inspector=inspector.cloneInspector(MainActivity.this,resetDataset);
                        inspector.setPage(actual);
                        inspector.start();
                    }
                    @Override
                    public void negative() {}
                }).setTitle(R.string.change_page).setDrawable(R.drawable.ic_find_in_page)
        );
    }

    private Setting setting=null;
    private class Setting{
        final Global.ThemeScheme theme;
        final Locale locale;
        Setting() {
            this.theme = Global.getTheme();
            this.locale=Global.initLanguage(MainActivity.this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStringLogin();
        ACRA.getErrorReporter().setEnabled(getSharedPreferences("Settings",0).getBoolean(getString(R.string.key_send_report),true));
        com.dar.nclientv2.settings.Login.initUseAccountTag(this);
        loadStringLogin();
        if(com.dar.nclientv2.settings.Login.isLogged())navigationView.getMenu().findItem(R.id.online_favorite_manager).setVisible(true);
        if(setting!=null){
            Global.initFromShared(this);
            inspector=inspector.cloneInspector(this,resetDataset);
            inspector.start();
            if(setting.theme!=Global.getTheme()||!setting.locale.equals(Global.initLanguage(this)))recreate();
            adapter.notifyDataSetChanged();
            if(Global.isInfiniteScroll())hidePageSwitcher();
            changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
            setting=null;
        }
        invalidateOptionsMenu();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.by_popular).setIcon(Global.isByPopular()?R.drawable.ic_star_border:R.drawable.ic_access_time);
        menu.findItem(R.id.by_popular).setTitle(Global.isByPopular()?R.string.sort_by_latest:R.string.sort_by_popular);
        showLanguageIcon(menu.findItem(R.id.only_language));
        menu.findItem(R.id.only_language).setVisible(status==MainStatus.NORMAL);
        if(status==MainStatus.TAG){
                MenuItem item=menu.findItem(R.id.tag_manager).setVisible(inspector.getTag().getId()>0);
                TagStatus ts=inspector.getTag().getStatus();
                switch (ts){
                    case DEFAULT:item.setIcon(R.drawable.ic_help);break;
                    case AVOIDED:item.setIcon(R.drawable.ic_close);break;
                    case ACCEPTED:item.setIcon(R.drawable.ic_check);break;
                }
                Global.setTint(menu.findItem(R.id.tag_manager).getIcon());
        }else {
            Global.setTint(menu.findItem(R.id.search).getIcon());
        }
        Global.setTint(menu.findItem(R.id.open_browser).getIcon());
        Global.setTint(menu.findItem(R.id.by_popular).getIcon());
        Global.setTint(menu.findItem(R.id.add_bookmark).getIcon());
        if(status!=MainStatus.FAVORITE)menu.findItem(R.id.search).setActionView(null);
        else{
            ((SearchView)menu.findItem(R.id.search).getActionView()).setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    inspector=InspectorV3.favoriteInspector(MainActivity.this,query,1,resetDataset);
                    inspector.start();
                    getSupportActionBar().setTitle(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }
        return true;
    }

    private void showLanguageIcon(MenuItem item) {
        if(Global.getOnlyLanguage()==null){
            item.setTitle(R.string.all_languages);item.setIcon(R.drawable.ic_world);

        }
        else
            switch (Global.getOnlyLanguage()){
                case JAPANESE:item.setTitle(R.string.only_japanese);item.setIcon(R.drawable.ic_jpbw);break;
                case CHINESE:item.setTitle(R.string.only_chinese);item.setIcon(R.drawable.ic_cnbw);break;
                case UNKNOWN:item.setTitle(R.string.only_other);item.setIcon(R.drawable.ic_help);break;
                case ENGLISH:item.setTitle(R.string.only_english);item.setIcon(R.drawable.ic_gbbw);break;
            }
            Global.setTint(item.getIcon());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent i;
        switch (id){
            case R.id.by_popular:
                item.setIcon(Global.updateByPopular(this,!Global.isByPopular())?R.drawable.ic_star_border:R.drawable.ic_access_time);
                item.setTitle(Global.isByPopular()?R.string.sort_by_latest:R.string.sort_by_popular);
                Global.setTint(item.getIcon());
                inspector=inspector.cloneInspector(this,resetDataset);
                inspector.setByPopular(Global.isByPopular());
                inspector.start();
                break;
            case R.id.only_language:updateLanguageIcon(item);showLanguageIcon(item); break;
            case R.id.search:
                if(status!=MainStatus.FAVORITE) {
                    i = new Intent(this, SearchActivity.class);
                    startActivity(i);
                }
                break;
            case R.id.open_browser:
                if(inspector!=null) {
                    i = new Intent(Intent.ACTION_VIEW,Uri.parse(inspector.getUrl()));
                    startActivity(i);
                }
                break;
            case R.id.add_bookmark:
                Queries.BookmarkTable.addBookmark(Database.getDatabase(),inspector);
                break;
            case R.id.tag_manager:
                TagStatus ts=TagV2.updateStatus(inspector.getTag());
                switch (ts){
                    case DEFAULT:item.setIcon(R.drawable.ic_help);break;
                    case AVOIDED:item.setIcon(R.drawable.ic_close);break;
                    case ACCEPTED:item.setIcon(R.drawable.ic_check);break;
                }
                Global.setTint(item.getIcon());
                break;
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void manageQuery(String query, Set<Tag> tags){
        query=query.trim();
        if(query.length()==0&&tags==null)return;
        try {
                int id=Integer.parseInt(query);
                if(id>0&&id<=Global.getMaxId()){
                    InspectorV3.galleryInspector(this, id, startGallery );
                    return;
                }
        }catch (NumberFormatException ignore){}
        StringBuilder builder=new StringBuilder();
        builder.append(query);
        if(tags!=null)for(Tag t:tags){
            builder.append(' ').append(t.getName());
            if(builder.length()>50)break;
        }
        getSupportActionBar().setTitle(builder.toString());
        supportInvalidateOptionsMenu();

        Log.d(Global.LOGTAG,"TAGS: "+tags);
        inspector=InspectorV3.searchInspector(this, query, tags, 1, Global.isByPopular(), resetDataset);
        inspector.start();
    }

    private void showLogoutForm() {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_exit_to_app).setTitle(R.string.logout).setMessage(R.string.are_you_sure);
        builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
            Login.logout(MainActivity.this);
            navigationView.getMenu().findItem(R.id.online_favorite_manager).setVisible(false);
            loginItem.setTitle(R.string.login);

        }).setNegativeButton(R.string.no,null).show();
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        Intent intent;
        int id = item.getItemId();
        switch (id){

            case R.id.downloaded:if(Global.hasStoragePermission(this))startLocalActivity();else requestStorage();break;
            case R.id.action_login:
                if(com.dar.nclientv2.settings.Login.isLogged()){
                    showLogoutForm();

                }else {
                    intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                }
                break;
            case R.id.bookmarks:
                intent=new Intent(this,BookmarkActivity.class);
                startActivity(intent);
                break;
            case R.id.favorite_manager:
                intent=new Intent(this,FavoriteActivity.class);
                startActivity(intent);
                break;
            case R.id.action_settings:
                setting=new Setting();
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.online_favorite_manager:
                /*intent=new Intent(this,FavoriteActivity.class);
                intent.putExtra(getPackageName()+".ONLINE",true);
                startActivity(intent);*/
                intent=new Intent(this,MainActivity.class);
                intent.putExtra(getPackageName()+".FAVORITE",true);
                startActivity(intent);
                break;
            case R.id.random:
                intent = new Intent(this, RandomActivity.class);
                startActivity(intent);
                break;
            case R.id.tag_manager:
                intent=new Intent(this,TagFilter.class);
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
        Global.initStorage(this);
        if(requestCode==1&&grantResults.length >0&&grantResults[0]== PackageManager.PERMISSION_GRANTED) startLocalActivity();
        if(requestCode==2&&grantResults.length >0&&grantResults[0]== PackageManager.PERMISSION_GRANTED) new VersionChecker(this,true);
    }

    private void startLocalActivity(){
        Intent i=new Intent(this,LocalActivity.class);
        startActivity(i);
    }
}
