package com.dar.nclientv2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.adapters.GalleryAdapter;
import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.components.views.RangeSelector;
import com.dar.nclientv2.components.widgets.CustomGridLayoutManager;
import com.dar.nclientv2.settings.Favorites;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.utility.CSRFGet;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.snackbar.Snackbar;

import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

import java.io.IOException;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GalleryActivity extends BaseActivity{
    @NonNull private GenericGallery gallery=Gallery.emptyGallery();
    private boolean isLocal;
    private GalleryAdapter adapter;
    private int zoom;
    private boolean isLocalFavorite;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.activity_gallery);
        if(Global.isLockScreen())getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        GenericGallery gal= getIntent().getParcelableExtra(getPackageName()+".GALLERY");
        if(gal==null&&!tryLoadFromURL()){finish();return;}
        if(gal!=null)this.gallery=gal;
        if(gallery.getType()!= GenericGallery.Type.LOCAL){
            Queries.HistoryTable.addGallery(((Gallery)gallery).toSimpleGallery());
        }
        LogUtility.d(""+gallery);
        if(Global.useRtl())recycler.setRotationY(180);
        isLocal=getIntent().getBooleanExtra(getPackageName()+".ISLOCAL",false);
        zoom = getIntent().getIntExtra(getPackageName()+".ZOOM",0);
        refresher.setEnabled(false);
        recycler.setLayoutManager(new CustomGridLayoutManager(this,Global.getColumnCount()));

        loadGallery(gallery,zoom);//if already has gallery
    }

    private boolean tryLoadFromURL() {
        Uri data = getIntent().getData();
        if(data != null && data.getPathSegments().size() >= 2){//if using an URL
            List<String> params = data.getPathSegments();
            LogUtility.d(params.size()+": "+params);
            if(params.size()>2){
                try{
                    zoom=Integer.parseInt(params.get(2));
                }catch (NumberFormatException e){
                    LogUtility.e(e.getLocalizedMessage(),e);
                }
            }
            InspectorV3.galleryInspector(this,Integer.parseInt(params.get(1)),new InspectorV3.DefaultInspectorResponse(){

                @Override
                public void onSuccess(List<GenericGallery> galleries) {
                    if(galleries.size()>0) {
                        Intent intent = new Intent(GalleryActivity.this, GalleryActivity.class);
                        intent.putExtra(getPackageName() + ".GALLERY", galleries.get(0));
                        intent.putExtra(getPackageName() + ".ZOOM", zoom);
                        startActivity(intent);
                    }
                    finish();
                }
            }).start();
            return true;
        }
        return false;
    }

    private void lookup(){
        CustomGridLayoutManager manager= (CustomGridLayoutManager)recycler.getLayoutManager();
        GalleryAdapter adapter=(GalleryAdapter)recycler.getAdapter();
        manager.setSpanSizeLookup(new CustomGridLayoutManager.SpanSizeLookup(){
            @Override
            public int getSpanSize(int position){
                return adapter.positionToType(position)==GalleryAdapter.Type.PAGE?1:manager.getSpanCount();
            }
        });
    }
    private void loadGallery(GenericGallery gall,int zoom) {
        this.gallery=gall;
        if(getSupportActionBar()!=null){
            applyTitle();
        }
        adapter=new GalleryAdapter(this,gallery,Global.getColumnCount());
        recycler.setAdapter(adapter);
        lookup();
        if(zoom>0 && Global.getDownloadPolicy()!= Global.DataUsageType.NONE){
            Intent intent = new Intent(this, ZoomActivity.class);
            intent.putExtra(getPackageName()+".GALLERY",this.gallery);
            intent.putExtra(getPackageName()+".PAGE",zoom);
            startActivity(intent);
        }
    }
    private void applyTitle() {
        CollapsingToolbarLayout collapsing=findViewById(R.id.collapsing);
        ActionBar actionBar=getSupportActionBar();
        String title=gallery.getTitle();
        if(title==null)title="";
        if(collapsing==null||actionBar==null)return;
        String finalTitle = title;
        View.OnLongClickListener listener=v -> {
            CopyToClipboardActivity.copyTextToClipboard(GalleryActivity.this, finalTitle);
            GalleryActivity.this.runOnUiThread(
                    ()->Toast.makeText(GalleryActivity.this, R.string.title_copied_to_clipboard,Toast.LENGTH_SHORT).show()
            );
            return true;
        };

        collapsing.setOnLongClickListener(listener);
        findViewById(R.id.toolbar).setOnLongClickListener(listener);
        if(title.length()>100){
            collapsing.setExpandedTitleTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
            collapsing.setMaxLines(5);
        } else {
            collapsing.setExpandedTitleTextAppearance(android.R.style.TextAppearance_DeviceDefault_Large);
            collapsing.setMaxLines(4);
        }
        actionBar.setTitle(title);

    }

    @Override
    protected int getPortCount() {
        return 0;
    }

    @Override
    protected int getLandCount() {
        return 0;
    }


    public void initFavoriteIcon(Menu menu){
        boolean onlineFavorite=!isLocal&&((Gallery)gallery).isOnlineFavorite();
        boolean unknown=getIntent().getBooleanExtra(getPackageName()+ ".UNKNOWN",false);
        MenuItem item=menu.findItem(R.id.add_online_gallery);

        item.setIcon(onlineFavorite?R.drawable.ic_star:R.drawable.ic_star_border);

        if(unknown)item.setTitle(R.string.toggle_online_favorite);
        else if(onlineFavorite)item.setTitle(R.string.remove_from_online_favorites);
        else item.setTitle(R.string.add_to_online_favorite);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery, menu);
        isLocalFavorite =Favorites.isFavorite(gallery);

        menu.findItem(R.id.favorite_manager).setIcon(isLocalFavorite?R.drawable.ic_favorite:R.drawable.ic_favorite_border);
        menuItemsVisible(menu);
        initFavoriteIcon(menu);
        Utility.tintMenu(menu);
        updateColumnCount(false);
        return true;
    }

    private void menuItemsVisible(Menu menu) {
        boolean isValidOnline=gallery.isValid()&&!isLocal;
        menu.findItem(R.id.add_online_gallery).setVisible(isValidOnline&&Login.isLogged());
        menu.findItem(R.id.favorite_manager).setVisible(isValidOnline);
        menu.findItem(R.id.download_gallery).setVisible(isValidOnline);
        menu.findItem(R.id.related).setVisible(isValidOnline);
        menu.findItem(R.id.comments).setVisible(isValidOnline);

        menu.findItem(R.id.share).setVisible(gallery.isValid());
        menu.findItem(R.id.load_internet).setVisible(isLocal&&gallery.isValid());
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
            case R.id.download_gallery:
                if(Global.hasStoragePermission(this))
                    new RangeSelector(this, (Gallery) gallery).show();
                else
                    requestStorage();
                break;
            case R.id.add_online_gallery:
                addToFavorite(item);

                break;
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
                if(isLocalFavorite){
                    if(Favorites.removeFavorite(gallery)) isLocalFavorite =!isLocalFavorite;
                }else if(Favorites.addFavorite((Gallery) gallery)){
                    isLocalFavorite =!isLocalFavorite;
                }else{
                    Snackbar.make(recycler,getString(R.string.favorite_max_reached,Favorites.MAXFAVORITE),Snackbar.LENGTH_LONG).show();
                }
                item.setIcon(isLocalFavorite ?R.drawable.ic_favorite:R.drawable.ic_favorite_border);
                Global.setTint(item.getIcon());
            break;
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addToFavorite(final MenuItem item) {
        String startUrl=Utility.getBaseUrl()+"g/"+gallery.getId()+"/";
        String url=startUrl+"favorite";
        LogUtility.d("Calling: "+url);
        new CSRFGet(new CSRFGet.Response() {
            @Override
            public void onResponse(String token)throws IOException {
                LogUtility.d("FIND TOKEN: "+token);
                RequestBody formBody = new FormBody.Builder()
                        .add("WTF","OK")
                        .build();
                assert Global.getClient() != null;
                Response response=Global.getClient().newCall(
                        new Request.Builder()
                                .addHeader("Referer",startUrl)
                                .addHeader("X-CSRFToken",token)
                                .addHeader("X-Requested-With","XMLHttpRequest")
                                .url(url)
                                .post(formBody)
                                .build()
                ).execute();

                String resp=response.body().string();
                LogUtility.d("Called: "+response.request().method()+response.request().url().toString()+response.code()+resp);
                final boolean removedFromFavorite=resp.contains("false");
                GalleryActivity.this.runOnUiThread(() -> {
                    item.setIcon(removedFromFavorite?R.drawable.ic_star_border:R.drawable.ic_star);
                    item.setTitle(removedFromFavorite?R.string.add_to_online_favorite:R.string.remove_from_online_favorites);
                });
                response.close();
            }
        },startUrl,"csrfmiddlewaretoken").start();

    }

    private void updateColumnCount(boolean increase) {
        int x=Global.getColumnCount();
        MenuItem item= ((Toolbar)findViewById(R.id.toolbar)).getMenu().findItem(R.id.change_view);
        if(increase||((CustomGridLayoutManager)recycler.getLayoutManager()).getSpanCount()!=x){
            if(increase)x=x%4+1;
            int pos=((CustomGridLayoutManager)recycler.getLayoutManager()).findFirstVisibleItemPosition();
            Global.updateColumnCount(this,x);

            recycler.setLayoutManager(new CustomGridLayoutManager(this,x));
            LogUtility.d("Span count: "+((CustomGridLayoutManager)recycler.getLayoutManager()).getSpanCount());
            if(adapter!=null){
                adapter.setColCount(Global.getColumnCount());
                recycler.setAdapter(adapter);
                lookup();
                recycler.scrollToPosition(pos);
                adapter.setMaxImageSize(null);

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
            public void onSuccess(List<GenericGallery> galleries) {
                if(galleries.size()==0)return;
                Intent intent=new Intent(GalleryActivity.this, GalleryActivity.class);
                LogUtility.d(galleries.get(0).toString());
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
                new RangeSelector(this, (Gallery) gallery).show();
    }
}
