package com.dar.nclientv2.components.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.dar.nclientv2.utility.LogUtility;

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
            LogUtility.e(ex.getLocalizedMessage(),ex);
        }
        return false;
    }

    @Override
    public boolean performClick() {
        try {
            return super.performClick();
        } catch (IllegalArgumentException ex) {
            LogUtility.e(ex.getLocalizedMessage(),ex);
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
        setOnTouchListener((v, event) -> {
            tapGestureDetector.onTouchEvent(event);
            performClick();
            return false;
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
