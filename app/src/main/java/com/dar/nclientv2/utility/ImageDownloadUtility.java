package com.dar.nclientv2.utility;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.bitmap.Rotate;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.components.GlideX;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.targets.BitmapTarget;

import java.io.File;

public class ImageDownloadUtility {
    public static void preloadImage(Context context, Object object){
        if(Global.getDownloadPolicy()== Global.DataUsageType.NONE)return;
        RequestManager manager= GlideX.with(context);
        if(manager!=null)manager.load(object).preload();

    }
    @Nullable
    public static BitmapTarget loadImageOp(Context context, ImageView view, File file, int angle){
        RequestManager glide=GlideX.with(context);
        if(glide==null)return null;
        Drawable logo=Global.getLogo(context.getResources());
        BitmapTarget target=new BitmapTarget(view);
        glide.asBitmap().transform(new Rotate(angle)).error(logo).placeholder(logo).load(file).into(target);
        return target;
    }
    @Nullable
    public static BitmapTarget loadImageOp(Context context,ImageView view,Gallery gallery,int page,int angle){
        String url=getUrlForGallery(gallery, page, true);
        LogUtility.d("Requested url glide: "+ url);
        if(Global.getDownloadPolicy()== Global.DataUsageType.NONE){loadLogo(view);return null;}
        RequestManager glide=GlideX.with(context);
        if(glide==null)return null;
        Drawable logo=Global.getLogo(context.getResources());
        BitmapTarget target=new BitmapTarget(view);
        glide.asBitmap()
                .transform(new Rotate(angle))
                .error(logo)
                .placeholder(logo)
                .load(url)
                .into(target);
        return target;
    }
    private static String getUrlForGallery(Gallery gallery, int page, boolean shouldFull){
        return shouldFull? gallery.getPageUrl(page):gallery.getLowPage(page);
    }

    public static void downloadPage(Activity activity, ImageView imageView, Gallery gallery, int page, boolean shouldFull){
        loadImage(activity,getUrlForGallery(gallery,page,shouldFull),imageView);
    }

    private static void loadLogo(ImageView imageView){
        imageView.setImageDrawable(Global.getLogo(imageView.getResources()));
    }

    public static void loadImage(Activity activity,String url,ImageView imageView){
        LogUtility.d("Requested url glide: "+ url);
        if(activity.isFinishing()||Global.isDestroyed(activity))return;
        if(Global.getDownloadPolicy()== Global.DataUsageType.NONE){loadLogo(imageView);return;}
        RequestManager glide=GlideX.with(activity);
        if(glide==null)return;
        int logo=Global.getLogo();
        glide.load(url)
                .placeholder(logo)
                .error(logo)
                .into(imageView);
    }
    public static void loadImage(Activity activity,File file,ImageView imageView){
        if(activity.isFinishing()||Global.isDestroyed(activity))return;
        RequestManager glide=GlideX.with(activity);
        if(glide==null)return;
        int logo=Global.getLogo();
        glide.load(file)
                .placeholder(logo)
                .error(logo)
                .into(imageView);
    }
    /**
     * Load Resource using id
     * */
    public static void loadImage(@DrawableRes int resource, ImageView imageView){
        imageView.setImageResource(resource);
    }
}
