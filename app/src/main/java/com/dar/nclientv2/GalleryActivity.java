package com.dar.nclientv2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.dar.nclientv2.adapters.GalleryAdapter;
import com.dar.nclientv2.api.Inspector;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.enums.ApiRequestType;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.DownloadGallery;
import com.dar.nclientv2.components.BaseActivity;
import com.dar.nclientv2.settings.Global;

public class GalleryActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private GenericGallery gallery;
    private boolean isLocal;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        setContentView(R.layout.activity_gallery);
        Toolbar toolbar = findViewById(R.id.toolbar);
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        setSupportActionBar(toolbar);
        gallery= getIntent().getParcelableExtra(getPackageName()+".GALLERY");
        if(getIntent().getBooleanExtra(getPackageName()+".INSTANTDOWNLOAD",false))downloadGallery();
        isLocal=getIntent().getBooleanExtra(getPackageName()+".ISLOCAL",false);
        getSupportActionBar().setTitle(gallery.getTitle());
        Log.d(Global.LOGTAG,gallery+"");
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        refresher.setEnabled(false);
        NavigationView navigationView = findViewById(R.id.nav_view);
        if(!gallery.isLocal()) {
            final Gallery gallery=(Gallery) this.gallery;
            for (TagType x : TagType.values()) {
                int c = gallery.getTagCount(x);
                final TagType y = x;
                for (int a = 0; a < c; a++) {
                    final int b = a;

                    MenuItem menuItem = navigationView.getMenu().getItem(x.ordinal()).getSubMenu().add(getIdFromTagType(x), Menu.NONE, a, getString(R.string.tag_format, gallery.getTag(x, a).getName(), gallery.getTag(x, a).getCount()));
                    menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Intent intent = new Intent(GalleryActivity.this, MainActivity.class);
                            intent.putExtra(getPackageName() + ".TAG", gallery.getTag(y, b).toQueryTag(TagStatus.DEFAULT));
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
        navigationView.setNavigationItemSelectedListener(this);
        recycler.setLayoutManager(new GridLayoutManager(this,Global.getColumnCount()));
        recycler.setAdapter(new GalleryAdapter(this,gallery));
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gallery, menu);
        Global.setTint(menu.findItem(R.id.download_gallery).getIcon());
        Global.setTint(menu.findItem(R.id.load_internet).getIcon());
        Global.setTint(menu.findItem(R.id.change_view).getIcon());
        menu.findItem(R.id.download_gallery).setVisible(!isLocal);
        menu.findItem(R.id.load_internet).setVisible(isLocal&&gallery.getId()!=-1);
        updateColumnCount(false);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateColumnCount(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.download_gallery:if(Global.hasStoragePermission(this))downloadGallery();else{requestStorage();}break;
            case R.id.change_view:updateColumnCount(true);Global.setTint(item.getIcon()); break;
            case R.id.load_internet:toInternet();break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateColumnCount(boolean increase) {
        MenuItem item= ((Toolbar)findViewById(R.id.toolbar)).getMenu().findItem(R.id.change_view);
        int x=Global.getColumnCount();
        if(increase){
            x=x%4+1;
            Global.updateColumnCount(this,x);
        }
        RecyclerView.Adapter adapter=recycler.getAdapter();
        recycler.setLayoutManager(new GridLayoutManager(this,x));
        if(adapter!=null)recycler.setAdapter(adapter);
        if(item!=null)
        switch (x){
            case 1:item.setIcon(R.drawable.ic_view_1);break;
            case 2:item.setIcon(R.drawable.ic_view_2);break;
            case 3:item.setIcon(R.drawable.ic_view_3);break;
            case 4:item.setIcon(R.drawable.ic_view_4);break;
        }
    }

    private void toInternet() {
        refresher.setEnabled(true);
        new Inspector(this,1,Integer.toString(gallery.getId()), ApiRequestType.BYSINGLE);
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
        int id = item.getItemId();


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
