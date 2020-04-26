package com.dar.nclientv2;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.components.views.ZoomFragment;
import com.dar.nclientv2.components.widgets.CustomViewPager;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.targets.BitmapTarget;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ZoomActivity extends AppCompatActivity {
    @TargetApi(16)
    private final static int hideFlags= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | (Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT?View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY:0);
    @TargetApi(16)
    private final static int showFlags=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

    private GenericGallery gallery;
    private int actualPage=0;
    private boolean isHidden=false;
    private CustomViewPager mViewPager;
    private TextView pageManagerLabel, cornerPageViewer;
    private View pageSwitcher;
    private SeekBar seekBar;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        side=getSharedPreferences("Settings",0).getBoolean("volumeSide",true);
        setContentView(R.layout.activity_zoom);

        //read arguments
        gallery=getIntent().getParcelableExtra(getPackageName()+".GALLERY");
        final int page=getIntent().getExtras().getInt(getPackageName()+".PAGE",1)-1;

        //toolbar setup
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        setTitle(gallery.getTitle());

        //getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        if(Global.isLockScreen())getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        //find views
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        pageSwitcher =findViewById(R.id.page_switcher);
        pageManagerLabel =findViewById(R.id.pages);
        cornerPageViewer =findViewById(R.id.page_text);
        seekBar=findViewById(R.id.seekBar);

        //initial setup for views
        changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
        mViewPager.setKeepScreenOn(Global.isLockScreen());
        findViewById(R.id.prev).setOnClickListener(v -> changeClosePage(false));
        findViewById(R.id.next).setOnClickListener(v -> changeClosePage(true));
        seekBar.setMax(gallery.getPageCount()-1);
        if(Global.useRtl())seekBar.setRotationY(180);

        //Adding listeners
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int newPage) {
                int oldPage=actualPage;
                actualPage=newPage;
                setPageText(Global.useRtl()?gallery.getPageCount()-newPage:newPage+1);
                seekBar.setProgress(Global.useRtl()?gallery.getPageCount()-1-newPage:newPage);
                clearFarRequests(oldPage,newPage);
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
        pageManagerLabel.setOnClickListener(v -> {
            DefaultDialogs.pageChangerDialog(
                    new DefaultDialogs.Builder(this)
                            .setActual(offsetPage(actualPage)+1)
                            .setMin(1)
                            .setMax(gallery.getPageCount())
                            .setTitle(R.string.change_page)
                            .setDrawable(R.drawable.ic_find_in_page)
                            .setDialogs(new DefaultDialogs.DialogResults() {
                                @Override
                                public void positive(int actual) {
                                    changePage(offsetPage(actual-1));
                                }
                                @Override
                                public void negative() {}
                            })
            );
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    setPageText(progress+1);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                changePage(offsetPage(seekBar.getProgress()));
            }
        });


        changePage(offsetPage(page));
        setPageText(page+1);
    }
    private void setPageText(int page){
        pageManagerLabel.setText(getString(R.string.page_format,page,gallery.getPageCount()));
        cornerPageViewer.setText(getString(R.string.page_format,page,gallery.getPageCount()));
    }

    int offsetPage(int page){
        return Global.useRtl()?gallery.getPageCount()-1-page:page;
    }

    private boolean up=false,down=false,side;
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event){
        if(Global.volumeOverride()){
            switch(keyCode){
                case KeyEvent.KEYCODE_VOLUME_UP:up = false;break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:down = false;break;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(Global.volumeOverride()){
            switch(keyCode){
                case KeyEvent.KEYCODE_VOLUME_UP:
                    up = true;changeClosePage(side);if(up && down) changeSide();
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    down = true;changeClosePage(!side);if(up && down) changeSide();
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    private void changeSide(){
        getSharedPreferences("Settings",0).edit().putBoolean("volumeSide",side=!side).apply();
        Toast.makeText(this, side?R.string.next_page_volume_up:R.string.next_page_volume_down, Toast.LENGTH_SHORT).show();
    }
    private void changeClosePage(boolean next){
        if(next&&mViewPager.getCurrentItem()<(mViewPager.getAdapter().getCount()-1))changePage(mViewPager.getCurrentItem()+1);
        if(!next&&mViewPager.getCurrentItem()>0)changePage(mViewPager.getCurrentItem()-1);
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
        pageSwitcher.setPadding(0,0,0,landscape?0:Global.getNavigationBarHeight(this));
        ConstraintLayout.LayoutParams lp=(ConstraintLayout.LayoutParams) pageSwitcher.getLayoutParams();
        lp.setMargins(0,0,landscape?Global.getNavigationBarHeight(this):0,0);
        pageSwitcher.setLayoutParams(lp);
    }

    private void changePage(int newPage){
        mViewPager.setCurrentItem(newPage);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_zoom, menu);
        Utility.tintMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id){
            case R.id.save_page:
                if(Global.hasStoragePermission(this)){
                    downloadPage();
                }else requestStorage();
                break;
            case R.id.share:
                if(gallery.getId()<=0)sendImage(false);
                else openSendImageDialog();
                break;
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openSendImageDialog() {
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(this);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> sendImage(true))
                .setNegativeButton(R.string.no, (dialog, which) -> sendImage(false))
                .setCancelable(true).setTitle(R.string.send_with_title)
                .setMessage(R.string.caption_send_with_title)
                .show();
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
            downloadPage();

    }
    private ZoomFragment getActualFragment(){
        return getActualFragment(mViewPager.getCurrentItem());
    }
    private void clearFarRequests(int oldPage, int newPage){
        ZoomFragment fragment;
        for(int i=oldPage-1;i<=oldPage+1;i++){
            if(i== newPage ||i== newPage -1||i== newPage +1)continue;
            fragment=getActualFragment(i);
            if(fragment==null)continue;
            fragment.cancelRequest();
        }

    }
    private ZoomFragment getActualFragment(int position){
        return (ZoomFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + position);
    }

    private void sendImage(boolean withText){
        Bitmap bitmap;
        ZoomFragment page =getActualFragment();
        File file= null;
        String endName;
        int pageNum=mViewPager.getCurrentItem();
        if(gallery.isLocal()){
            LocalGallery gallery=(LocalGallery)this.gallery;
            file =gallery.getPage(pageNum);
            if(file==null)return;
            endName=file.getName();
        }else{
            Gallery gallery=(Gallery) this.gallery;
            endName=gallery.getFilename(pageNum);
        }
        try {
            file = File.createTempFile(""+gallery.getId(),endName);
            file.deleteOnExit();
            LogUtility.d("TEMP: "+file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(file==null)return;
        //is useless to download the vector used by the app
        if(page!=null&&page.getDrawable() instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) page.getDrawable()).getBitmap();
            saveImage(bitmap,file,true);//Here we use files because they are temp
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            if(withText)shareIntent.putExtra(Intent.EXTRA_TEXT, gallery.sharePageUrl(pageNum));
            Uri x= FileProvider.getUriForFile(
                    getApplicationContext(),getApplicationContext().getPackageName() + ".provider",file);
            shareIntent.putExtra(Intent.EXTRA_STREAM, x);
            switch (endName.substring(endName.length()-3)){
                case "jpg":shareIntent.setType("image/jpeg");break;
                case "png":shareIntent.setType("image/png");break;
                case "gif":shareIntent.setType("image/gif");break;
                default:shareIntent.setType("image/*");break;
            }
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            List<ResolveInfo> resInfoList = getApplicationContext().getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                getApplicationContext().grantUriPermission(packageName, x, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)));
        }
    }


    private void downloadPage() {
        final File output=new File(Global.SCREENFOLDER,gallery.getId()+"-"+(mViewPager.getCurrentItem()+1)+".jpg");
        Bitmap bitmap;
        ZoomFragment page =getActualFragment();
        //is useless to download the vector used by the app
        if(page!=null&&page.getDrawable() instanceof BitmapDrawable){
            bitmap=((BitmapDrawable)page.getDrawable()).getBitmap();
            saveImage(bitmap,output,false);
        }
    }

    private void saveImage(Bitmap bitmap, File output,boolean silent) {
        try {
            if(!output.exists())output.createNewFile();
            FileOutputStream ostream = new FileOutputStream(output);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, ostream);
            ostream.flush();
            ostream.close();
            if(!silent)Toast.makeText(this, R.string.download_completed, Toast.LENGTH_SHORT).show();
        }catch (IOException e){
            LogUtility.e(e.getLocalizedMessage(),e);}
    }


    public static void loadImage(AppCompatActivity activity, String url, ImageView target, boolean high){
        Glide.with(activity).asBitmap().load(url)
                .placeholder(R.drawable.ic_launcher_foreground).error(R.drawable.ic_close)
                .priority(high? Priority.HIGH: Priority.LOW)
                .into(new BitmapTarget(target));
    }
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        }
        @NonNull
        @Override
        public Fragment getItem(int position) {
            ZoomFragment f= ZoomFragment.newInstance(gallery,position);
            f.setClickListener(v -> {
                isHidden = !isHidden;
                LogUtility.d("Clicked "+isHidden);
                applyVisibilityFlag();
                animateLayout();
            });
            return f;
        }

        @Override
        public int getCount() {
            return gallery.getPageCount();
        }
    }

    private void animateLayout() {

        AnimatorListenerAdapter adapter=new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(isHidden){
                    pageSwitcher.setVisibility(View.GONE);
                    toolbar.setVisibility(View.GONE);
                    cornerPageViewer.setVisibility(View.VISIBLE);
                }
            }
        };

        pageSwitcher.setVisibility(View.VISIBLE);
        toolbar.setVisibility(View.VISIBLE);
        cornerPageViewer.setVisibility(View.GONE);

        pageSwitcher.animate().alpha(isHidden?0f:0.75f).setDuration(150).setListener(adapter).start();

        toolbar.animate().alpha(isHidden?0f:0.75f).setDuration(150).setListener(adapter).start();
    }

    private void applyVisibilityFlag() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
            getWindow().getDecorView().setSystemUiVisibility(isHidden ? hideFlags : showFlags);
        }else{
            getWindow().addFlags(isHidden ? WindowManager.LayoutParams.FLAG_FULLSCREEN : WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(isHidden ? WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN : WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }
}
