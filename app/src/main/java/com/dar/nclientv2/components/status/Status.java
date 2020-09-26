package com.dar.nclientv2.components.status;

import android.graphics.Color;

public class Status {
    public final int color;
    public final String name;

    Status(int color, String name) {
        this.color = Color.argb(0x7f,Color.red(color),Color.green(color),Color.blue(color));;
        this.name = name;
    }
    public int opaqueColor(){
        return color|0xff000000;
    }
}
