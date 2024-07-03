package com.dar.nclientv3.ui.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.dar.nclientv3.StatusViewerActivity;
import com.dar.nclientv3.async.database.Queries;
import com.dar.nclientv3.components.status.StatusManager;

import java.util.List;
import java.util.Locale;

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
        String status = statuses.get(position);
        int count = Queries.StatusMangaTable.getCountPerStatus(status);
        return String.format(Locale.US, "%s - %d", status, count);
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
