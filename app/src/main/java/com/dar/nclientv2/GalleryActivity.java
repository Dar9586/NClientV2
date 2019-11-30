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

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;

import com.dar.nclientv2.adapters.GalleryAdapter;
import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.async.DownloadGallery;
import com.dar.nclientv2.components.BaseActivity;
import com.dar.nclientv2.settings.Favorites;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class GalleryActivity extends BaseActivity{
    @NonNull private GenericGallery gallery;
    private boolean isLocal;
    private GalleryAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadThemeAndLanguage(this);
        setContentView(R.layout.activity_gallery);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        gallery= getIntent().getParcelableExtra(getPackageName()+".GALLERY");
        Log.d(Global.LOGTAG,""+gallery);
        if(Global.useRtl())recycler.setRotationY(180);
        if(getIntent().getBooleanExtra(getPackageName()+".INSTANTDOWNLOAD",false))downloadGallery();
        isLocal=getIntent().getBooleanExtra(getPackageName()+".ISLOCAL",false);
        int zoom=getIntent().getIntExtra(getPackageName()+".ZOOM",0);
        refresher.setEnabled(false);
        recycler.setLayoutManager(new GridLayoutManager(this,Global.getColumnCount()));

        Uri data = getIntent().getData();
        int isZoom=0;
        if(data != null && data.getPathSegments().size() >= 2){//if using an URL
            List<String> params = data.getPathSegments();
            for(String x:params)Log.i(Global.LOGTAG,x);
            if(params.size()>2){
                try{
                    isZoom=Integer.parseInt(params.get(2));
                }catch (NumberFormatException e){
                    Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
                }
            }
            InspectorV3.galleryInspector(this,Integer.parseInt(params.get(1)),new InspectorV3.DefaultInspectorResponse(){

                @Override
                public void onSuccess(List<Gallery> galleries) {
                    Intent intent = new Intent(GalleryActivity.this, ZoomActivity.class);
                    intent.putExtra(getPackageName()+".GALLERY",galleries.get(0));
                    intent.putExtra(getPackageName()+".PAGE",zoom);
                    startActivity(intent);
                    finish();
                }
            }).start();
        }else loadGallery(gallery,zoom);//if already has gallery

    }
    private void lookup(){
        GridLayoutManager manager= (GridLayoutManager)recycler.getLayoutManager();
        GalleryAdapter adapter=(GalleryAdapter)recycler.getAdapter();
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup(){
            @Override
            public int getSpanSize(int position){
                return adapter.positionToType(position)==GalleryAdapter.Type.PAGE?1:manager.getSpanCount();
            }
        });
    }
    private void loadGallery(GenericGallery gall,int zoom) {
        this.gallery=gall;
        if(getSupportActionBar()!=null)getSupportActionBar().setTitle(gallery.getTitle());
        adapter=new GalleryAdapter(this,gallery);
        recycler.setAdapter(adapter);
        lookup();
        if(zoom>0){
            Intent intent = new Intent(this, ZoomActivity.class);
            intent.putExtra(getPackageName()+".GALLERY",this.gallery);
            intent.putExtra(getPackageName()+".PAGE",zoom);
            startActivity(intent);
        }
    }


    private boolean isFavorite;
    private Menu menu;
    public void loadMenu(){
        if(menu.findItem(R.id.add_online_gallery).isVisible()){
            boolean x=Login.isOnlineFavorite(gallery.getId());
            menu.findItem(R.id.add_online_gallery).setTitle(x?R.string.remove_from_online_favorites:R.string.add_to_online_favorite);
            menu.findItem(R.id.add_online_gallery).setIcon(x?R.drawable.ic_star:R.drawable.ic_star_border);
        }
        menu.findItem(R.id.share).setVisible(gallery!=null&&gallery.isValid());
        menu.findItem(R.id.comments).setVisible(gallery!=null&&!gallery.isLocal());
        menu.findItem(R.id.favorite_manager).setIcon((isFavorite=Favorites.isFavorite(gallery))?R.drawable.ic_favorite:R.drawable.ic_favorite_border);
        menu.findItem(R.id.load_internet).setVisible(isLocal&&gallery!=null&&gallery.getId()!=-1);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gallery, menu);
        this.menu=menu;

        menu.findItem(R.id.add_online_gallery).setVisible(!isLocal&&Login.isLogged());
        menu.findItem(R.id.favorite_manager).setVisible(!isLocal||isFavorite);
        menu.findItem(R.id.download_gallery).setVisible(!isLocal);
        menu.findItem(R.id.related).setVisible(!isLocal);
        if(gallery!=null)loadMenu();
        Global.setTint(menu.findItem(R.id.download_gallery).getIcon());
        Global.setTint(menu.findItem(R.id.load_internet).getIcon());
        Global.setTint(menu.findItem(R.id.change_view).getIcon());
        Global.setTint(menu.findItem(R.id.share).getIcon());
        Global.setTint(menu.findItem(R.id.related).getIcon());
        Global.setTint(menu.findItem(R.id.favorite_manager).getIcon());
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
                    public void onResponse(@NonNull Call call,@NonNull Response response){
                        Log.d(Global.LOGTAG,"Called: ");
                        final boolean x=Login.isOnlineFavorite(gallery.getId());
                        if(!x)Login.saveOnlineFavorite((Gallery)gallery);
                        else Login.removeOnlineFavorite((Gallery)gallery);
                        GalleryActivity.this.runOnUiThread(() -> {
                            item.setIcon(x?R.drawable.ic_star_border:R.drawable.ic_star);
                            item.setTitle(x?R.string.add_to_online_favorite:R.string.remove_from_online_favorites);
                        });
                    }
                });
                break;
            case R.id.download_gallery:if(Global.hasStoragePermission(this))downloadGallery();else{requestStorage();}break;
            case R.id.change_view:updateColumnCount(true); break;
            case R.id.load_internet:toInternet();break;
            case R.id.comments:
                Intent i=new Intent(this, CommentActivity.class);
                i.putExtra(getPackageName()+".GALLERY",gallery);
                startActivity(i);
                break;

            case R.id.share:
                Global.shareGallery(this,gallery);
                break;
            case R.id.related:
                /*Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(getPackageName() + ".RELATED", gallery.getId());
                startActivity(intent);*/
                recycler.smoothScrollToPosition(recycler.getAdapter().getItemCount());
                break;
            case R.id.favorite_manager:
                if(isFavorite){
                    if(Favorites.removeFavorite((Gallery)gallery)) isFavorite=!isFavorite;
                }else if(Favorites.addFavorite((Gallery) gallery)){
                    isFavorite=!isFavorite;
                }else{
                    Snackbar.make(recycler,getString(R.string.favorite_max_reached,Favorites.MAXFAVORITE),Snackbar.LENGTH_LONG).show();
                }
                item.setIcon(isFavorite?R.drawable.ic_favorite:R.drawable.ic_favorite_border);
                Global.setTint(item.getIcon());
            break;
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateColumnCount(boolean increase) {
        int x=Global.getColumnCount();
        MenuItem item= ((Toolbar)findViewById(R.id.toolbar)).getMenu().findItem(R.id.change_view);
        if(increase||((GridLayoutManager)recycler.getLayoutManager()).getSpanCount()!=x){
            if(increase)x=x%4+1;
            int pos=((GridLayoutManager)recycler.getLayoutManager()).findFirstVisibleItemPosition();
            Global.updateColumnCount(this,x);

            recycler.setLayoutManager(new GridLayoutManager(this,x));
            Log.d(Global.LOGTAG,"Span count: "+((GridLayoutManager)recycler.getLayoutManager()).getSpanCount());
            if(adapter!=null){
                recycler.setAdapter(adapter);
                lookup();
                recycler.scrollToPosition(pos);
                adapter.setImageSize(null);

            }
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
        InspectorV3.galleryInspector(this, gallery.getId(), new InspectorV3.DefaultInspectorResponse() {
            @Override
            public void onSuccess(List<Gallery> galleries) {
                Intent intent=new Intent(GalleryActivity.this, GalleryActivity.class);
                Log.d(Global.LOGTAG,galleries.get(0).toString());
                intent.putExtra(getPackageName()+".GALLERY",galleries.get(0));
                runOnUiThread(()->startActivity(intent));
            }
        }).start();
    }

    @TargetApi(23)
    private void requestStorage(){
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},1);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            Global.initStorage(this);
            if(requestCode==1&&grantResults.length >0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
                downloadGallery();
                //new DownloadGallery(this,gallery).start();
    }
    private void downloadGallery(){
        Intent intent=new Intent(getApplicationContext(), DownloadGallery.class);
        intent.putExtra(getPackageName()+".GALLERY",gallery);
        startService(intent);
    }
}
