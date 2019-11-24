package com.dar.nclientv2;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.dar.nclientv2.adapters.TagsAdapter;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.ScrapeTags;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.settings.TagV2;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class TagFilter extends AppCompatActivity{
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
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
        setContentView(R.layout.activity_tag_filter);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(1);

        TabLayout tabLayout = findViewById(R.id.tabs);
        if(Login.isLogged())tabLayout.addTab(tabLayout.newTab().setText(R.string.online_tags));

        Log.d(Global.LOGTAG,"ISNULL?"+(tabLayout==null));
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                PlaceholderFragment page = (PlaceholderFragment)getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + position);
                if (page != null) {
                    ((TagsAdapter)page.recyclerView.getAdapter()).addItem();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        mViewPager.setCurrentItem(getPage());
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
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
    private androidx.appcompat.widget.SearchView searchView;
    private void updateSortItem(MenuItem item){
        item.setIcon(TagV2.isSortedByName()?R.drawable.ic_sort_by_alpha:R.drawable.ic_sort);
        item.setTitle(TagV2.isSortedByName()?R.string.sort_by_title:R.string.sort_by_popular);
        Global.setTint(item.getIcon());
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tag_filter, menu);
        updateSortItem(menu.findItem(R.id.sort_by_name));
        searchView=(androidx.appcompat.widget.SearchView)menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
                if (page != null) {
                    ((PlaceholderFragment)page).refilter(newText);
                }
                return true;
            }
        });
        return true;
    }
    private void createDialog(){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle(R.string.are_you_sure).setMessage(getString(R.string.clear_this_list)).setIcon(R.drawable.ic_help);
        builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
            Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
            if (page != null) {
                ((PlaceholderFragment)page).reset();

            }
        }).setNegativeButton(android.R.string.no, null).setCancelable(true);
        builder.show();
    }
    public void addItems(TagType type){
        if(mViewPager==null)return;
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
        if(page!=null)((PlaceholderFragment)page).addItems(type);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        PlaceholderFragment page = (PlaceholderFragment)getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
        switch (id){
            case R.id.reset_tags:createDialog();break;
            case R.id.set_min_count:minCountBuild(); break;
            case R.id.sort_by_name:
                TagV2.updateSortByName(this);
                updateSortItem(item);
                page.refilter(searchView.getQuery().toString());

                break;
            /*case R.id.load_next:
                if(page.isNormalType())
                    new ScrapeTags(this,(TagsAdapter)page.recyclerView.getAdapter(),page.type).start();
                break;*/
        }

        return super.onOptionsItemSelected(item);
    }

    private void minCountBuild(){
        int min=TagV2.getMinCount();
        DefaultDialogs.Builder builder=new DefaultDialogs.Builder(this);
        builder.setActual(min).setMax(100).setMin(2);
        builder.setYesbtn(android.R.string.ok).setNobtn(android.R.string.cancel);
        builder.setTitle(R.string.set_minimum_count).setDialogs(new DefaultDialogs.DialogResults(){
            @Override
            public void positive(int actual){
                Log.d(Global.LOGTAG,"ACTUAL: "+actual);
                TagV2.updateMinCount(TagFilter.this,actual);
                PlaceholderFragment page =(PlaceholderFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
                if(page!=null){
                    page.addItems(page.type);
                }
            }

            @Override
            public void negative(){

            }
        });
        DefaultDialogs.pageChangerDialog(builder);
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
        TagType type;
        public RecyclerView recyclerView;
        TagFilter activity;

        public boolean isNormalType(){
            return type!=TagType.UNKNOWN&&type!=TagType.CATEGORY;
        }

        public PlaceholderFragment() { }
        //UNKNOW sono gli status
        //CATEGORY è la roba online
        private static int getTag(int page){
            switch (page){
                case 0:return TagType.UNKNOWN.ordinal();
                case 1:return TagType.TAG.ordinal();
                case 2:return TagType.ARTIST.ordinal();
                case 3:return TagType.CHARACTER.ordinal();
                case 4:return TagType.PARODY.ordinal();
                case 5:return TagType.GROUP.ordinal();
                case 6:return TagType.CATEGORY.ordinal();
            }
            return -1;
        }
        static PlaceholderFragment newInstance(int page) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt("TAGTYPE", getTag(page));
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            activity=(TagFilter)getActivity();
            type=TagType.values()[ getArguments().getInt("TAGTYPE")];
            View rootView = inflater.inflate(R.layout.fragment_tag_filter, container, false);
            recyclerView=rootView.findViewById(R.id.recycler);
            if(Global.getTheme()== Global.ThemeScheme.BLACK){
                recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), RecyclerView.VERTICAL));
                recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), RecyclerView.HORIZONTAL));
            }

             /*recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
                 @Override
                 public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
                     GridLayoutManager manager=(GridLayoutManager)recyclerView.getLayoutManager();
                     if(type!=TagType.UNKNOWN&&type!=TagType.CATEGORY
                             &&manager.findLastVisibleItemPosition() >= (recyclerView.getAdapter().getItemCount()-1-manager.getSpanCount())){
                         if(((TagsAdapter)recyclerView.getAdapter()).getLastQuery().equals(""))
                                new ScrapeTags(PlaceholderFragment.this.getContext(),(TagsAdapter)recyclerView.getAdapter(),type).start();
                     }
                 }
             });*/
            loadTags();
            return rootView;
        }
        public void loadTags(){
            String query=activity.searchView==null?"":activity.searchView.getQuery().toString();
            recyclerView.setLayoutManager(new GridLayoutManager(activity, getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?4:2));
            TagsAdapter adapter;
            switch(type){
                case UNKNOWN:adapter=new TagsAdapter(activity,query,null,false);break;
                case CATEGORY:adapter=new TagsAdapter(activity,query,null,true);break;
                default:adapter=new TagsAdapter(activity,query,type,false);break;
            }
            recyclerView.setAdapter(adapter);
        }
        public void refilter(String newText){
            if(activity!=null)activity.runOnUiThread(() -> ((TagsAdapter)recyclerView.getAdapter()).getFilter().filter(newText));
        }

        public void reset(){
            switch(type){
                case UNKNOWN:TagV2.resetAllStatus();break;
                case CATEGORY:break;
                default:
                    Intent i=new Intent(activity, ScrapeTags.class);
                    activity.startService(i);
                    break;
            }
        }

        public void addItems(TagType type){
            if(this.type==type){
                refilter(""+activity.searchView.getQuery());
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(Global.LOGTAG,"creating at: "+position);
            return PlaceholderFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return Login.isLogged()?7:6;
        }
    }
}
