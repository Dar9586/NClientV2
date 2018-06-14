package com.dar.nclientv2.components;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.dar.nclientv2.settings.Global;

public class CustomViewPager extends ViewPager {
    private OnItemClickListener mOnItemClickListener;
    public CustomViewPager(@NonNull Context context) {
        super(context);
        setup();
    }

    public CustomViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            performClick();
            return super.onTouchEvent(ev);
        } catch (IllegalArgumentException ex) {
            Log.e(Global.LOGTAG,ex.getLocalizedMessage(),ex);
        }
        return false;
    }

    @Override
    public boolean performClick() {
        try {
            return super.performClick();
        } catch (IllegalArgumentException ex) {
            Log.e(Global.LOGTAG,ex.getLocalizedMessage(),ex);
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return false;
    }
    private void setup() {
        final GestureDetector tapGestureDetector = new    GestureDetector(getContext(), new TapGestureListener());
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                tapGestureDetector.onTouchEvent(event);
                performClick();
                return false;
            }

        });
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    interface OnItemClickListener {
        void onItemClick(int position);
    }

    private class TapGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if(mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(getCurrentItem());
            }
            return true;
        }
    }
}
