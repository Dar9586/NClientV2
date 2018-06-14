package com.dar.nclientv2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import com.github.chrisbanes.photoview.OnMatrixChangedListener;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.OkHttpClient;

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
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private CustomViewPager mViewPager;
    private TextView pageManager;
    private File directory;
    private View pageSwitcher;
    private SeekBar seekBar;
    private final OkHttpClient client=new OkHttpClient();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        setContentView(R.layout.activity_zoom);
        Toolbar toolbar = findViewById(R.id.toolbar);
        //toolbar.setPadding(toolbar.getPaddingLeft(),Global.getStatusBarHeight(this),toolbar.getPaddingRight(),toolbar.getTitleMarginBottom());
        setSupportActionBar(toolbar);
        gallery=getIntent().getParcelableExtra(getPackageName()+".GALLERY");
        directory=Global.hasStoragePermission(this)?Global.findGalleryFolder(gallery.getId()):null;
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
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
                    if(position<gallery.getPageCount()-1)Global.preloadImage(gallery.getPage(position+1).getUrl());
                    if(position>0)Global.preloadImage(gallery.getPage(position-1).getUrl());

                }
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
        changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
        findViewById(R.id.prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mViewPager.getCurrentItem()>0)changePage(mViewPager.getCurrentItem()-1);
            }
        });
        findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mViewPager.getCurrentItem()<(mViewPager.getAdapter().getCount()-1))changePage(mViewPager.getCurrentItem()+1);
            }
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
        Global.GALLERYFOLDER.mkdirs();
        final File output=new File(Global.GALLERYFOLDER,gallery.getId()+"-"+(mViewPager.getCurrentItem()+1)+".jpg");
        Bitmap bitmap;
        PlaceholderFragment page =(PlaceholderFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
        if(page!=null){
            bitmap=((BitmapDrawable)page.photoView.getDrawable()).getBitmap();
            try {
                if(!output.exists())output.createNewFile();
                FileOutputStream ostream = new FileOutputStream(output);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
                ostream.flush();
                ostream.close();
                if(!Global.isHideFromGallery())Global.addToGallery(this,output);
                Toast.makeText(this, R.string.download_completed, Toast.LENGTH_SHORT).show();
            }catch (IOException e){
                Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);}
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        static PlaceholderFragment newInstance(int sectionNumber) {
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

            photoView.setOnMatrixChangeListener(new OnMatrixChangedListener() {
                @Override
                public void onMatrixChanged(RectF rect) {
                    photoView.setAllowParentInterceptOnEdge(photoView.getScale()<=1f);
                }
            });
            photoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    x.getWindow().getDecorView().setSystemUiVisibility(x.isHidden?showFlags:hideFlags);
                    x.findViewById(R.id.page_switcher).setVisibility(x.isHidden?View.VISIBLE:View.GONE);
                    x.findViewById(R.id.appbar).setVisibility(x.isHidden?View.VISIBLE:View.GONE);
                    x.isHidden=!x.isHidden;

                }
            });
            int page=getArguments().getInt("PAGE",0);
            File file=x.directory==null?null:new File(x.directory,("000"+(page+1)+".jpg").substring(Integer.toString(page+1).length()));
            if(file==null||!file.exists()){
                if(x.gallery.isLocal())Global.loadImage(R.mipmap.ic_launcher,photoView);
                else Global.loadImage(((Gallery)x.gallery).getPage(page).getUrl(),photoView,true);

            }
            else Global.loadImage(file,photoView);
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
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
