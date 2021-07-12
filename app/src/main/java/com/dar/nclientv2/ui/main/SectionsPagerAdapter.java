package com.dar.nclientv2.ui.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.dar.nclientv2.StatusViewerActivity;
import com.dar.nclientv2.components.status.StatusManager;

import java.util.List;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentStateAdapter {
    List<String> statuses;

    public SectionsPagerAdapter(StatusViewerActivity context) {
        super(context.getSupportFragmentManager(), context.getLifecycle());
        statuses = StatusManager.getNames();
    }


    @Nullable
    public CharSequence getPageTitle(int position) {
        return statuses.get(position);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return PlaceholderFragment.newInstance(statuses.get(position));
    }

    @Override
    public int getItemCount() {
        return statuses.size();
    }
}
