package com.dar.nclientv2.async;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.AsyncTask;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

import com.dar.nclientv2.components.BlurImageView;
import com.dar.nclientv2.settings.Global;

public class AsyncBlurImage extends AsyncTask<Object,Void,Object[]>{
    private static final float RADIUS=5f;

    @Override
    protected Object[] doInBackground(Object... objs){
        Drawable drawable=(Drawable)objs[0];
        BlurImageView imageView=(BlurImageView)objs[1];
        Bitmap image=null;
        if(drawable==null)return null;
        if(drawable instanceof BitmapDrawable)image=((BitmapDrawable)drawable).getBitmap();
        else if(drawable instanceof VectorDrawable){
            VectorDrawable v=(VectorDrawable)drawable;
            image=Bitmap.createBitmap(v.getIntrinsicWidth(),v.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(image);
            v.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            v.draw(canvas);
        }else Log.e(Global.LOGTAG,"TYPE NOT FOUND: "+drawable.getClass());
        image=Bitmap.createScaledBitmap(image,image.getWidth()>>1,image.getHeight()>>1,false);
        RenderScript rs = RenderScript.create(imageView.getContext());
        Allocation tmpIn = Allocation.createFromBitmap(rs, image);
        Allocation tmpOut = Allocation.createTyped(rs, tmpIn.getType());
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        theIntrinsic.setRadius(RADIUS);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(image);

        rs.destroy();
        tmpIn.destroy();
        tmpOut.destroy();
        theIntrinsic.destroy();

        Object[]x=new Object[2];
        x[0]=imageView;
        x[1]=new BitmapDrawable(imageView.getResources(),image);
        return x;
    }

    @Override
    protected void onPostExecute(Object[] objs){
        if(objs!=null) ((BlurImageView)objs[0]).setImageDrawable((Drawable)objs[1],true);
    }
}
