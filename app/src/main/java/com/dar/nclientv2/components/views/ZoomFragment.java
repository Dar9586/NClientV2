package com.dar.nclientv2.components.views;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;

public class ZoomFragment extends Fragment {
    private GenericGallery gallery;
    private int page;
    private PhotoView photoView;
    private File galleryFolder;
    private View.OnClickListener clickListener;
    public ZoomFragment() { }

    public void setClickListener(View.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public static ZoomFragment newInstance(GenericGallery gallery, int page) {
        Bundle args = new Bundle();
        if(Global.useRtl())page=gallery.getPageCount()-1-page;
        args.putParcelable("GALLERY",gallery);
        args.putInt("PAGE",page);
        ZoomFragment fragment = new ZoomFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_zoom, container, false);
        assert getArguments()!=null;
        photoView=rootView.findViewById(R.id.image);
        gallery=getArguments().getParcelable("GALLERY");
        page=getArguments().getInt("PAGE",0);
        LogUtility.d("Requested: "+page);
        assert gallery!=null;
        galleryFolder=Global.findGalleryFolder(gallery.getId());
        loadPage()
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_close)
                .into(photoView);
        photoView.setOnClickListener(v -> {
            if(clickListener!=null)clickListener.onClick(v);
        });
        return rootView;
    }

    private RequestBuilder<Drawable> loadPage() {
        RequestBuilder<Drawable> request;
        RequestManager glide=Glide.with(photoView);

        File file=LocalGallery.getPage(galleryFolder,page+1);
        if(file!=null){
            request=glide.load(file);
        }else {
            if (gallery.isLocal()) request = glide.load(R.mipmap.ic_launcher);
            else request = glide.load(((Gallery) gallery).getPage(page));
        }
        return request;
    }
    public Drawable getDrawable(){
        return photoView.getDrawable();
    }
}
