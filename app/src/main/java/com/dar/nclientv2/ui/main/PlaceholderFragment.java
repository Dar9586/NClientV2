package com.dar.nclientv2.ui.main;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dar.nclientv2.R;
import com.dar.nclientv2.adapters.StatusViewerAdapter;
import com.dar.nclientv2.components.widgets.CustomGridLayoutManager;
import com.dar.nclientv2.settings.Global;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private StatusViewerAdapter adapter = null;
    private RecyclerView recycler;
    private SwipeRefreshLayout refresher;

    public static PlaceholderFragment newInstance(String statusName) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putString("STATUS_NAME", statusName);
        fragment.setArguments(bundle);
        return fragment;
    }

    private void updateColumnCount(boolean landscape) {
        recycler.setLayoutManager(new CustomGridLayoutManager(getContext(), getColumnCount(landscape)));
        recycler.setAdapter(adapter);
    }

    private int getColumnCount(boolean landscape) {
        return landscape ? Global.getColLandStatus() : Global.getColPortStatus();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            updateColumnCount(true);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            updateColumnCount(false);
        }
    }

    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_status_viewer, container, false);
        recycler = root.findViewById(R.id.recycler);
        refresher = root.findViewById(R.id.refresher);
        adapter = new StatusViewerAdapter(getActivity(), getArguments().getString("STATUS_NAME"));
        refresher.setOnRefreshListener(() -> {
            adapter.reloadGalleries();
            refresher.setRefreshing(false);
        });
        updateColumnCount(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        return root;
    }

    public void changeQuery(String newQuery) {
        if (adapter != null) adapter.setQuery(newQuery);

    }

    public void changeSort(boolean byTitle) {
        if (adapter != null) adapter.updateSort(byTitle);
    }

    public void reload(String query, boolean sortByTitle) {
        if (adapter != null)
            adapter.update(query, sortByTitle);
    }
}
