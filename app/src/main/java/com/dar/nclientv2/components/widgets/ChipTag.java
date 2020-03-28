package com.dar.nclientv2.components.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.settings.Global;
import com.google.android.material.chip.Chip;

public class ChipTag extends Chip {
    private Tag tag;
    private boolean canBeAvoided=true;
    public ChipTag(Context context) {
        super(context);
    }

    public ChipTag(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChipTag(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public void init(Tag t,boolean close,boolean canBeAvoided){
        setTag(t);
        setCloseIconVisible(close);
        setCanBeAvoided(canBeAvoided);
    }
    private void setCanBeAvoided(boolean canBeAvoided) {
        this.canBeAvoided = canBeAvoided;
    }

    private void setTag(Tag tag) {
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
    public void updateStatus(){
        switch (tag.getStatus()){
            case DEFAULT:changeStatus(TagStatus.ACCEPTED);break;
            case ACCEPTED:changeStatus(canBeAvoided?TagStatus.AVOIDED:TagStatus.DEFAULT);break;
            case AVOIDED:changeStatus(TagStatus.DEFAULT) ;break;
        }
    }
    private void loadStatusIcon(){
        Drawable drawable = ContextCompat.getDrawable(getContext(),tag.getStatus()==TagStatus.ACCEPTED?R.drawable.ic_check:tag.getStatus()==TagStatus.AVOIDED?R.drawable.ic_close:R.drawable.ic_void);
        if(drawable==null){
            setChipIconResource(tag.getStatus()==TagStatus.ACCEPTED?R.drawable.ic_check:tag.getStatus()==TagStatus.AVOIDED?R.drawable.ic_close:R.drawable.ic_void);
            return;
        }
        setChipIcon(drawable);
        Global.setTint(getChipIcon());
    }
}
