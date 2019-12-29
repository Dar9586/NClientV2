package com.dar.nclientv2.targets;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class BitmapTarget extends CustomTarget<Bitmap> {
    private final ImageView view;
    private boolean stop=false;
    public BitmapTarget(ImageView view) {
        this.view = view;
    }

    @Override
    public void onStop() {
        super.onStop();
        stop=true;
    }

    @Override
    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
        if(!stop)view.setImageBitmap(resource);
    }

    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {
        if(!stop)view.setImageDrawable(placeholder);
    }

    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
        super.onLoadFailed(errorDrawable);
        if(!stop)view.setImageDrawable(errorDrawable);
    }

    @Override
    public void onLoadStarted(@Nullable Drawable placeholder) {
        super.onLoadStarted(placeholder);
        if(!stop)view.setImageDrawable(placeholder);
    }
}
