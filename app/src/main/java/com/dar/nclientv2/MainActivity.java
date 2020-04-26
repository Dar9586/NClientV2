package com.dar.nclientv2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dar.nclientv2.adapters.ListAdapter;
import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.ApiRequestType;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.ScrapeTags;
import com.dar.nclientv2.async.VersionChecker;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.async.downloader.DownloadGalleryV2;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.components.widgets.CustomGridLayoutManager;
import com.dar.nclientv2.loginapi.Login;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    /**
     * UNKNOWN in case of error
     * NORMAL when in main page
     * TAG when searching for a specific tag
     * FAVORITE when using online favorite button
     * SEARCH when used SeaarchActivity
     * BOOKMARK when loaded a bookmark
     * ID when searched for an ID
     * */
    private enum ModeType {UNKNOWN,NORMAL,TAG,FAVORITE,SEARCH,BOOKMARK,ID}
    private static final int CHANGE_LANGUAGE_DELAY=1000;

    private InspectorV3 inspector=null;
    private NavigationView navigationView;
    private static boolean firstTime=true;//true only when app starting
    private ModeType modeType = ModeType.UNKNOWN;
    private int actualPage=1,totalPage;
    private boolean inspecting=false;
    public ListAdapter adapter;


    private InspectorV3.InspectorResponse
            resetDataset=new InspectorV3.DefaultInspectorResponse() {
        @Override
        public void onSuccess(List<GenericGallery> galleries) {
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
    },
            addDataset=new InspectorV3.DefaultInspectorResponse() {
        @Override
        public void onSuccess(List<GenericGallery> galleries) {
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
    },
            startGallery=new InspectorV3.DefaultInspectorResponse() {
        @Override
        public void onSuccess(List<GenericGallery> galleries) {
            Gallery g=galleries.size()==1?(Gallery) galleries.get(0):Gallery.emptyGallery();
            Intent intent=new Intent(MainActivity.this, GalleryActivity.class);
            LogUtility.d(g.toString());
            intent.putExtra(getPackageName()+".GALLERY",g);
            runOnUiThread(()->{
                startActivity(intent);
                finish();
            });
            LogUtility.d("STARTED");
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

    //views
    public MenuItem loginItem,onlineFavoriteManager;
    private ImageButton prevPageButton,nextPageButton;
    private EditText pageIndexText;
    private View pageSwitcher;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;

    private Handler changeLanguageTimeHandler=new Handler();
    Runnable changeLanguageRunnable=() -> {
        useNormalMode();
        inspector.start();
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.activity_main);
        deleteAPKAfterUpdate();
        //load inspector
        selectStartMode(getIntent(),getPackageName());
        LogUtility.d("Main started with mode "+ modeType);
        //init views and actions
        findUsefulViews();
        initializeToolbar();
        initializeNavigationView();
        initializeRecyclerView();
        initializePageSwitcherActions();
        loadStringLogin();

        refresher.setOnRefreshListener(() -> {
            inspector = inspector.cloneInspector(MainActivity.this,resetDataset);
            if(Global.isInfiniteScroll())inspector.setPage(1);
            inspector.start();
        });

        manageDrawer();
        setActivityTitle();
        if(firstTime)checkUpdate();
        if(inspector!=null){
            inspector.start();
        }else{
            LogUtility.e(getIntent().getExtras());
        }
    }

    private void manageDrawer() {
        if(modeType != ModeType.NORMAL){
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }else{
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }
    }
    private void setActivityTitle() {
        switch (modeType){
            case FAVORITE:  getSupportActionBar().setTitle(R.string.favorite_online_manga);break;
            case SEARCH:    getSupportActionBar().setTitle(inspector.getSearchTitle());break;
            case TAG:       getSupportActionBar().setTitle(inspector.getTag().getName());break;
            case NORMAL:    getSupportActionBar().setTitle(R.string.app_name);break;
            default:        getSupportActionBar().setTitle("WTF");break;
        }
    }
    private void initializeToolbar() {
        setSupportActionBar(toolbar);
        ActionBar bar=getSupportActionBar();
        assert bar!=null;
        bar.setDisplayShowTitleEnabled(true);
        bar.setTitle(R.string.app_name);
    }
    private void initializePageSwitcherActions() {
        prevPageButton.setOnClickListener(v -> {
            if (actualPage > 1){
                inspector=inspector.cloneInspector(MainActivity.this,resetDataset);
                inspector.setPage(inspector.getPage()-1);
                inspector.start();
            }
        });
        nextPageButton.setOnClickListener(v -> {
            if (actualPage < totalPage){
                inspector=inspector.cloneInspector(MainActivity.this,resetDataset);
                inspector.setPage(inspector.getPage()+1);
                inspector.start();
            }
        });
        pageIndexText.setOnClickListener(v -> loadDialog());
    }
    private void initializeRecyclerView() {
        adapter=new ListAdapter(this);
        recycler.setAdapter(adapter);
        recycler.setHasFixedSize(true);
        //recycler.setItemViewCacheSize(24);
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
                if(inspecting)return;
                if(!Global.isInfiniteScroll())return;
                if(refresher.isRefreshing())return;

                CustomGridLayoutManager manager = (CustomGridLayoutManager)recycler.getLayoutManager();
                if(actualPage < totalPage && lastGalleryReached(manager)) {
                    inspecting=true;
                    inspector = inspector.cloneInspector(MainActivity.this,addDataset);
                    inspector.setPage(inspector.getPage()+1);
                    inspector.start();
                }
            }
        });
        changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
    }
    /**
     * Check if the last gallery has been shown
     **/
    private boolean lastGalleryReached(CustomGridLayoutManager manager) {
        return manager.findLastVisibleItemPosition() >= (recycler.getAdapter().getItemCount()-1-manager.getSpanCount());
    }



    private void initializeNavigationView() {
        changeNavigationImage(navigationView);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back));
        toolbar.setNavigationOnClickListener(v -> finish());
        navigationView.setNavigationItemSelectedListener(this);
        onlineFavoriteManager.setVisible(com.dar.nclientv2.settings.Login.isLogged());
    }

    private void findUsefulViews() {
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        prevPageButton=findViewById(R.id.prev);
        nextPageButton=findViewById(R.id.next);
        pageIndexText=findViewById(R.id.page_index);
        pageSwitcher=findViewById(R.id.page_switcher);
        drawerLayout = findViewById(R.id.drawer_layout);
        loginItem=navigationView.getMenu().findItem(R.id.action_login);
        onlineFavoriteManager=navigationView.getMenu().findItem(R.id.online_favorite_manager);
    }

    private void checkUpdate() {
            if(Global.shouldCheckForUpdates(this))
                new VersionChecker(this,true);
            ScrapeTags.startWork(this);
            firstTime=false;
    }

    private void deleteAPKAfterUpdate() {
        if(Global.hasStoragePermission(this)){//delete older APK
            final File f=new File(Global.UPDATEFOLDER,"NClientV2_"+Global.getVersionName(this)+".apk");
            LogUtility.d(f.getAbsolutePath());
            if(f.exists())f.delete();
        }
    }

    private void selectStartMode(Intent intent, String packageName) {
        Uri data=intent.getData();
             if(intent.getBooleanExtra(packageName+".ISBYTAG"    ,false))useTagMode(intent,packageName);
        else if(intent.getBooleanExtra(packageName+".SEARCHMODE" ,false))useSearchMode(intent,packageName);
        else if(intent.getBooleanExtra(packageName+".FAVORITE"   ,false))useFavoriteMode(1);
        else if(intent.getBooleanExtra(packageName+".BYBOOKMARK" ,false))useBookmarkMode(intent,packageName);
        else if(data!=null)manageDataStart(data);
        else useNormalMode();
    }

    private void useNormalMode() {
        inspector=InspectorV3.searchInspector(this,null,null,1,Global.isByPopular(),resetDataset);
        modeType = ModeType.NORMAL;
    }

    private void useBookmarkMode(Intent intent, String packageName) {
        inspector=intent.getParcelableExtra(packageName+".INSPECTOR");
        inspector.initialize(this,resetDataset);
        modeType = ModeType.BOOKMARK;
        ApiRequestType type=inspector.getRequestType();
             if(type==ApiRequestType.BYTAG)modeType = ModeType.TAG;
        else if(type==ApiRequestType.BYALL)modeType = ModeType.NORMAL;
        else if(type==ApiRequestType.BYSEARCH)modeType = ModeType.SEARCH;
        else if(type==ApiRequestType.FAVORITE)modeType = ModeType.FAVORITE;

    }

    private void useFavoriteMode(int page) {
        inspector=InspectorV3.favoriteInspector(this,null,page,resetDataset);
        modeType = ModeType.FAVORITE;
    }

    private void useSearchMode(Intent intent, String packageName) {
        String query=intent.getStringExtra(packageName+".QUERY");
        boolean ok=tryOpenId(query);
        if(!ok)createSearchInspector(intent,packageName,query);
    }

    private void createSearchInspector(Intent intent, String packageName, String query) {
        boolean advanced = intent.getBooleanExtra(packageName + ".ADVANCED", false);
        ArrayList<Tag>tagArrayList=intent.getParcelableArrayListExtra(packageName + ".TAGS");
        HashSet<Tag> tags = null;
        query = query.trim();
        if (advanced) {
            assert tagArrayList != null;//tags is always not null when advanced is set
            tags = new HashSet<>(tagArrayList);
        }
        inspector = InspectorV3.searchInspector(this, query, tags, 1, Global.isByPopular(), resetDataset);
        modeType=ModeType.SEARCH;
    }

    private boolean tryOpenId(String query) {
        try {
            int id=Integer.parseInt(query);
            inspector= InspectorV3.galleryInspector(this, id, startGallery );
            modeType=ModeType.ID;
            return true;
        }catch (NumberFormatException ignore){}
        return false;
    }

    private void useTagMode(Intent intent, String packageName) {
        Tag t=intent.getParcelableExtra(packageName+".TAG");
        inspector=InspectorV3.searchInspector(this,null,new HashSet<>(Collections.singleton(t)),1,Global.isByPopular(),resetDataset);
        modeType = ModeType.TAG;
    }

    /**
     * Load inspector from an URL, it can be either a tag or a search
     * */
    private void manageDataStart(Uri data) {
        List<String>datas=data.getPathSegments();
        TagType dataType;

        LogUtility.d("Datas: "+datas);
        if(datas.size()==0){
            useNormalMode();
            return;
        }
        dataType=TagType.typeByName(datas.get(0));
        if(dataType!=TagType.UNKNOWN)useDataTagMode(datas,dataType);
        else useDataSearchMode(data,datas);
    }

    private void useDataSearchMode(Uri data, List<String> datas) {
        String query=data.getQueryParameter("q");
        String pageParam=data.getQueryParameter("page");
        boolean favorite="favorites".equals(datas.get(0));
        boolean byPop="popular".equals(data.getQueryParameter("sort"));
        int page=1;

        if(pageParam!=null)page=Integer.parseInt(pageParam);

        if(favorite){
            if(com.dar.nclientv2.settings.Login.isLogged())useFavoriteMode(page);
            else{
                Intent intent=new Intent(this,FavoriteActivity.class);
                startActivity(intent);
                finish();
            }
            return;
        }

        inspector=InspectorV3.searchInspector(this,query,null,page,byPop,resetDataset);
        modeType= ModeType.SEARCH;
    }
    private void useDataTagMode(List<String> datas,TagType type) {
        String query=datas.get(1);
        boolean byPop=datas.size()==3;
        Tag tag=Queries.TagTable.getTagFromTagName(query);
        if(tag==null) tag=new Tag(query,-1,-1 ,type,TagStatus.DEFAULT);
        inspector=InspectorV3.searchInspector(this,null,new HashSet<>(Collections.singleton(tag)),1,byPop,resetDataset);
        modeType=ModeType.TAG;
    }

    private void loadStringLogin() {
        if(loginItem==null)return;
        if(com.dar.nclientv2.settings.Login.getUser()!=null)loginItem.setTitle(getString(R.string.login_formatted, com.dar.nclientv2.settings.Login.getUser().getUsername()));
        else loginItem.setTitle(com.dar.nclientv2.settings.Login.isLogged()?R.string.logout :R.string.login);

    }

    private void changeNavigationImage(NavigationView navigationView) {
        boolean light=Global.getTheme()== Global.ThemeScheme.LIGHT;
        View view=navigationView.getHeaderView(0);
        ImageView imageView=view.findViewById(R.id.imageView);
        View layoutHeader=view.findViewById(R.id.layout_header);
        Global.loadImage(light?R.drawable.ic_logo_dark :R.drawable.ic_logo,imageView);
        layoutHeader.setBackgroundResource(light?R.drawable.side_nav_bar_light:R.drawable.side_nav_bar_dark);
    }


    private void changeUsedLanguage(MenuItem item){
        switch (Global.getOnlyLanguage()){
            case ENGLISH:Global.updateOnlyLanguage(this, Language.JAPANESE);break;
            case JAPANESE:Global.updateOnlyLanguage(this, Language.CHINESE);break;
            case CHINESE:Global.updateOnlyLanguage(this, Language.ALL);break;
            case ALL:Global.updateOnlyLanguage(this, Language.ENGLISH);break;
        }
        //wait 250ms to reduce the requests
        changeLanguageTimeHandler.removeCallbacks(changeLanguageRunnable);
        changeLanguageTimeHandler.postDelayed(changeLanguageRunnable,CHANGE_LANGUAGE_DELAY);

    }
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
         else super.onBackPressed();
    }
    public void hidePageSwitcher(){
        runOnUiThread(()->pageSwitcher.setVisibility(View.GONE));
    }
    public void showPageSwitcher(final int actualPage,final int totalPage){
        this.actualPage=actualPage;
        this.totalPage=totalPage;

        if(Global.isInfiniteScroll()){
            hidePageSwitcher();
            return;
        }

        runOnUiThread(() -> {
            pageSwitcher.setVisibility(totalPage<=1?View.GONE:View.VISIBLE);
            prevPageButton.setAlpha(actualPage>1?1f:.5f);
            prevPageButton.setEnabled(actualPage>1);
            nextPageButton.setAlpha(actualPage<totalPage?1f:.5f);
            nextPageButton.setEnabled(actualPage<totalPage);
            pageIndexText.setText(String.format(Locale.US, "%d/%d", actualPage, totalPage));
        });

    }


    private void loadDialog(){
        DefaultDialogs.pageChangerDialog(
                new DefaultDialogs.Builder(this)
                        .setActual(actualPage)
                        .setMin(1)
                        .setMax(totalPage)
                        .setTitle(R.string.change_page)
                        .setDrawable(R.drawable.ic_find_in_page)
                        .setDialogs(new DefaultDialogs.DialogResults() {
                    @Override
                    public void positive(int actual) {
                        inspector=inspector.cloneInspector(MainActivity.this,resetDataset);
                        inspector.setPage(actual);
                        inspector.start();
                    }
                    @Override
                    public void negative() {}
                })
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
        Global.updateACRAReportStatus(this);
        com.dar.nclientv2.settings.Login.initUseAccountTag(this);
        loadStringLogin();
        onlineFavoriteManager.setVisible(com.dar.nclientv2.settings.Login.isLogged());

        if(setting!=null){
            Global.initFromShared(this);//restart all settings
            inspector=inspector.cloneInspector(this,resetDataset);
            inspector.start();//restart inspector
            if(setting.theme!=Global.getTheme()||!setting.locale.equals(Global.initLanguage(this))){
                Glide.with(getApplicationContext()).pauseAllRequestsRecursive();
                recreate();
            }
            adapter.notifyDataSetChanged();//restart adapter
            showPageSwitcher(inspector.getPage(),inspector.getPageCount());//restart page switcher
            changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
            setting=null;
        }
        invalidateOptionsMenu();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        popularItemDispay(menu.findItem(R.id.by_popular));

        showLanguageIcon(menu.findItem(R.id.only_language));

        menu.findItem(R.id.only_language).setVisible(modeType == ModeType.NORMAL);
        menu.findItem(R.id.random_favorite).setVisible(modeType == ModeType.FAVORITE);

        initializeSearchItem(menu.findItem(R.id.search));


        if(modeType == ModeType.TAG){
                MenuItem item=menu.findItem(R.id.tag_manager);
                item.setVisible(inspector.getTag().getId()>0);
                TagStatus ts=inspector.getTag().getStatus();
                updateTagStatus(item,ts);
        }
        Utility.tintMenu(menu);
        return true;
    }

    private void initializeSearchItem(MenuItem item) {
        if(modeType != ModeType.FAVORITE)
            item.setActionView(null);
        else{
            ((SearchView) item.getActionView()).setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
    }

    private void popularItemDispay(MenuItem item) {
        item.setIcon (Global.isByPopular()?R.drawable.ic_star_border:R.drawable.ic_access_time);
        item.setTitle(Global.isByPopular()?R.string.sort_by_latest  :R.string.sort_by_popular);
        Global.setTint(item.getIcon());
    }

    private void showLanguageIcon(MenuItem item) {
        switch (Global.getOnlyLanguage()){
            case JAPANESE:item.setTitle(R.string.only_japanese);item.setIcon(R.drawable.ic_jpbw);break;
            case CHINESE:item.setTitle(R.string.only_chinese);item.setIcon(R.drawable.ic_cnbw);break;
            case ENGLISH:item.setTitle(R.string.only_english);item.setIcon(R.drawable.ic_gbbw);break;
            case ALL:item.setTitle(R.string.all_languages);item.setIcon(R.drawable.ic_world);break;
        }
        Global.setTint(item.getIcon());
    }

    @Override
    protected int getPortCount() {
        return Global.getColPortMain();
    }

    @Override
    protected int getLandCount() {
        return Global.getColLandMain();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        LogUtility.d("Pressed item: "+item.getItemId());
        switch (item.getItemId()){
            case R.id.by_popular:
                Global.updateByPopular(this,!Global.isByPopular());
                popularItemDispay(item);
                inspector=inspector.cloneInspector(this,resetDataset);
                inspector.setByPopular(Global.isByPopular());
                inspector.start();
                break;
            case R.id.only_language:
                changeUsedLanguage(item);
                showLanguageIcon(item);
                break;
            case R.id.search:
                if(modeType != ModeType.FAVORITE) {//show textbox or start search activity
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
            case R.id.random_favorite:
                inspector=InspectorV3.randomInspector(this,startGallery,true);
                inspector.start();
                break;
            case R.id.download_page:
                if(inspector.getGalleries()!=null)
                    for(GenericGallery g:inspector.getGalleries())
                        DownloadGalleryV2.downloadGallery(this, g);
                break;
            case R.id.add_bookmark:
                Queries.BookmarkTable.addBookmark(inspector);
                break;
            case R.id.tag_manager:
                TagStatus ts=TagV2.updateStatus(inspector.getTag());
                updateTagStatus(item,ts);
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateTagStatus(MenuItem item, TagStatus ts) {
        switch (ts){
            case DEFAULT:item.setIcon(R.drawable.ic_help);break;
            case AVOIDED:item.setIcon(R.drawable.ic_close);break;
            case ACCEPTED:item.setIcon(R.drawable.ic_check);break;
        }
        Global.setTint(item.getIcon());
    }

    private void showLogoutForm() {
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(this);
        builder.setIcon(R.drawable.ic_exit_to_app).setTitle(R.string.logout).setMessage(R.string.are_you_sure);
        builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
            Login.logout();
            onlineFavoriteManager.setVisible(false);
            loginItem.setTitle(R.string.login);
        }).setNegativeButton(R.string.no,null).show();
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()){

            case R.id.downloaded:
                if(Global.hasStoragePermission(this))startLocalActivity();
                else requestStorage();
                break;
            case R.id.action_login:
                if(com.dar.nclientv2.settings.Login.isLogged())
                    showLogoutForm();
                else {
                    intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                }
                break;
            case R.id.bookmarks:
                intent=new Intent(this,BookmarkActivity.class);
                startActivity(intent);
                break;
            case R.id.history:
                intent=new Intent(this,HistoryActivity.class);
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
                intent=new Intent(this,MainActivity.class);
                intent.putExtra(getPackageName()+".FAVORITE",true);
                startActivity(intent);
                break;
            case R.id.random:
                intent = new Intent(this, RandomActivity.class);
                startActivity(intent);
                break;
            case R.id.tag_manager:
                intent=new Intent(this, TagFilterActivity.class);
                startActivity(intent);
                break;
        }
        //drawerLayout.closeDrawer(GravityCompat.START);
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
