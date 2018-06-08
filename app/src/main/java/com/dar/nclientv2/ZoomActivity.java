package com.dar.nclientv2;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.components.CustomViewPager;
import com.dar.nclientv2.settings.Global;
import com.github.chrisbanes.photoview.OnMatrixChangedListener;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.util.Locale;

public class ZoomActivity extends AppCompatActivity {
    private GenericGallery gallery;
    private int paddingBottom;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        setContentView(R.layout.activity_zoom);
        gallery=getIntent().getParcelableExtra(getPackageName()+".GALLERY");
        directory=Global.findGalleryFolder(gallery.getId());
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
                    if(position<gallery.getPageCount()-1)Global.preloadImage(ZoomActivity.this,gallery.getPage(position+1).getUrl());
                    if(position>0)Global.preloadImage(ZoomActivity.this,gallery.getPage(position-1).getUrl());

                }
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
        paddingBottom=pageSwitcher.getPaddingBottom();

        changeLayout(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
        /*ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) findViewById(R.id.page_switcher).setPadding(findViewById(R.id.page_switcher).getPa).getLayoutParams();
        lp.setMargins(0,0,0,Global.getNavigationBarHeight(this));
        findViewById(R.id.page_switcher).setLayoutParams(lp);*/
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
    private void loadDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.change_page)).setIcon(R.drawable.ic_find_in_page);
        View v=View.inflate(this, R.layout.page_changer, null);
        builder.setView(v);
        final SeekBar edt=v.findViewById(R.id.seekBar);
        final TextView pag=v.findViewById(R.id.page);
        v.findViewById(R.id.prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edt.setProgress(edt.getProgress()-1);
            }
        });
        v.findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edt.setProgress(edt.getProgress()+1);
            }
        });
        edt.setMax(gallery.getPageCount()-1);
        edt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pag.setText(getString(R.string.page_format,edt.getProgress()+1,gallery.getPageCount()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        edt.setProgress(mViewPager.getCurrentItem());
        pag.setText(String.format(Locale.US,"%d / %d",edt.getProgress()+1,gallery.getPageCount()));
        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                changePage(edt.getProgress());
                dialog.cancel();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel),null);
        builder.setCancelable(true);
        builder.show();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final ZoomActivity x=(ZoomActivity)getActivity();
            View rootView = inflater.inflate(R.layout.fragment_zoom, container, false);
            final PhotoView photoView =  rootView.findViewById(R.id.image);

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
                    x.isHidden=!x.isHidden;

                }
            });
            int page=getArguments().getInt("PAGE",0);
            File file=x.directory==null?null:new File(x.directory,("000"+(page+1)+".jpg").substring(Integer.toString(page+1).length()));
            if(file==null||!file.exists()){
                if(x.gallery.isLocal())Global.loadImage(getContext(),R.mipmap.ic_launcher,photoView);
                else Global.loadImage(getContext(),((Gallery)x.gallery).getPage(page).getUrl(),photoView);

            }
            else Global.loadImage(getContext(),file,photoView);
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
