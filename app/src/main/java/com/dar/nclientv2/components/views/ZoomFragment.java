package com.dar.nclientv2.components.views;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.bitmap.Rotate;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.components.GlideX;
import com.dar.nclientv2.files.GalleryFolder;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;

public class ZoomFragment extends Fragment {
    private static final float MAX_SCALE = 4f;
    private int page, galleryId;
    private PhotoView photoView = null;
    private ImageButton retryButton;
    private GalleryFolder galleryFolder=null;
    private Uri url;
    private int degree=0;
    private View.OnClickListener clickListener;
    private CustomTarget<Drawable> target = null;

    public ZoomFragment() {
    }


    public static ZoomFragment newInstance(GenericGallery gallery, int page,@Nullable GalleryFolder directory) {
        Bundle args = new Bundle();
        if(Global.useRtl())page=gallery.getPageCount()-1-page;
        args.putInt("PAGE",page);
        args.putInt("ID",gallery.getId());
        args.putString("URL",gallery.isLocal()?null:((Gallery)gallery).getPageUrl(page).toString());
        args.putParcelable("FOLDER",directory);
        ZoomFragment fragment = new ZoomFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public void setClickListener(View.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    private float calculateScaleFactor(int width, int height) {
        if (height < width * 2) return Global.getDefaultZoom();
        //width:1=dWidth:x
        //float size = ((float) Global.getDeviceHeight(getActivity())) / height;//the image has been resized size times
        //float widthTrueSize = width * size;//so also the width has been resized
        //float finalSize = ((float) Global.getDeviceWidth(getActivity()) / widthTrueSize);//scale the width
        float finalSize =
                (float) Global.getDeviceWidth(getActivity()) * height /
                        ((float) Global.getDeviceHeight(getActivity()) * width);
        finalSize = Math.max(finalSize, Global.getDefaultZoom());
        finalSize = Math.min(finalSize, MAX_SCALE);
        LogUtility.d("Final scale: " + finalSize);
        return (float) Math.floor(finalSize);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_zoom, container, false);
        assert getArguments() != null;
        //find views
        photoView = rootView.findViewById(R.id.image);
        retryButton = rootView.findViewById(R.id.imageView);
        //read arguments

        page=getArguments().getInt("PAGE",0);
        galleryId=getArguments().getInt("ID",0);
        String str=getArguments().getString("URL");
        url= str==null?null:Uri.parse(str);
        galleryFolder=getArguments().getParcelable("FOLDER");
        photoView.setAllowParentInterceptOnEdge(true);
        photoView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(v);
        });
        photoView.setMaximumScale(MAX_SCALE);
        retryButton.setOnClickListener(v -> loadImage());
        createTarget();
        loadImage();
        return rootView;
    }

    private void createTarget() {
        target = new CustomTarget<Drawable>() {
            void applyDrawable(ImageView toShow, ImageView toHide, Drawable drawable) {
                toShow.setVisibility(View.VISIBLE);
                toHide.setVisibility(View.GONE);
                toShow.setImageDrawable(drawable);
                if (toShow instanceof PhotoView)
                    scalePhoto(drawable);
            }

            @Override
            public void onLoadStarted(@Nullable Drawable placeholder) {
                super.onLoadStarted(placeholder);
                applyDrawable(photoView, retryButton, placeholder);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                super.onLoadFailed(errorDrawable);
                applyDrawable(retryButton, photoView, errorDrawable);
            }

            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                applyDrawable(photoView, retryButton, resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                applyDrawable(photoView, retryButton, placeholder);
            }
        };
    }

    private void scalePhoto(Drawable drawable) {
        photoView.setScale(calculateScaleFactor(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight()
        ), 0, 0, false);
    }

    private void loadImage() {
        cancelRequest();
        RequestBuilder<Drawable> dra = loadPage();
        if (dra == null) return;
        dra
                .transform(new Rotate(degree))
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_refresh)
                .into(target);
    }

    @Nullable
    private RequestBuilder<Drawable> loadPage() {
        RequestBuilder<Drawable> request;
        RequestManager glide=GlideX.with(photoView);
        if(glide==null)return null;
        File file=galleryFolder==null?null:galleryFolder.getPage(page+1);
        if(file!=null){
            request=glide.load(file);
            LogUtility.d("Requested file glide: "+ file);
        }else {
            if (url==null) request = glide.load(R.mipmap.ic_launcher);
            else {
                LogUtility.d("Requested url glide: " + url);
                request = glide.load(url);
            }
        }
        return request;
    }

    public Drawable getDrawable() {
        return photoView.getDrawable();
    }

    public void cancelRequest() {
        if (photoView != null && target != null) {
            RequestManager manager = GlideX.with(photoView);
            if (manager != null) manager.clear(target);
        }
    }

    private void updateDegree() {
        degree = (degree + 270) % 360;
        loadImage();
    }

    public void rotate() {
        updateDegree();
    }
}
