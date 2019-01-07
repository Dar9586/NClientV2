package com.dar.nclientv2;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.components.CustomViewPager;
import com.dar.nclientv2.settings.Global;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class ZoomActivity extends AppCompatActivity {
    private GenericGallery gallery;
    private final static int hideFlags=View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
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
        Global.initHideFromGallery(this);
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
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                pageManager.setText(getString(R.string.page_format,position+1,gallery.getPageCount()));
                seekBar.setProgress(position);
                if(!gallery.isLocal()){
                    Gallery gallery=(Gallery)ZoomActivity.this.gallery;
                    if(position<gallery.getPageCount()-1)Global.preloadImage(gallery.getPage(position+1));
                    if(position>0)Global.preloadImage(gallery.getPage(position-1));

                }
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
        changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
        findViewById(R.id.prev).setOnClickListener(v -> {
            if(mViewPager.getCurrentItem()>0)changePage(mViewPager.getCurrentItem()-1);
        });
        findViewById(R.id.next).setOnClickListener(v -> {
            if(mViewPager.getCurrentItem()<(mViewPager.getAdapter().getCount()-1))changePage(mViewPager.getCurrentItem()+1);
        });

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
        if(requestCode==1&&grantResults.length >0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
            downloadPage();

    }

    private void downloadPage() {
        Global.saveNoMedia(this);
        final File output=new File(Global.GALLERYFOLDER,gallery.getId()+"-"+(mViewPager.getCurrentItem()+1)+".jpg");
        Bitmap bitmap;
        PlaceholderFragment page =(PlaceholderFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
        //is useless to download the vector used by the app
        if(page!=null&&page.photoView.getDrawable() instanceof BitmapDrawable){
            bitmap=((BitmapDrawable)page.photoView.getDrawable()).getBitmap();
            try {
                if(!output.exists())output.createNewFile();
                FileOutputStream ostream = new FileOutputStream(output);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, ostream);
                ostream.flush();
                ostream.close();
                if(!Global.isHideFromGallery())Global.addToGallery(this,output);
                Toast.makeText(this, R.string.download_completed, Toast.LENGTH_SHORT).show();
            }catch (IOException e){
                Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);}
        }
    }

    public static class PlaceholderFragment extends Fragment {

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
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final ZoomActivity x=(ZoomActivity)getActivity();
            View rootView = inflater.inflate(R.layout.fragment_zoom, container, false);
            photoView =  rootView.findViewById(R.id.image);

            photoView.setOnMatrixChangeListener(rect -> photoView.setAllowParentInterceptOnEdge(photoView.getScale()<=1f));
            photoView.setOnClickListener(v -> {

                /*x.getWindow().getDecorView().setSystemUiVisibility(x.isHidden?showFlags:hideFlags);
                x.findViewById(R.id.page_switcher).setVisibility(x.isHidden?View.VISIBLE:View.GONE);
                x.findViewById(R.id.appbar).setVisibility(x.isHidden?View.VISIBLE:View.GONE);
                x.isHidden=!x.isHidden;*/
                final View y=x.findViewById(R.id.page_switcher);
                final View z=x.findViewById(R.id.appbar);
                x.isHidden=!x.isHidden;
                x.getWindow().getDecorView().setSystemUiVisibility(x.isHidden?hideFlags:showFlags);
                y.setVisibility(View.VISIBLE);
                z.setVisibility(View.VISIBLE);
                y.animate().alpha(x.isHidden?0f:0.75f).setDuration(150).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if(x.isHidden)y.setVisibility(View.GONE);
                    }
                }).start();

                z.animate().alpha(x.isHidden?0f:0.75f).setDuration(150).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if(x.isHidden)z.setVisibility(View.GONE);
                    }
                }).start();

            });
            int page=getArguments().getInt("PAGE",0);
            File file=x.directory==null?null:new File(x.directory,("000"+(page+1)+".jpg").substring(Integer.toString(page+1).length()));
            if(file==null||!file.exists()){
                if(x.gallery.isLocal())Global.loadImage(R.mipmap.ic_launcher,photoView);
                else Global.loadImage(((Gallery)x.gallery).getPage(page),photoView,true);

            }
            else Global.loadImage(file,photoView);
            return rootView;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
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
