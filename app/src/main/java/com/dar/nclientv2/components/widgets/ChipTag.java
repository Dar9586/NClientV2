package com.dar.nclientv2.components.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.google.android.material.chip.Chip;

public class ChipTag extends Chip {
    private Tag tag;
    public ChipTag(Context context) {
        super(context);
    }

    public ChipTag(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChipTag(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTag(Tag tag) {
        this.tag = tag;
        setText(tag.getName());
        loadStatusIcon();
    }

    @Override
    public Tag getTag() {
        return tag;
    }
    public void changeStatus(TagStatus status){
        tag.setStatus(status);
        loadStatusIcon();
    }

    private void loadStatusIcon(){
        Drawable drawable = ContextCompat.getDrawable(getContext(),tag.getStatus()==TagStatus.ACCEPTED?R.drawable.ic_check:tag.getStatus()==TagStatus.AVOIDED?R.drawable.ic_close:R.drawable.ic_void);
        if(drawable==null){
            setChipIconResource(tag.getStatus()==TagStatus.ACCEPTED?R.drawable.ic_check:tag.getStatus()==TagStatus.AVOIDED?R.drawable.ic_close:R.drawable.ic_void);
            return;
        }
        DrawableCompat.setTint(drawable,Color.BLACK);
        setChipIcon(drawable);
    }
}
