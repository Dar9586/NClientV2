package com.dar.nclientv2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.dar.nclientv2.adapters.GalleryAdapter;
import com.dar.nclientv2.api.Inspector;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.enums.ApiRequestType;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.DownloadGallery;
import com.dar.nclientv2.components.BaseActivity;
import com.dar.nclientv2.settings.Favorites;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.settings.Tags;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class GalleryActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private GenericGallery gallery;
    private boolean isLocal;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        Global.initHttpClient(this);
        Global.loadNotificationChannel(this);
        Global.initColumnCount(this);
        Tags.initTagSets(this);
        Global.initImageQuality(this);
        Favorites.countFavorite(this);
        Tags.initTagPreferencesSets(this);
        setContentView(R.layout.activity_gallery);
        Toolbar toolbar = findViewById(R.id.toolbar);
        final NavigationView navigationView = findViewById(R.id.nav_view);
        setSupportActionBar(toolbar);
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        gallery= getIntent().getParcelableExtra(getPackageName()+".GALLERY");
        Log.d(Global.LOGTAG,""+gallery);
        if(getIntent().getBooleanExtra(getPackageName()+".INSTANTDOWNLOAD",false))downloadGallery();
        isLocal=getIntent().getBooleanExtra(getPackageName()+".ISLOCAL",false);
        int zoom=getIntent().getIntExtra(getPackageName()+".ZOOM",0);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        refresher.setEnabled(false);
        recycler.setLayoutManager(new GridLayoutManager(this,Global.getColumnCount()));
        navigationView.setNavigationItemSelectedListener(this);


        Uri data = getIntent().getData();
        int isZoom=0;
        if(data != null && data.getPathSegments().size() >= 2){
            List<String> params = data.getPathSegments();
            for(String x:params)Log.i(Global.LOGTAG,x);
            if(params.size()>2){
                try{
                    isZoom=Integer.parseInt(params.get(2));
                }catch (NumberFormatException e){
                    Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
                }
            }
            new Inspector(this,isZoom,params.get(1),ApiRequestType.BYSINGLE);
        }else loadGallery(gallery,zoom);

    }
    private void loadGallery(GenericGallery gall,int zoom) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        this.gallery=gall;
        if(getSupportActionBar()!=null)getSupportActionBar().setTitle(gallery.getTitle());
        if(!gallery.isLocal()) {
            final Gallery gallery=(Gallery) this.gallery;
            for (final TagType x : TagType.values()) {
                int c = gallery.getTagCount(x);
                if(c==0) navigationView.getMenu().getItem(x.ordinal()).setVisible(false);
                for (int a = 0; a < c; a++) {
                    final int b = a;

                    MenuItem menuItem = navigationView.getMenu().getItem(x.ordinal()).getSubMenu().add(getIdFromTagType(x), Menu.NONE, a, getString(R.string.tag_format, gallery.getTag(x, a).getName(), gallery.getTag(x, a).getCount()));

                    menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Intent intent = new Intent(GalleryActivity.this, MainActivity.class);
                            intent.putExtra(getPackageName() + ".TAG", gallery.getTag(x, b));
                            GalleryActivity.this.startActivity(intent);
                            return true;
                        }
                    });
                }
            }
        }else{
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            toolbar.setNavigationIcon(null);
        }
        recycler.setAdapter(new GalleryAdapter(this,gallery));
        if(zoom>0){
            Intent intent = new Intent(this, ZoomActivity.class);
            intent.putExtra(getPackageName()+".GALLERY",this.gallery);
            intent.putExtra(getPackageName()+".PAGE",zoom);
            startActivity(intent);
        }
        if(!gall.isLocal()) {
            ((TextView) navigationView.getHeaderView(0).findViewById(R.id.title)).setText(gall.getTitle());
            ((TextView) navigationView.getHeaderView(0).findViewById(R.id.textView)).setText(getString(R.string.page_count_format, gall.getPageCount()));
            Global.loadImage(((Gallery) gall).getCover().getUrl(), (ImageView) navigationView.getHeaderView(0).findViewById(R.id.imageView));
        }
    }

    private int getIdFromTagType(TagType type){
        switch (type){
            case TAG:return R.id.tags;
            case PARODY:return R.id.parodies;
            case ARTIST:return R.id.artists;
            case GROUP:return R.id.groups;
            case CATEGORY:return R.id.categories;
            case LANGUAGE:return R.id.languages;
            case CHARACTER:return R.id.characters;
        }
        return R.id.unknown;
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
    private boolean isFavorite;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gallery, menu);
        if(!isLocal&&Login.isLogged()){
            MenuItem item= menu.findItem(R.id.add_online_gallery);
            item.setVisible(true);
            boolean x=gallery==null||Login.getOnlineFavorite(this,gallery.getId())==null;
            item.setIcon(x?R.drawable.ic_star_border:R.drawable.ic_star);
            item.setTitle(x?R.string.add_to_online_favorite:R.string.remove_from_online_favorites);
        }

        menu.findItem(R.id.add_online_gallery).setVisible(Login.isLogged());
        Global.setTint(menu.findItem(R.id.download_gallery).getIcon());
        Global.setTint(menu.findItem(R.id.load_internet).getIcon());
        Global.setTint(menu.findItem(R.id.change_view).getIcon());
        Global.setTint(menu.findItem(R.id.share).getIcon());
        Global.setTint(menu.findItem(R.id.related).getIcon());
        menu.findItem(R.id.share).setVisible(gallery!=null&&gallery.isValid());
        menu.findItem(R.id.favorite_manager).setIcon((isFavorite=Favorites.isFavorite(this,gallery))?R.drawable.ic_favorite:R.drawable.ic_favorite_border);
        Global.setTint(menu.findItem(R.id.favorite_manager).getIcon());
        menu.findItem(R.id.favorite_manager).setVisible(!isLocal||isFavorite);
        menu.findItem(R.id.download_gallery).setVisible(!isLocal);
        menu.findItem(R.id.related).setVisible(!isLocal);
        menu.findItem(R.id.load_internet).setVisible(isLocal&&gallery!=null&&gallery.getId()!=-1);
        updateColumnCount(false);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateColumnCount(false);
        if(isLocal)supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.add_online_gallery:
                if(gallery!=null)Global.client.newCall(new Request.Builder().url("https://nhentai.net/g/"+gallery.getId()+"/favorite").build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call,@NonNull IOException e) {}

                    @Override
                    public void onResponse(@NonNull Call call,@NonNull Response response)  {
                        Log.d(Global.LOGTAG,"Called");
                            final boolean x=Login.getOnlineFavorite(GalleryActivity.this,gallery.getId())==null;
                            if (x) Login.saveOnlineFavorite(GalleryActivity.this, (Gallery) gallery);
                            else Login.removeOnlineFavorite(GalleryActivity.this, gallery.getId());
                            GalleryActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    item.setIcon(x?R.drawable.ic_star:R.drawable.ic_star_border);
                                    item.setTitle(x?R.string.remove_from_online_favorites:R.string.add_to_online_favorite);
                                }
                            });
                    }
                });
                break;
            case R.id.download_gallery:if(Global.hasStoragePermission(this))downloadGallery();else{requestStorage();}break;
            case R.id.change_view:updateColumnCount(true); break;
            case R.id.load_internet:toInternet();break;
            case R.id.share:
                Global.shareGallery(this,gallery);
                break;
            case R.id.related:
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(getPackageName() + ".RELATED", gallery.getId());
                startActivity(intent);
                break;
            case R.id.favorite_manager:
                if(isFavorite){
                    if(Favorites.removeFavorite(this,gallery)) isFavorite=!isFavorite;
                }else if(Favorites.addFavorite(this,(Gallery) gallery)){
                    isFavorite=!isFavorite;
                }else{
                    Snackbar.make(recycler,getString(R.string.favorite_max_reached,Favorites.MAXFAVORITE),Snackbar.LENGTH_LONG).show();
                }
                item.setIcon(isFavorite?R.drawable.ic_favorite:R.drawable.ic_favorite_border);
                Global.setTint(item.getIcon());
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateColumnCount(boolean increase) {
        int x=Global.getColumnCount();
        MenuItem item= ((Toolbar)findViewById(R.id.toolbar)).getMenu().findItem(R.id.change_view);
        if(increase||((GridLayoutManager)recycler.getLayoutManager()).getSpanCount()!=x){
            if(increase)x=x%4+1;
            Global.updateColumnCount(this,x);
            RecyclerView.Adapter adapter=recycler.getAdapter();
            recycler.setLayoutManager(new GridLayoutManager(this,x));
            Log.d(Global.LOGTAG,"Span count: "+((GridLayoutManager)recycler.getLayoutManager()).getSpanCount());
            if(adapter!=null)recycler.setAdapter(adapter);
        }

        if(item!=null) {
            switch (x) {
                case 1: item.setIcon(R.drawable.ic_view_1);break;
                case 2: item.setIcon(R.drawable.ic_view_2);break;
                case 3: item.setIcon(R.drawable.ic_view_3);break;
                case 4: item.setIcon(R.drawable.ic_view_4);break;
            }
            Global.setTint(item.getIcon());
        }
    }

    private void toInternet() {
        refresher.setEnabled(true);
        new Inspector(this,0,Integer.toString(gallery.getId()), ApiRequestType.BYSINGLE);
    }

    @TargetApi(23)
    private void requestStorage(){
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},1);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if(requestCode==1&&grantResults.length >0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
                downloadGallery();
                //new DownloadGallery(this,gallery).start();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    private void downloadGallery(){
        Intent intent=new Intent(getApplicationContext(), DownloadGallery.class);
        intent.putExtra(getPackageName()+".GALLERY",gallery);
        startService(intent);
    }
}
