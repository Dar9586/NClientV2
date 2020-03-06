package com.dar.nclientv2.components.widgets;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dar.nclientv2.utility.LogUtility;

public class CustomSwipe extends SwipeRefreshLayout {
    public CustomSwipe(@NonNull Context context) {
        super(context);
    }

    public CustomSwipe(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setEnabled(boolean refreshing) {
        try{
            throw new Exception();
        }catch (Exception e){
            LogUtility.e("NEW VALUE: "+refreshing+",,"+e.getLocalizedMessage(),e);
        }
        super.setRefreshing(refreshing);
    }
}
