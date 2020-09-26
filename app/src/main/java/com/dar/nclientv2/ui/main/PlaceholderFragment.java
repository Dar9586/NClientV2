package com.dar.nclientv2.ui.main;

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

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {


    public static PlaceholderFragment newInstance(String statusName) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putString("STATUS_NAME", statusName);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_status_viewer, container, false);
        RecyclerView recyclerView=root.findViewById(R.id.recycler);
        SwipeRefreshLayout refreshLayout=root.findViewById(R.id.refresher);
        StatusViewerAdapter adapter=new StatusViewerAdapter(getActivity(),getArguments().getString("STATUS_NAME"));
        refreshLayout.setOnRefreshListener(() -> {
            adapter.reloadGalleries();
            refreshLayout.setRefreshing(false);
        });
        // TODO: 26/09/20 Add row count  (not always 2)
        recyclerView.setLayoutManager(new CustomGridLayoutManager(getContext(),2));
        assert getArguments() != null;
        recyclerView.setAdapter(adapter);
        return root;
    }
}
