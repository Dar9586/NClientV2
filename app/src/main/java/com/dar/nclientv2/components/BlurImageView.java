package com.dar.nclientv2.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.dar.nclientv2.async.AsyncBlurImage;

public class BlurImageView extends AppCompatImageView {
    AsyncBlurImage async;
    private boolean blur;

    public BlurImageView(Context context) {
        super(context);
    }

    public BlurImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlurImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBlur(boolean blur){
        this.blur = blur;
        setImageDrawable(getDrawable());
    }

    public boolean isBlur(){
        return blur;
    }

    public void setImageDrawable(Drawable drawable) {
        setImageDrawable(drawable,false);
    }
    public void setImageDrawable(Drawable drawable, boolean async){
        if(this.async!=null&&this.async.getStatus()!= AsyncTask.Status.FINISHED)this.async.cancel(true);
        super.setImageDrawable(drawable);
        if(!async&&blur){
            this.async=new AsyncBlurImage();
            this.async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,drawable,this);
        }
    }
    private static final Paint blurPaint=new Paint();
    static{
        blurPaint.setARGB(150,0,0,0);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(blur)canvas.drawRect(0,0,getWidth(),getHeight(),blurPaint);
    }


}

