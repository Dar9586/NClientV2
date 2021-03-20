package com.dar.nclientv2.utility;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.bitmap.Rotate;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.components.GlideX;
import com.dar.nclientv2.settings.Global;

import java.io.File;

public class ImageDownloadUtility {
    public static void preloadImage(Context context, Uri url) {
        if (Global.getDownloadPolicy() == Global.DataUsageType.NONE) return;
        RequestManager manager = GlideX.with(context);
        LogUtility.d("Requested url glide: " + url);
        if (manager != null) manager.load(url).preload();
    }

    public static void loadImageOp(Context context, ImageView view, File file, int angle) {
        RequestManager glide = GlideX.with(context);
        if (glide == null) return;
        Drawable logo = Global.getLogo(context.getResources());
        glide.load(file).transform(new Rotate(angle)).error(logo).placeholder(logo).into(view);
        LogUtility.d("Requested file glide: " + file);
    }

    public static void loadImageOp(Context context, ImageView view, Gallery gallery, int page, int angle) {
        Uri url = getUrlForGallery(gallery, page, true);
        loadImageOp(context,view,url,angle);
    }
    public static void loadImageOp(Context context, ImageView view, Uri url, int angle) {
        LogUtility.d("Requested url glide: " + url);
        if (Global.getDownloadPolicy() == Global.DataUsageType.NONE) {
            loadLogo(view);
            return;
        }
        RequestManager glide = GlideX.with(context);
        if (glide == null) return;
        Drawable logo = Global.getLogo(context.getResources());
        RequestBuilder<Drawable>dra= glide.load(url);
        if(angle!=0)
            dra=dra.transform(new Rotate(angle));
        dra.error(logo)
            .placeholder(logo)
            .into(view);
    }

    private static Uri getUrlForGallery(Gallery gallery, int page, boolean shouldFull) {
        return shouldFull ? gallery.getPageUrl(page) : gallery.getLowPage(page);
    }

    public static void downloadPage(Activity activity, ImageView imageView, Gallery gallery, int page, boolean shouldFull) {
        shouldFull=gallery.getPageExtension(page).equals("gif")||shouldFull;
        loadImageOp(activity,imageView,getUrlForGallery(gallery, page, shouldFull),0);
    }

    private static void loadLogo(ImageView imageView) {
        imageView.setImageDrawable(Global.getLogo(imageView.getResources()));
    }

    public static void loadImage(Activity activity, Uri url, ImageView imageView) {
        loadImageOp(activity,imageView,url,0);
    }

    public static void loadImage(Activity activity, File file, ImageView imageView) {
        loadImage(activity,Uri.fromFile(file),imageView);
    }

    /**
     * Load Resource using id
     */
    public static void loadImage(@DrawableRes int resource, ImageView imageView) {
        imageView.setImageResource(resource);
    }
}
