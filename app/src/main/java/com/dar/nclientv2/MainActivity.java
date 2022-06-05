package com.dar.nclientv2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;
import com.dar.nclientv2.adapters.ListAdapter;
import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.components.Ranges;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.ApiRequestType;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.SortType;
import com.dar.nclientv2.api.enums.SpecialTagIds;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.ScrapeTags;
import com.dar.nclientv2.async.VersionChecker;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.async.downloader.DownloadGalleryV2;
import com.dar.nclientv2.components.GlideX;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.components.views.PageSwitcher;
import com.dar.nclientv2.components.widgets.CustomGridLayoutManager;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.settings.TagV2;
import com.dar.nclientv2.utility.ImageDownloadUtility;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends BaseActivity
    implements NavigationView.OnNavigationItemSelectedListener {
    private static final int MAX_FAIL_BEFORE_CLEAR_COOKIE = 3;
    private static final int CHANGE_LANGUAGE_DELAY = 1000;
    private static boolean firstTime = true;//true only when app starting
    private int failCount = 0;
    private final InspectorV3.InspectorResponse startGallery = new MainInspectorResponse() {
        @Override
        public void onSuccess(List<GenericGallery> galleries) {
            Gallery g = galleries.size() == 1 ? (Gallery) galleries.get(0) : Gallery.emptyGallery();
            Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
            LogUtility.d(g.toString());
            intent.putExtra(getPackageName() + ".GALLERY", g);
            runOnUiThread(() -> {
                startActivity(intent);
                finish();
            });
            LogUtility.d("STARTED");
        }
    };
    private final Handler changeLanguageTimeHandler = new Handler(Looper.myLooper());
    public ListAdapter adapter;
    private final InspectorV3.InspectorResponse addDataset = new MainInspectorResponse() {
        @Override
        public void onSuccess(List<GenericGallery> galleries) {
            adapter.addGalleries(galleries);
        }
    };
    //views
    public MenuItem loginItem, onlineFavoriteManager;
    private InspectorV3 inspector = null;
    private NavigationView navigationView;
    private ModeType modeType = ModeType.UNKNOWN;
    private int idOpenedGallery = -1;//Position in the recycler of the opened gallery
    private boolean inspecting = false, filteringTag = false;
    private SortType temporaryType;
    private Snackbar snackbar = null;
    private boolean showedCaptcha = false, noNeedForCaptcha = false;
    private PageSwitcher pageSwitcher;
    private final InspectorV3.InspectorResponse
        resetDataset = new MainInspectorResponse() {
        @Override
        public void onSuccess(List<GenericGallery> galleries) {
            super.onSuccess(galleries);
            adapter.restartDataset(galleries);
            showPageSwitcher(inspector.getPage(), inspector.getPageCount());
            runOnUiThread(() -> recycler.smoothScrollToPosition(0));
        }
    };
    final Runnable changeLanguageRunnable = () -> {
        useNormalMode();
        inspector.start();
    };
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private Setting setting = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //load inspector
        selectStartMode(getIntent(), getPackageName());
        LogUtility.d("Main started with mode " + modeType);
        //init views and actions
        findUsefulViews();
        initializeToolbar();
        initializeNavigationView();
        initializeRecyclerView();
        initializePageSwitcherActions();
        loadStringLogin();
        refresher.setOnRefreshListener(() -> {
            inspector = inspector.cloneInspector(MainActivity.this, resetDataset);
            if (Global.isInfiniteScrollMain()) inspector.setPage(1);
            inspector.start();
        });

        manageDrawer();
        setActivityTitle();
        if (firstTime) checkUpdate();
        if (inspector != null) {
            inspector.start();
        } else {
            LogUtility.e(getIntent().getExtras());
        }
    }

    private void manageDrawer() {
        if (modeType != ModeType.NORMAL) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }
    }

    private void setActivityTitle() {
        switch (modeType) {
            case FAVORITE:
                getSupportActionBar().setTitle(R.string.favorite_online_manga);
                break;
            case SEARCH:
                getSupportActionBar().setTitle(inspector.getSearchTitle());
                break;
            case TAG:
                getSupportActionBar().setTitle(inspector.getTag().getName());
                break;
            case NORMAL:
                getSupportActionBar().setTitle(R.string.app_name);
                break;
            default:
                getSupportActionBar().setTitle("WTF");
                break;
        }
    }

    private void initializeToolbar() {
        setSupportActionBar(toolbar);
        ActionBar bar = getSupportActionBar();
        assert bar != null;
        bar.setDisplayShowTitleEnabled(true);
        bar.setTitle(R.string.app_name);
    }

    private void initializePageSwitcherActions() {
        pageSwitcher.setChanger(new PageSwitcher.DefaultPageChanger() {
            @Override
            public void pageChanged(PageSwitcher switcher, int page) {
                inspector = inspector.cloneInspector(MainActivity.this, resetDataset);
                inspector.setPage(pageSwitcher.getActualPage());
                inspector.start();
            }
        });
    }

    private void initializeRecyclerView() {
        adapter = new ListAdapter(this);
        recycler.setAdapter(adapter);
        recycler.setHasFixedSize(true);
        //recycler.setItemViewCacheSize(24);
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (inspecting) return;
                if (!Global.isInfiniteScrollMain()) return;
                if (refresher.isRefreshing()) return;

                CustomGridLayoutManager manager = (CustomGridLayoutManager) recycler.getLayoutManager();
                assert manager != null;
                if (!pageSwitcher.lastPageReached() && lastGalleryReached(manager)) {
                    inspecting = true;
                    inspector = inspector.cloneInspector(MainActivity.this, addDataset);
                    inspector.setPage(inspector.getPage() + 1);
                    inspector.start();
                }
            }
        });
        changeLayout(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    /**
     * Check if the last gallery has been shown
     **/
    private boolean lastGalleryReached(CustomGridLayoutManager manager) {
        return manager.findLastVisibleItemPosition() >= (recycler.getAdapter().getItemCount() - 1 - manager.getSpanCount());
    }

    private void initializeNavigationView() {
        changeNavigationImage(navigationView);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        navigationView.setNavigationItemSelectedListener(this);
        onlineFavoriteManager.setVisible(com.dar.nclientv2.settings.Login.isLogged());
    }

    public void setIdOpenedGallery(int idOpenedGallery) {
        this.idOpenedGallery = idOpenedGallery;
    }

    private void findUsefulViews() {
        masterLayout = findViewById(R.id.master_layout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);
        recycler = findViewById(R.id.recycler);
        refresher = findViewById(R.id.refresher);
        pageSwitcher = findViewById(R.id.page_switcher);
        drawerLayout = findViewById(R.id.drawer_layout);
        loginItem = navigationView.getMenu().findItem(R.id.action_login);
        onlineFavoriteManager = navigationView.getMenu().findItem(R.id.online_favorite_manager);
    }

    private void loadStringLogin() {
        if (loginItem == null) return;
        if (com.dar.nclientv2.settings.Login.getUser() != null)
            loginItem.setTitle(getString(R.string.login_formatted, com.dar.nclientv2.settings.Login.getUser().getUsername()));
        else
            loginItem.setTitle(com.dar.nclientv2.settings.Login.isLogged() ? R.string.logout : R.string.login);

    }

    private void hideError() {
        //errorText.setVisibility(View.GONE);
        if (snackbar != null && snackbar.isShown()) runOnUiThread(() -> snackbar.dismiss());
        snackbar = null;
    }

    private void showError(@Nullable String text, @Nullable View.OnClickListener listener) {
        if (text == null) {
            hideError();
            return;
        }
        if (listener == null) {
            snackbar = Snackbar.make(masterLayout, text, Snackbar.LENGTH_SHORT);
        } else {
            snackbar = Snackbar.make(masterLayout, text, Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.retry, listener);
        }
        snackbar.show();
    }

    private void showError(@StringRes int text, View.OnClickListener listener) {
        showError(getString(text), listener);
    }

    private void checkUpdate() {
        if (Global.shouldCheckForUpdates(this))
            new VersionChecker(this, true);
        ScrapeTags.startWork(this);
        firstTime = false;
    }

    private void selectStartMode(Intent intent, String packageName) {
        Uri data = intent.getData();
        if (intent.getBooleanExtra(packageName + ".ISBYTAG", false))
            useTagMode(intent, packageName);
        else if (intent.getBooleanExtra(packageName + ".SEARCHMODE", false))
            useSearchMode(intent, packageName);
        else if (intent.getBooleanExtra(packageName + ".FAVORITE", false)) useFavoriteMode(1);
        else if (intent.getBooleanExtra(packageName + ".BYBOOKMARK", false))
            useBookmarkMode(intent, packageName);
        else if (data != null) manageDataStart(data);
        else useNormalMode();
    }

    private void useNormalMode() {
        inspector = InspectorV3.basicInspector(this, 1, resetDataset);
        modeType = ModeType.NORMAL;
    }

    private void useBookmarkMode(Intent intent, String packageName) {
        inspector = intent.getParcelableExtra(packageName + ".INSPECTOR");
        assert inspector != null;
        inspector.initialize(this, resetDataset);
        modeType = ModeType.BOOKMARK;
        ApiRequestType type = inspector.getRequestType();
        if (type == ApiRequestType.BYTAG) modeType = ModeType.TAG;
        else if (type == ApiRequestType.BYALL) modeType = ModeType.NORMAL;
        else if (type == ApiRequestType.BYSEARCH) modeType = ModeType.SEARCH;
        else if (type == ApiRequestType.FAVORITE) modeType = ModeType.FAVORITE;

    }

    private void useFavoriteMode(int page) {
        //instantiateWebView();
        inspector = InspectorV3.favoriteInspector(this, null, page, resetDataset);
        modeType = ModeType.FAVORITE;
    }

    private void useSearchMode(Intent intent, String packageName) {
        String query = intent.getStringExtra(packageName + ".QUERY");
        boolean ok = tryOpenId(query);
        if (!ok) createSearchInspector(intent, packageName, query);
    }

    private void createSearchInspector(Intent intent, String packageName, String query) {
        boolean advanced = intent.getBooleanExtra(packageName + ".ADVANCED", false);
        ArrayList<Tag> tagArrayList = intent.getParcelableArrayListExtra(packageName + ".TAGS");
        Ranges ranges = intent.getParcelableExtra(getPackageName() + ".RANGES");
        HashSet<Tag> tags = null;
        query = query.trim();
        if (advanced) {
            assert tagArrayList != null;//tags is always not null when advanced is set
            tags = new HashSet<>(tagArrayList);
        }
        inspector = InspectorV3.searchInspector(this, query, tags, 1, Global.getSortType(), ranges, resetDataset);
        modeType = ModeType.SEARCH;
    }

    private boolean tryOpenId(String query) {
        try {
            int id = Integer.parseInt(query);
            inspector = InspectorV3.galleryInspector(this, id, startGallery);
            modeType = ModeType.ID;
            return true;
        } catch (NumberFormatException ignore) {
        }
        return false;
    }

    private void useTagMode(Intent intent, String packageName) {
        Tag t = intent.getParcelableExtra(packageName + ".TAG");
        inspector = InspectorV3.tagInspector(this, t, 1, Global.getSortType(), resetDataset);
        modeType = ModeType.TAG;
    }

    /**
     * Load inspector from an URL, it can be either a tag or a search
     */
    private void manageDataStart(Uri data) {
        List<String> datas = data.getPathSegments();
        TagType dataType;

        LogUtility.d("Datas: " + datas);
        if (datas.size() == 0) {
            useNormalMode();
            return;
        }
        dataType = TagType.typeByName(datas.get(0));
        if (dataType != TagType.UNKNOWN) useDataTagMode(datas, dataType);
        else useDataSearchMode(data, datas);
    }

    private void useDataSearchMode(Uri data, List<String> datas) {
        String query = data.getQueryParameter("q");
        String pageParam = data.getQueryParameter("page");
        boolean favorite = "favorites".equals(datas.get(0));
        SortType type = SortType.findFromAddition(data.getQueryParameter("sort"));
        int page = 1;

        if (pageParam != null) page = Integer.parseInt(pageParam);

        if (favorite) {
            if (com.dar.nclientv2.settings.Login.isLogged()) useFavoriteMode(page);
            else {
                Intent intent = new Intent(this, FavoriteActivity.class);
                startActivity(intent);
                finish();
            }
            return;
        }

        inspector = InspectorV3.searchInspector(this, query, null, page, type, null, resetDataset);
        modeType = ModeType.SEARCH;
    }

    private void useDataTagMode(List<String> datas, TagType type) {
        String query = datas.get(1);
        Tag tag = Queries.TagTable.getTagFromTagName(query);
        if (tag == null)
            tag = new Tag(query, -1, SpecialTagIds.INVALID_ID, type, TagStatus.DEFAULT);
        SortType sortType = SortType.RECENT_ALL_TIME;
        if (datas.size() == 3) {
            sortType = SortType.findFromAddition(datas.get(2));
        }
        inspector = InspectorV3.tagInspector(this, tag, 1, sortType, resetDataset);
        modeType = ModeType.TAG;
    }

    private void changeNavigationImage(NavigationView navigationView) {
        boolean light = Global.getTheme() == Global.ThemeScheme.LIGHT;
        View view = navigationView.getHeaderView(0);
        ImageView imageView = view.findViewById(R.id.imageView);
        View layoutHeader = view.findViewById(R.id.layout_header);
        ImageDownloadUtility.loadImage(light ? R.drawable.ic_logo_dark : R.drawable.ic_logo, imageView);
        layoutHeader.setBackgroundResource(light ? R.drawable.side_nav_bar_light : R.drawable.side_nav_bar_dark);
    }

    private void changeUsedLanguage(MenuItem item) {
        switch (Global.getOnlyLanguage()) {
            case ENGLISH:
                Global.updateOnlyLanguage(this, Language.JAPANESE);
                break;
            case JAPANESE:
                Global.updateOnlyLanguage(this, Language.CHINESE);
                break;
            case CHINESE:
                Global.updateOnlyLanguage(this, Language.ALL);
                break;
            case ALL:
                Global.updateOnlyLanguage(this, Language.ENGLISH);
                break;
        }
        //wait 250ms to reduce the requests
        changeLanguageTimeHandler.removeCallbacks(changeLanguageRunnable);
        changeLanguageTimeHandler.postDelayed(changeLanguageRunnable, CHANGE_LANGUAGE_DELAY);

    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }

    public void hidePageSwitcher() {
        runOnUiThread(() -> pageSwitcher.setVisibility(View.GONE));
    }

    public void showPageSwitcher(final int actualPage, final int totalPage) {
        pageSwitcher.setPages(totalPage, actualPage);


        if (Global.isInfiniteScrollMain()) {
            hidePageSwitcher();
        }

    }


    private void showLogoutForm() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setIcon(R.drawable.ic_exit_to_app).setTitle(R.string.logout).setMessage(R.string.are_you_sure);
        builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
            Login.logout(this);
            onlineFavoriteManager.setVisible(false);
            loginItem.setTitle(R.string.login);
        }).setNegativeButton(R.string.no, null).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Global.updateACRAReportStatus(this);
        com.dar.nclientv2.settings.Login.initLogin(this);
        if (idOpenedGallery != -1) {
            adapter.updateColor(idOpenedGallery);
            idOpenedGallery = -1;
        }
        loadStringLogin();
        onlineFavoriteManager.setVisible(com.dar.nclientv2.settings.Login.isLogged());
        if (!noNeedForCaptcha) {
            if (Login.hasCookie("csrftoken")) {
                inspector = inspector.cloneInspector(this, resetDataset);
                inspector.start();//restart inspector
                noNeedForCaptcha = true;
            }
        }
        if (setting != null) {
            Global.initFromShared(this);//restart all settings
            inspector = inspector.cloneInspector(this, resetDataset);
            inspector.start();//restart inspector
            if (setting.theme != Global.getTheme() || !Objects.equals(setting.locale, Global.initLanguage(this))) {
                RequestManager manager = GlideX.with(getApplicationContext());
                if (manager != null) manager.pauseAllRequestsRecursive();
                recreate();
            }
            adapter.notifyDataSetChanged();//restart adapter
            adapter.resetStatuses();
            showPageSwitcher(inspector.getPage(), inspector.getPageCount());//restart page switcher
            changeLayout(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
            setting = null;
        } else if (filteringTag) {
            inspector = InspectorV3.basicInspector(this, 1, resetDataset);
            inspector.start();
            filteringTag = false;
        }
        invalidateOptionsMenu();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        popularItemDispay(menu.findItem(R.id.by_popular));

        showLanguageIcon(menu.findItem(R.id.only_language));

        menu.findItem(R.id.only_language).setVisible(modeType == ModeType.NORMAL);
        menu.findItem(R.id.random_favorite).setVisible(modeType == ModeType.FAVORITE);

        initializeSearchItem(menu.findItem(R.id.search));


        if (modeType == ModeType.TAG) {
            MenuItem item = menu.findItem(R.id.tag_manager);
            item.setVisible(inspector.getTag().getId() > 0);
            TagStatus ts = inspector.getTag().getStatus();
            updateTagStatus(item, ts);
        }
        Utility.tintMenu(menu);
        return true;
    }

    private void initializeSearchItem(MenuItem item) {
        if (modeType != ModeType.FAVORITE)
            item.setActionView(null);
        else {
            ((SearchView) item.getActionView()).setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    inspector = InspectorV3.favoriteInspector(MainActivity.this, query, 1, resetDataset);
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
        item.setTitle(getString(R.string.sort_type_title_format, getString(Global.getSortType().getNameId())));
        Global.setTint(item.getIcon());
    }

    private void showLanguageIcon(MenuItem item) {
        switch (Global.getOnlyLanguage()) {
            case JAPANESE:
                item.setTitle(R.string.only_japanese);
                item.setIcon(R.drawable.ic_jpbw);
                break;
            case CHINESE:
                item.setTitle(R.string.only_chinese);
                item.setIcon(R.drawable.ic_cnbw);
                break;
            case ENGLISH:
                item.setTitle(R.string.only_english);
                item.setIcon(R.drawable.ic_gbbw);
                break;
            case ALL:
                item.setTitle(R.string.all_languages);
                item.setIcon(R.drawable.ic_world);
                break;
        }
        Global.setTint(item.getIcon());
    }

    @Override
    protected int getPortraitColumnCount() {
        return Global.getColPortMain();
    }

    @Override
    protected int getLandscapeColumnCount() {
        return Global.getColLandMain();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        LogUtility.d("Pressed item: " + item.getItemId());
        if (item.getItemId() == R.id.by_popular) {
            updateSortType(item);
        } else if (item.getItemId() == R.id.only_language) {
            changeUsedLanguage(item);
            showLanguageIcon(item);
        } else if (item.getItemId() == R.id.search) {
            if (modeType != ModeType.FAVORITE) {//show textbox or start search activity
                i = new Intent(this, SearchActivity.class);
                startActivity(i);
            }
        } else if (item.getItemId() == R.id.open_browser) {
            if (inspector != null) {
                i = new Intent(Intent.ACTION_VIEW, Uri.parse(inspector.getUrl()));
                startActivity(i);
            }
        } else if (item.getItemId() == R.id.random_favorite) {
            inspector = InspectorV3.randomInspector(this, startGallery, true);
            inspector.start();
        } else if (item.getItemId() == R.id.download_page) {
            if (inspector.getGalleries() != null)
                showDialogDownloadAll();
        } else if (item.getItemId() == R.id.add_bookmark) {
            Queries.BookmarkTable.addBookmark(inspector);
        } else if (item.getItemId() == R.id.tag_manager) {
            TagStatus ts = TagV2.updateStatus(inspector.getTag());
            updateTagStatus(item, ts);
        } else if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateSortType(MenuItem item) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        for (SortType type : SortType.values())
            adapter.add(getString(type.getNameId()));
        temporaryType = Global.getSortType();
        builder.setIcon(R.drawable.ic_sort).setTitle(R.string.sort_select_type);
        builder.setSingleChoiceItems(adapter, temporaryType.ordinal(), (dialog, which) -> temporaryType = SortType.values()[which]);
        builder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                temporaryType = SortType.values()[position];
                parent.setSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            Global.updateSortType(MainActivity.this, temporaryType);
            popularItemDispay(item);
            inspector = inspector.cloneInspector(MainActivity.this, resetDataset);
            inspector.setSortType(temporaryType);
            inspector.start();
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showDialogDownloadAll() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder
            .setTitle(R.string.download_all_galleries_in_this_page)
            .setIcon(R.drawable.ic_file)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok, (dialog, which) -> {
                for (GenericGallery g : inspector.getGalleries())
                    DownloadGalleryV2.downloadGallery(MainActivity.this, g);
            });
        builder.show();
    }

    private void updateTagStatus(MenuItem item, TagStatus ts) {
        switch (ts) {
            case DEFAULT:
                item.setIcon(R.drawable.ic_help);
                break;
            case AVOIDED:
                item.setIcon(R.drawable.ic_close);
                break;
            case ACCEPTED:
                item.setIcon(R.drawable.ic_check);
                break;
        }
        Global.setTint(item.getIcon());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent;
        if (item.getItemId() == R.id.downloaded) {
            if (Global.hasStoragePermission(this)) startLocalActivity();
            else requestStorage();
        } else if (item.getItemId() == R.id.bookmarks) {
            intent = new Intent(this, BookmarkActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.history) {
            intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);

        } else if (item.getItemId() == R.id.favorite_manager) {
            intent = new Intent(this, FavoriteActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_settings) {
            setting = new Setting();
            intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.online_favorite_manager) {
            intent = new Intent(this, MainActivity.class);
            intent.putExtra(getPackageName() + ".FAVORITE", true);
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_login) {
            if (Login.isLogged())
                showLogoutForm();
            else {
                intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            }
        } else if (item.getItemId() == R.id.random) {
            intent = new Intent(this, RandomActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.tag_manager) {
            intent = new Intent(this, TagFilterActivity.class);
            filteringTag = true;
            startActivity(intent);
        } else if (item.getItemId() == R.id.status_manager) {
            intent = new Intent(this, StatusViewerActivity.class);
            startActivity(intent);
        }
        //drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestStorage() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Global.initStorage(this);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            startLocalActivity();
        if (requestCode == 2 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            new VersionChecker(this, true);
    }

    private void startLocalActivity() {
        Intent i = new Intent(this, LocalActivity.class);
        startActivity(i);
    }

    /**
     * UNKNOWN in case of error
     * NORMAL when in main page
     * TAG when searching for a specific tag
     * FAVORITE when using online favorite button
     * SEARCH when used SearchActivity
     * BOOKMARK when loaded a bookmark
     * ID when searched for an ID
     */
    private enum ModeType {UNKNOWN, NORMAL, TAG, FAVORITE, SEARCH, BOOKMARK, ID}

    abstract class MainInspectorResponse extends InspectorV3.DefaultInspectorResponse {
        @Override
        public void onSuccess(List<GenericGallery> galleries) {
            super.onSuccess(galleries);
            if (adapter != null) adapter.resetStatuses();
            noNeedForCaptcha = true;
            if (galleries.size() == 0)
                showError(R.string.no_entry_found, null);
        }

        @Override
        public void onStart() {
            runOnUiThread(() -> refresher.setRefreshing(true));
            hideError();
        }

        @Override
        public void onEnd() {
            runOnUiThread(() -> refresher.setRefreshing(false));
            inspecting = false;
        }

        @Override
        public void onFailure(Exception e) {
            super.onFailure(e);
            if (e instanceof InspectorV3.InvalidResponseException) {
                failCount += 1;
                if (failCount == MAX_FAIL_BEFORE_CLEAR_COOKIE || (!noNeedForCaptcha && !showedCaptcha)) {
                    Login.removeCloudflareCookies();
                    failCount = 0;
                    showedCaptcha = true;
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.putExtra(getPackageName() + ".IS_CAPTCHA", true);
                    startActivity(intent);
                }
                showError(R.string.invalid_response, v -> {
                    inspector = inspector.cloneInspector(MainActivity.this, inspector.getResponse());
                    inspector.start();
                });
            } else {
                showError(R.string.unable_to_connect_to_the_site, v -> {
                    inspector = inspector.cloneInspector(MainActivity.this, inspector.getResponse());
                    inspector.start();
                });
            }
        }

        @Override
        public boolean shouldStart(InspectorV3 inspector) {
            return true;
            //loadWebVewUrl(inspector.getUrl());
            //return inspector.canParseDocument();
        }
    }

    private class Setting {
        final Global.ThemeScheme theme;
        final Locale locale;

        Setting() {
            this.theme = Global.getTheme();
            this.locale = Global.initLanguage(MainActivity.this);
        }
    }
}
