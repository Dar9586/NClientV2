package com.dar.nclientv2;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.dar.nclientv2.adapters.TagsAdapter;
import com.dar.nclientv2.components.widgets.CustomGridLayoutManager;
import com.dar.nclientv2.components.widgets.TagTypePage;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;
import com.dar.nclientv2.utility.LogUtility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class TagFilterActivity extends AppCompatActivity{
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
    private SearchView searchView;
    private ViewPager mViewPager;

    public ViewPager getViewPager() {
        return mViewPager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.activity_tag_filter);

        //init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        TagTypePageAdapter mTagTypePageAdapter = new TagTypePageAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mTagTypePageAdapter);
        mViewPager.setOffscreenPageLimit(1);

        TabLayout tabLayout = findViewById(R.id.tabs);

        LogUtility.d("ISNULL?"+(tabLayout==null));
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                TagTypePage page = (TagTypePage)getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + position);
                if (page != null) {
                    ((TagsAdapter)page.getRecyclerView().getAdapter()).addItem();
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
            for(String x:params) LogUtility.i(x);
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
        searchView=(androidx.appcompat.widget.SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
                if (page != null) {
                    ((TagTypePage)page).refilter(newText);
                }
                return true;
            }
        });
        return true;
    }
    private void createDialog(){
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.are_you_sure).setMessage(getString(R.string.clear_this_list)).setIcon(R.drawable.ic_help);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
            if (page != null) {
                ((TagTypePage)page).reset();

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
        TagTypePage page = (TagTypePage)getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
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
        builder.setYesbtn(R.string.ok).setNobtn(R.string.cancel);
        builder.setTitle(R.string.set_minimum_count).setDialogs(new DefaultDialogs.DialogResults(){
            @Override
            public void positive(int actual){
                LogUtility.d("ACTUAL: "+actual);
                TagV2.updateMinCount(TagFilterActivity.this,actual);
                TagTypePage page =(TagTypePage) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
                if(page!=null){
                    page.changeSize();
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
            RecyclerView recycler=((TagTypePage)page).getRecyclerView();
            if(recycler!=null) {
                RecyclerView.Adapter adapter = recycler.getAdapter();
                CustomGridLayoutManager gridLayoutManager = new CustomGridLayoutManager(this, count);
                recycler.setLayoutManager(gridLayoutManager);
                recycler.setAdapter(adapter);
            }
        }
    }








    static class TagTypePageAdapter extends FragmentPagerAdapter {

        TagTypePageAdapter(FragmentManager fm) {
            super(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            LogUtility.d("creating at: "+position);
            return TagTypePage.newInstance(position);
        }

        @Override
        public int getCount() {
            return 6;
        }
    }
}
