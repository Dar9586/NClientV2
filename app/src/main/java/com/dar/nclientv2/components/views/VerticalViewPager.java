package com.dar.nclientv2.components.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.dar.nclientv2.components.widgets.CustomViewPager;
import com.dar.nclientv2.utility.LogUtility;

public class VerticalViewPager extends ViewPager {
    private CustomViewPager.OnItemClickListener mOnItemClickListener;
    private boolean verticalMode = true;
    @Nullable
    private OnClickListener onClickListener;

    public VerticalViewPager(Context context) {
        this(context, null);
    }

    public VerticalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        if (verticalMode) return false;
        else return super.canScrollHorizontally(direction);
    }

    public void setVerticalMode(boolean verticalMode) {
        this.verticalMode = verticalMode;
        init();
    }

    @Override
    public boolean canScrollVertically(int direction) {
        if (verticalMode) return super.canScrollHorizontally(direction);
        else return super.canScrollVertically(direction);
    }

    private void init() {
        setPageTransformer(true, verticalMode ? new VerticalPageTransformer() : null);
        setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        setup();
    }

    @Override
    public void setOnClickListener(@Nullable final OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        try {
            final boolean toIntercept = super.onInterceptTouchEvent(flipXY(ev));
            flipXY(ev);
            return toIntercept;
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean performClick() {
        try {
            return super.performClick();
        } catch (IllegalArgumentException ex) {
            LogUtility.e(ex.getLocalizedMessage(), ex);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            performClick();
            final boolean toHandle = super.onTouchEvent(flipXY(ev));
            flipXY(ev);
            return toHandle;
        } catch (IllegalArgumentException ex) {
            LogUtility.e(ex.getLocalizedMessage(), ex);
        }
        return false;
    }


    private MotionEvent flipXY(MotionEvent ev) {
        if (!verticalMode) return ev;
        ev.setLocation(ev.getY(), ev.getX());
        return ev;
    }

    private void setup() {
        final GestureDetector tapGestureDetector = new GestureDetector(getContext(), new VerticalViewPager.TapGestureListener());
        final GestureDetector onSingleTapConfirmedGestureDetector =
            new GestureDetector(getContext(), new OnSingleTapConfirmedGestureListener(this));

        setOnTouchListener((v, event) -> {
            tapGestureDetector.onTouchEvent(event);
            onSingleTapConfirmedGestureDetector.onTouchEvent(event);
            performClick();
            return false;
        });
    }

    public void setOnItemClickListener(CustomViewPager.OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private static final class VerticalPageTransformer implements ViewPager.PageTransformer {
        @Override
        public void transformPage(View view, float position) {
            final int pageWidth = view.getWidth();
            final int pageHeight = view.getHeight();
            if (position < -1) {
                view.setAlpha(0);
            } else if (position <= 1) {
                view.setAlpha(1);
                view.setTranslationX(pageWidth * -position);
                float yPosition = position * pageHeight;
                view.setTranslationY(yPosition);
            } else {
                view.setAlpha(0);
            }
        }
    }

    private class TapGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(getCurrentItem());
            }
            return true;
        }
    }

    private class OnSingleTapConfirmedGestureListener extends GestureDetector.SimpleOnGestureListener {

        @NonNull
        private final View view;

        public OnSingleTapConfirmedGestureListener(@NonNull final View view) {
            this.view = view;
        }

        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            if (onClickListener != null) {
                onClickListener.onClick(view);
            }
            return true;
        }
    }

}
