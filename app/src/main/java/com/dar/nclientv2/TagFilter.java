package com.dar.nclientv2;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dar.nclientv2.adapters.TagsAdapter;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.scraper.Scraper;
import com.dar.nclientv2.components.CustomResultReceiver;
import com.dar.nclientv2.settings.Global;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TagFilter extends AppCompatActivity implements CustomResultReceiver.Receiver{

    private CustomResultReceiver mReceiver;
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        setContentView(R.layout.activity_tag_filter);

        Toolbar toolbar = findViewById(R.id.toolbar);
        PlaceholderFragment.orderByPopular=Global.isTagOrderByPopular();
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mReceiver=new CustomResultReceiver(new Handler());
        mReceiver.setReceiver(this);
        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(0);
        TabLayout tabLayout = findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + position);
                if (page != null) {
                    ((PlaceholderFragment)page).applyAdapter();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

    }

    private android.support.v7.widget.SearchView searchView;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tag_filter, menu);
        Global.setTint(menu.findItem(R.id.order_type).getIcon());
        searchView=(android.support.v7.widget.SearchView)menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
                if (page != null) {
                    ((TagsAdapter)((PlaceholderFragment)page).recyclerView.getAdapter()).filter(newText);
                }
                return true;
            }
        });
        return true;
    }
    private void createDialog(final boolean reset){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle(R.string.are_you_sure).setMessage(reset?R.string.reload_tags:R.string.reset_tags_long).setIcon(R.drawable.ic_help);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
                if (page != null) {
                    if(reset)((PlaceholderFragment)page).loadTags();
                    else{
                        Global.resetAllStatus(TagFilter.this);
                        ((PlaceholderFragment)page).applyAdapter();
                    }
                }
            }
        }).setNegativeButton(R.string.no, null).setCancelable(true);
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());

        switch (id){
            case R.id.change_min:loadDialog();break;
            case R.id.refresh:if(page != null)createDialog(true);break;
            case R.id.reset_tags:createDialog(false);break;
            case R.id.order_type:PlaceholderFragment.orderByPopular=Global.updateTagOrder(this,!PlaceholderFragment.orderByPopular);
            if(page!=null)((PlaceholderFragment)page).sortDataset();
                item.setIcon(PlaceholderFragment.orderByPopular?R.drawable.ic_sort:R.drawable.ic_sort_by_alpha_black_24dp);
                Global.setTint(item.getIcon());
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if(resultData.getBoolean(getPackageName()+".FINISH",true)){
            Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + resultData.getInt(getPackageName()+".PAGE",1));
            // based on the current position you can then cast the page to the correct
            // class and call the method:
            if (page != null) {
                ((PlaceholderFragment)page).applyAdapter();
            }
        }

    }
    private void loadDialog(){
        final int maxValue=50;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.minium_tag_count)).setIcon(R.drawable.ic_hashtag);
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
        edt.setMax(maxValue);
        edt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pag.setText(getString(R.string.page_format,edt.getProgress(),maxValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        edt.setProgress(Global.getMinTagCount()-1);
        pag.setText(String.format(Locale.US,"%d / %d",edt.getProgress()+1,maxValue));
        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Global.updateMinTagCount(TagFilter.this,edt.getProgress());
                dialog.cancel();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.setCancelable(true);
        builder.show();
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
        final int count=landscape?4:2;
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
        if (page != null) {
            RecyclerView recycler=((PlaceholderFragment)page).recyclerView;
            if(recycler!=null) {
                RecyclerView.Adapter adapter = recycler.getAdapter();
                GridLayoutManager gridLayoutManager = new GridLayoutManager(this, count);
                recycler.setLayoutManager(gridLayoutManager);
                recycler.setAdapter(adapter);
            }
        }
    }
    public static class PlaceholderFragment extends Fragment {
        static boolean orderByPopular;
        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        private static int getTag(int page){
            switch (page){
                case 0:return -1;
                case 1:return TagType.TAG.ordinal();
                case 2:return TagType.ARTIST.ordinal();
                case 3:return TagType.CHARACTER.ordinal();
                case 4:return TagType.PARODY.ordinal();
                case 5:return TagType.GROUP.ordinal();
            }
            return -1;
        }
        static PlaceholderFragment newInstance(int page) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt("TAGTYPE", getTag(page));
            args.putInt("PAGE", page);
            fragment.setArguments(args);
            return fragment;
        }
        int page;
        RecyclerView recyclerView;
        int tag;
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_tag_filter, container, false);
             recyclerView=rootView.findViewById(R.id.recycler);
             tag=getArguments().getInt("TAGTYPE");
             page=getArguments().getInt("PAGE");
             List<Tag> x=null;
             if(tag!=-1)x=Global.getSet(getContext(),TagType.values()[tag]);
            if(tag==-1||x.size()>1) applyAdapter();
            else loadTags();
            return rootView;
        }

        private void loadTags() {
            if(tag==-1){applyAdapter();return;}
            Snackbar.make(((TagFilter)getActivity()).mViewPager, R.string.long_time_toast, Snackbar.LENGTH_LONG).show();
            Intent intent=new Intent(getContext().getApplicationContext(), Scraper.class);
            intent.putExtra(getContext().getPackageName()+".TAGTYPE",tag);
            intent.putExtra(getContext().getPackageName()+".PAGE",page);
            intent.putExtra(getContext().getPackageName()+".RECEIVER", ((TagFilter)getActivity()).mReceiver);
            getContext().startService(intent);
        }

        private void applyAdapter(){
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?4:2));
            if(tag!=-1) recyclerView.setAdapter(new TagsAdapter(getContext(),Global.getSet(getContext(),TagType.values()[tag])));
            else recyclerView.setAdapter(new TagsAdapter(getContext(),Global.getListPrefer()));
            if(((TagFilter)getActivity()).searchView!=null)
            ((TagsAdapter)recyclerView.getAdapter()).filter(((TagFilter)getActivity()).searchView.getQuery().toString());
            sortDataset();

        }
        private void sortDataset(){
            if(orderByPopular){
                Collections.sort(((TagsAdapter) recyclerView.getAdapter()).getDataset(), new Comparator<Tag>() {
                    @Override
                    public int compare(Tag o1, Tag o2) {
                        return o2.getCount()-o1.getCount();
                    }
                });
            }else{
                Collections.sort(((TagsAdapter) recyclerView.getAdapter()).getDataset(), new Comparator<Tag>() {
                    @Override
                    public int compare(Tag o1, Tag o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
            }
            recyclerView.getAdapter().notifyDataSetChanged();
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
            return 6;
        }
    }
}
