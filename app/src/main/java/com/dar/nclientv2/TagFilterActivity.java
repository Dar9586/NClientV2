package com.dar.nclientv2;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.dar.nclientv2.adapters.TagsAdapter;
import com.dar.nclientv2.components.activities.GeneralActivity;
import com.dar.nclientv2.components.widgets.CustomGridLayoutManager;
import com.dar.nclientv2.components.widgets.TagTypePage;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.settings.TagV2;
import com.dar.nclientv2.utility.LogUtility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class TagFilterActivity extends GeneralActivity {
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private SearchView searchView;
    private ViewPager2 mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Global.initActivity(this);
        setContentView(R.layout.activity_tag_filter);

        //init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        TagTypePageAdapter mTagTypePageAdapter = new TagTypePageAdapter(this);
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mTagTypePageAdapter);
        mViewPager.setOffscreenPageLimit(1);

        TabLayout tabLayout = findViewById(R.id.tabs);


        LogUtility.d("ISNULL?" + (tabLayout == null));
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                TagTypePage page = getFragment(position);
                if (page != null) {
                    ((TagsAdapter) page.getRecyclerView().getAdapter()).addItem();
                }
            }
        });


        new TabLayoutMediator(tabLayout, mViewPager, (tab, position) -> {
            int id = 0;
            switch (position) {
                case 0:
                    id = R.string.applied_filters;
                    break;
                case 1:
                    id = R.string.tags;
                    break;
                case 2:
                    id = R.string.artists;
                    break;
                case 3:
                    id = R.string.characters;
                    break;
                case 4:
                    id = R.string.parodies;
                    break;
                case 5:
                    id = R.string.groups;
                    break;
                case 6:
                    id = R.string.online_tags;
                    break;
            }
            tab.setText(id);
        }).attach();
        mViewPager.setCurrentItem(getPage());
    }

    @Nullable
    private TagTypePage getActualFragment() {
        return getFragment(mViewPager.getCurrentItem());
    }

    @Nullable
    private TagTypePage getFragment(int position) {
        return (TagTypePage) getSupportFragmentManager().findFragmentByTag("f" + position);
    }

    private int getPage() {
        Uri data = getIntent().getData();
        if (data != null) {
            List<String> params = data.getPathSegments();
            for (String x : params) LogUtility.i(x);
            if (params.size() > 0) {
                switch (params.get(0)) {
                    case "tags":
                        return 1;
                    case "artists":
                        return 2;
                    case "characters":
                        return 3;
                    case "parodies":
                        return 4;
                    case "groups":
                        return 5;
                }
            }
        }
        return 0;
    }

    private void updateSortItem(MenuItem item) {
        item.setIcon(TagV2.isSortedByName() ? R.drawable.ic_sort_by_alpha : R.drawable.ic_sort);
        item.setTitle(TagV2.isSortedByName() ? R.string.sort_by_title : R.string.sort_by_popular);
        Global.setTint(item.getIcon());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tag_filter, menu);
        updateSortItem(menu.findItem(R.id.sort_by_name));
        searchView = (androidx.appcompat.widget.SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                TagTypePage page = getActualFragment();
                if (page != null) {
                    page.refilter(newText);
                }
                return true;
            }
        });
        return true;
    }

    private void createDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.are_you_sure).setMessage(getString(R.string.clear_this_list)).setIcon(R.drawable.ic_help);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            TagTypePage page = getActualFragment();
            if (page != null) {
                page.reset();

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
        TagTypePage page = getActualFragment();
        if (id == R.id.reset_tags) createDialog();
        else if (id == R.id.set_min_count) minCountBuild();
        else if (id == R.id.sort_by_name) {
            TagV2.updateSortByName(this);
            updateSortItem(item);
            if (page != null)
                page.refilter(searchView.getQuery().toString());
        }

        return super.onOptionsItemSelected(item);
    }

    private void minCountBuild() {
        int min = TagV2.getMinCount();
        DefaultDialogs.Builder builder = new DefaultDialogs.Builder(this);
        builder.setActual(min).setMax(100).setMin(2);
        builder.setYesbtn(R.string.ok).setNobtn(R.string.cancel);
        builder.setTitle(R.string.set_minimum_count).setDialogs(new DefaultDialogs.CustomDialogResults() {
            @Override
            public void positive(int actual) {
                LogUtility.d("ACTUAL: " + actual);
                TagV2.updateMinCount(TagFilterActivity.this, actual);
                TagTypePage page = getActualFragment();
                if (page != null) {
                    page.changeSize();
                }
            }
        });
        DefaultDialogs.pageChangerDialog(builder);
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            changeLayout(true);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            changeLayout(false);
        }
    }

    private void changeLayout(boolean landscape) {
        final int count = landscape ? 4 : 2;
        TagTypePage page = getActualFragment();
        if (page != null) {
            RecyclerView recycler = page.getRecyclerView();
            if (recycler != null) {
                RecyclerView.Adapter adapter = recycler.getAdapter();
                CustomGridLayoutManager gridLayoutManager = new CustomGridLayoutManager(this, count);
                recycler.setLayoutManager(gridLayoutManager);
                recycler.setAdapter(adapter);
            }
        }
    }


    static class TagTypePageAdapter extends FragmentStateAdapter {

        TagTypePageAdapter(TagFilterActivity activity) {
            super(activity.getSupportFragmentManager(), activity.getLifecycle());
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return TagTypePage.newInstance(position);
        }

        @Override
        public int getItemCount() {
            return Login.isLogged() ? 7 : 6;
        }
    }
}
