package com.dar.nclientv2.components.widgets;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.TagFilterActivity;
import com.dar.nclientv2.adapters.TagsAdapter;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.ScrapeTags;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;

public class TagTypePage extends Fragment {
    private TagType type;
    private RecyclerView recyclerView;
    private TagFilterActivity activity;
    private String query;
    private TagsAdapter adapter;
    public TagTypePage() { }

    public void setQuery(String query) {
        this.query = query;
        refilter(query);
    }

    private static int getTag(int page){
        switch (page){
            case 0:return TagType.UNKNOWN.ordinal();//tags with status
            case 1:return TagType.TAG.ordinal();
            case 2:return TagType.ARTIST.ordinal();
            case 3:return TagType.CHARACTER.ordinal();
            case 4:return TagType.PARODY.ordinal();
            case 5:return TagType.GROUP.ordinal();
            case 6:return TagType.CATEGORY.ordinal();//online blacklisted tags
        }
        return -1;
    }
    public static TagTypePage newInstance(int page) {
        TagTypePage fragment = new TagTypePage();
        Bundle args = new Bundle();
        args.putInt("TAGTYPE", getTag(page));
        fragment.setArguments(args);
        return fragment;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        activity=(TagFilterActivity)getActivity();
        type=TagType.values()[ getArguments().getInt("TAGTYPE")];
        View rootView = inflater.inflate(R.layout.fragment_tag_filter, container, false);
        recyclerView=rootView.findViewById(R.id.recycler);

        if(Global.getTheme()== Global.ThemeScheme.BLACK){
            recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), RecyclerView.VERTICAL));
            recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), RecyclerView.HORIZONTAL));
        }

        loadTags();
        return rootView;
    }
    public void loadTags(){

        recyclerView.setLayoutManager(new CustomGridLayoutManager(activity, getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE?4:2));
        TagsAdapter adapter;
        switch(type){
            case UNKNOWN:adapter=new TagsAdapter(activity,query,false);break;
            case CATEGORY:adapter=new TagsAdapter(activity,query,true);break;
            default:adapter=new TagsAdapter(activity,query,type);break;
        }
        recyclerView.setAdapter(adapter);
    }
    public void refilter(String newText){
        if(activity!=null)activity.runOnUiThread(() -> ((TagsAdapter)recyclerView.getAdapter()).getFilter().filter(newText));
    }

    public void reset(){
        switch(type){
            case UNKNOWN:
                TagV2.resetAllStatus();break;
            case CATEGORY:break;
            default:
                Intent i=new Intent(activity, ScrapeTags.class);
                activity.startService(i);
                break;
        }
    }

    public void changeSize() {
        refilter(query);
    }
}

