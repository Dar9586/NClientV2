package com.dar.nclientv2.components.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dar.nclientv2.settings.Global;

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
            Log.e(Global.LOGTAG,"NEW VALUE: "+refreshing+",,"+e.getLocalizedMessage(),e);
        }
        super.setRefreshing(refreshing);
    }
}
