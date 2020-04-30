package com.dar.nclientv2.components.views;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.bitmap.Rotate;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;

public class ZoomFragment extends Fragment {
    // TODO: 25/04/20 java.lang.VerifyError: com/bumptech/glide/load/data/ParcelFileDescriptorRewinder at com.bumptech.glide.Glide
    private int page,galleryId;
    private PhotoView photoView=null;
    private ImageButton retryButton;
    private File galleryFolder;
    private String url;
    private int degree=0;
    private View.OnClickListener clickListener;
    private CustomTarget<Drawable>target=null;
    public ZoomFragment() { }

    public void setClickListener(View.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }


    public static ZoomFragment newInstance(GenericGallery gallery, int page) {
        Bundle args = new Bundle();
        if(Global.useRtl())page=gallery.getPageCount()-1-page;
        args.putInt("PAGE",page);
        args.putInt("ID",gallery.getId());
        args.putString("URL",gallery.isLocal()?null:((Gallery)gallery).getPage(page));
        ZoomFragment fragment = new ZoomFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_zoom, container, false);
        assert getArguments()!=null;
        //find views
        photoView=rootView.findViewById(R.id.image);
        retryButton=rootView.findViewById(R.id.imageView);
        //read arguments
        page=getArguments().getInt("PAGE",0);
        galleryId=getArguments().getInt("ID",0);
        url=getArguments().getString("URL");

        LogUtility.d("Requested: "+page);
        if(Global.hasStoragePermission(photoView.getContext()))
            galleryFolder=Global.findGalleryFolder(galleryId);

        photoView.setOnClickListener(v -> {
            if(clickListener!=null)clickListener.onClick(v);
        });
        retryButton.setOnClickListener(v -> loadImage());
        createTarget();
        loadImage();

        return rootView;
    }

    private void createTarget() {
        target=new CustomTarget<Drawable>() {
            void applyDrawable(ImageView toShow,ImageView toHide,Drawable drawable){
                toShow.setVisibility(View.VISIBLE);
                toHide.setVisibility(View.GONE);
                toShow.setImageDrawable(drawable);
            }

            @Override
            public void onLoadStarted(@Nullable Drawable placeholder) {
                super.onLoadStarted(placeholder);
                applyDrawable(photoView,retryButton,placeholder);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                super.onLoadFailed(errorDrawable);
                applyDrawable(retryButton,photoView,errorDrawable);
            }

            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                applyDrawable(photoView,retryButton,resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                applyDrawable(photoView,retryButton,placeholder);
            }
        };
    }

    private void loadImage(){
        cancelRequest();
        loadPage()
                .transform(new Rotate(degree))
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_refresh)
                .into(target);
    }
    private RequestBuilder<Drawable> loadPage() {
        RequestBuilder<Drawable> request;
        RequestManager glide=Glide.with(photoView);

        File file=LocalGallery.getPage(galleryFolder,page+1);
        if(file!=null){
            request=glide.load(file);
        }else {
            if (url==null) request = glide.load(R.mipmap.ic_launcher);
            else request = glide.load(url);
        }
        return request;
    }
    public Drawable getDrawable(){
        return photoView.getDrawable();
    }

    public void cancelRequest() {
        if(photoView!=null&&target!=null)
            Glide.with(photoView).clear(target);
    }
    private void updateDegree(){
        degree=(degree+270)%360;
        loadImage();
    }
    public void rotate() {
        updateDegree();
    }
}
