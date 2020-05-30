package com.dar.nclientv2.components.widgets;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.SearchView;

public class CustomSearchView extends SearchView {
    private SearchView.SearchAutoComplete mSearchSrcTextView;

    public CustomSearchView(Context context) {
        super(context);
    }

    public CustomSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setOnQueryTextListener(OnQueryTextListener listener) {
        super.setOnQueryTextListener(listener);
        mSearchSrcTextView = this.findViewById(androidx.appcompat.R.id.search_src_text);
        mSearchSrcTextView.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (listener != null) {
                listener.onQueryTextSubmit(getQuery().toString());
            }
            return true;
        });
    }
}
