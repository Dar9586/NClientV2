package com.dar.nclientv2;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.dar.nclientv2.components.activities.GeneralActivity;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.ui.main.PlaceholderFragment;
import com.dar.nclientv2.ui.main.SectionsPagerAdapter;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.tabs.TabLayout;

public class StatusViewerActivity extends GeneralActivity {
    private boolean sortByTitle = false;
    private String query;
    private ViewPager viewPager;
    private Toolbar toolbar;
    private SectionsPagerAdapter sectionsPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status_viewer);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.manage_statuses);
        sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                getPositionFragment(position).reload(query, sortByTitle);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.sort_by_name) {
            sortByTitle = !sortByTitle;
            getActualFragment().changeSort(sortByTitle);
            item.setTitle(sortByTitle ? R.string.sort_by_latest : R.string.sort_by_title);
            item.setIcon(sortByTitle ? R.drawable.ic_sort_by_alpha : R.drawable.ic_access_time);
            Global.setTint(item.getIcon());
        }
        return super.onOptionsItemSelected(item);
    }

    private PlaceholderFragment getActualFragment() {
        return getPositionFragment(viewPager.getCurrentItem());
    }

    private PlaceholderFragment getPositionFragment(int position) {
        return (PlaceholderFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.view_pager + ":" + position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.status_viewer, menu);
        final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                query = newText;
                getActualFragment().changeQuery(query);
                return true;
            }
        });
        Utility.tintMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }
}
