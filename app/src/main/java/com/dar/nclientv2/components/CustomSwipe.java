package com.dar.nclientv2.components;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.dar.nclientv2.settings.Global;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
