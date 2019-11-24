package com.dar.nclientv2;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.components.CustomViewPager;
import com.dar.nclientv2.settings.Global;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ZoomActivity extends AppCompatActivity {
    private GenericGallery gallery;
    private boolean overrideVolume;
    public int actualPage=0;
    @TargetApi(16)
    private final static int hideFlags= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | (Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT?View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY:0);
    @TargetApi(16)
    private final static int showFlags=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
    private boolean isHidden=false;

    private CustomViewPager mViewPager;
    private TextView pageManager;
    private File directory;
    private View pageSwitcher;
    private SeekBar seekBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        side=getSharedPreferences("Settings",0).getBoolean("volumeSide",true);
        overrideVolume=getSharedPreferences("Settings",0).getBoolean(getString(R.string.key_override_volume),true);
        setContentView(R.layout.activity_zoom);
        Toolbar toolbar = findViewById(R.id.toolbar);
        //toolbar.setPadding(toolbar.getPaddingLeft(),Global.getStatusBarHeight(this),toolbar.getPaddingRight(),toolbar.getTitleMarginBottom());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        gallery=getIntent().getParcelableExtra(getPackageName()+".GALLERY");
        setTitle(gallery.getTitle());
        directory=Global.hasStoragePermission(this)?Global.findGalleryFolder(gallery.getId()):null;
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        pageSwitcher =findViewById(R.id.page_switcher);
        pageManager=findViewById(R.id.pages);
        seekBar=findViewById(R.id.seekBar);
        //mViewPager.setOffscreenPageLimit(1);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                actualPage=position;
                pageManager.setText(getString(R.string.page_format,position+1,gallery.getPageCount()));
                seekBar.setProgress(position);
                PlaceholderFragment.current=position;
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
        changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
        findViewById(R.id.prev).setOnClickListener(v -> changeClosePage(false));
        findViewById(R.id.next).setOnClickListener(v -> changeClosePage(true));

        final int page=getIntent().getExtras().getInt(getPackageName()+".PAGE",0);

        seekBar.setMax(gallery.getPageCount()-1);
        changePage(page);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser)pageManager.setText(getString(R.string.page_format,progress+1,gallery.getPageCount()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                changePage(seekBar.getProgress());
            }
        });
        pageManager.setText(getString(R.string.page_format,page+1,gallery.getPageCount()));

    }
    private boolean up=false,down=false,side;
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event){
        if(overrideVolume){
            switch(keyCode){
                case KeyEvent.KEYCODE_VOLUME_UP:up = false;break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:down = false;break;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(overrideVolume){
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
        PlaceholderFragment.current=newPage;
        mViewPager.setCurrentItem(newPage);
        seekBar.setProgress(newPage);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_zoom, menu);
        Global.setTint(menu.findItem(R.id.save_page).getIcon());
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
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
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
    private PlaceholderFragment getActualFragment(){
        return getActualFragment(mViewPager.getCurrentItem());
    }
    private PlaceholderFragment getActualFragment(int position){
        return (PlaceholderFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + position);
    }
    private void downloadPage() {
        final File output=new File(Global.SCREENFOLDER,gallery.getId()+"-"+(mViewPager.getCurrentItem()+1)+".jpg");
        Bitmap bitmap;
        PlaceholderFragment page =getActualFragment();
        //is useless to download the vector used by the app
        if(page!=null&&page.photoView.getDrawable() instanceof BitmapDrawable){
            bitmap=((BitmapDrawable)page.photoView.getDrawable()).getBitmap();
            try {
                if(!output.exists())output.createNewFile();
                FileOutputStream ostream = new FileOutputStream(output);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, ostream);
                ostream.flush();
                ostream.close();
                Toast.makeText(this, R.string.download_completed, Toast.LENGTH_SHORT).show();
            }catch (IOException e){
                Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);}
        }
    }

    public static class PlaceholderFragment extends Fragment {

        public static int current=0;

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt("PAGE", sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }
        PhotoView photoView;
        private int page;
        private ZoomActivity activity;
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            activity =(ZoomActivity)getActivity();
            View rootView = inflater.inflate(R.layout.fragment_zoom, container, false);
            photoView =  rootView.findViewById(R.id.image);

            photoView.setOnMatrixChangeListener(rect -> photoView.setAllowParentInterceptOnEdge(photoView.getScale()<=1f));
            photoView.setOnClickListener(v -> {

                /*activity.getWindow().getDecorView().setSystemUiVisibility(activity.isHidden?showFlags:hideFlags);
                activity.findViewById(R.id.page_switcher).setVisibility(activity.isHidden?View.VISIBLE:View.GONE);
                activity.findViewById(R.id.appbar).setVisibility(activity.isHidden?View.VISIBLE:View.GONE);
                activity.isHidden=!activity.isHidden;*/
                final View y = activity.findViewById(R.id.page_switcher);
                final View z = activity.findViewById(R.id.appbar);
                activity.isHidden = !activity.isHidden;
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
                    activity.getWindow().getDecorView().setSystemUiVisibility(activity.isHidden ? hideFlags : showFlags);
                }else{
                    activity.getWindow().addFlags(activity.isHidden ? WindowManager.LayoutParams.FLAG_FULLSCREEN : WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                    activity.getWindow().clearFlags(activity.isHidden ? WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN : WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
                y.setVisibility(View.VISIBLE);
                z.setVisibility(View.VISIBLE);
                y.animate().alpha(activity.isHidden?0f:0.75f).setDuration(150).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if(activity.isHidden)y.setVisibility(View.GONE);
                    }
                }).start();

                z.animate().alpha(activity.isHidden?0f:0.75f).setDuration(150).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if(activity.isHidden)z.setVisibility(View.GONE);
                    }
                }).start();

            });
            page=getArguments().getInt("PAGE",0);
            Log.d(Global.LOGTAG,"Loaded page: "+page);
            if(page==current)loadPage(true);
            else if(page==(current-1)||page==(current+1))loadPage(false);
            return rootView;
        }
        public void loadPage(boolean high){

            File file= LocalGallery.getPage(activity.directory,page+1);
            if(file==null||!file.exists()){
                if(activity.gallery.isLocal()) Glide.with(activity).load(R.mipmap.ic_launcher).into(photoView);
                else Glide.with(activity).load(((Gallery)activity.gallery).getPage(page)).priority(high? Priority.HIGH: Priority.LOW).into(photoView);
            }
            else Glide.with(activity).load(file).into(photoView);
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return gallery.getPageCount();
        }
    }
}
