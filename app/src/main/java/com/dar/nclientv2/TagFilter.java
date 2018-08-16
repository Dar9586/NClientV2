package com.dar.nclientv2;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.dar.nclientv2.adapters.TagsAdapter;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;

import java.util.ArrayList;
import java.util.List;

public class TagFilter extends AppCompatActivity{

    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    public ViewPager getViewPager() {
        return mViewPager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        Global.initHttpClient(this);
        Global.initTagOrder(this);
        Global.initMinTagCount(this);
        Global.initTagSets(this);
        Global.initTagPreferencesSets(this);
        setContentView(R.layout.activity_tag_filter);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(1);

        TabLayout tabLayout = findViewById(R.id.tabs);
        if(Global.isLogged())tabLayout.addTab(tabLayout.newTab().setText(R.string.online_tags));

        Log.d(Global.LOGTAG,"ISNULL?"+(tabLayout==null));
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + position);
                if (page != null) {
                    ((PlaceholderFragment)page).updateDataset();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        mViewPager.setCurrentItem(getPage());
    }
    private int getPage(){
        Uri data = getIntent().getData();
        if(data != null){
            List<String> params = data.getPathSegments();
            for(String x:params)Log.i(Global.LOGTAG,x);
            if(params.size()>0){
                switch (params.get(0)){
                    case "tags":return 1;
                    case "artists":return 2;
                    case "characters":return 3;
                    case "parodies":return 4;
                    case "groups":return 5;
                }
            }
        }
        return 0;
    }
    private android.support.v7.widget.SearchView searchView;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tag_filter, menu);
        menu.findItem(R.id.order_type).setIcon(Global.isTagOrderByPopular()?R.drawable.ic_sort:R.drawable.ic_sort_by_alpha);
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
                    ((PlaceholderFragment)page).filterDataset(newText);
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
                        ((PlaceholderFragment)page).updateDataset();
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
            case R.id.order_type:
                Global.updateTagOrder(this,!Global.isTagOrderByPopular());
            if(page!=null)((PlaceholderFragment)page).sortDataset();
                item.setIcon(Global.isTagOrderByPopular()?R.drawable.ic_sort:R.drawable.ic_sort_by_alpha);
                Global.setTint(item.getIcon());
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadDialog(){
        DefaultDialogs.pageChangerDialog(
                new DefaultDialogs.Builder(this).setActual(Global.getMinTagCount()).setMax(100).setDialogs(new DefaultDialogs.DialogResults() {
                    @Override
                    public void positive(int actual) {
                        Global.updateMinTagCount(TagFilter.this,actual);
                    }
                    @Override
                    public void negative() {}
                }).setTitle(R.string.minimum_tag_count).setDrawable(R.drawable.ic_hashtag)
        );
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
             if(Global.getTheme()== Global.ThemeScheme.BLACK){
                 recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), RecyclerView.VERTICAL));
                 recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), RecyclerView.HORIZONTAL));
             }
             tag=getArguments().getInt("TAGTYPE");
             page=getArguments().getInt("PAGE");
            applyAdapter();

            return rootView;
        }

        private void loadTags() {
            if(tag==-1&&page!=6)applyAdapter();
            else ((TagsAdapter)recyclerView.getAdapter()).resetDataset(TagType.values()[tag==-1?0:tag]);
        }

        private void applyAdapter(){
            String query=((TagFilter)getActivity()).searchView==null?"":((TagFilter)getActivity()).searchView.getQuery().toString();
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?4:2));
            if(tag!=-1){
                List<Tag>t=Global.getTagSet(TagType.values()[tag]);
                TagsAdapter adapter=new TagsAdapter((TagFilter)getActivity(),t==null?new ArrayList<Tag>(1):t,query);
                recyclerView.setAdapter(adapter);
                if(adapter.getTrueDataset().size()==0)loadTags();
            }
            else {
                if(page==0)recyclerView.setAdapter(new TagsAdapter((TagFilter)getActivity(),Global.getListPrefer(),query));
                else recyclerView.setAdapter(new TagsAdapter((TagFilter)getActivity(),query));
            }
        }
        private void filterDataset(String newText){
            if(((TagFilter)getActivity()).searchView!=null&&recyclerView.getAdapter()!=null)
                ((TagsAdapter)recyclerView.getAdapter()).getFilter().filter(newText);

        }
        private void sortDataset(){
            if(recyclerView.getAdapter()!=null) ((TagsAdapter)recyclerView.getAdapter()).sortDataset(false);
        }

        void updateDataset() {
            if(tag==-1)applyAdapter();
            else filterDataset(((TagFilter)getActivity()).searchView.getQuery().toString());
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
            Log.d(Global.LOGTAG,"creating at: "+position);
            return PlaceholderFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return Global.isLogged()?7:6;
        }
    }
}
