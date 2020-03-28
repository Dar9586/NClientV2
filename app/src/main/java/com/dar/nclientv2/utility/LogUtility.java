package com.dar.nclientv2.utility;

import android.util.Log;

import com.dar.nclientv2.settings.Global;

public class LogUtility {
    public static void d(Object message){
        if(message==null)return;
        Log.d(Global.LOGTAG, message.toString());
    }
    public static void d(Object message,Throwable throwable){
        if(message==null)message="";
        Log.d(Global.LOGTAG,message.toString(),throwable);
    }
    public static void i(Object message){
        if(message==null)return;
        Log.i(Global.LOGTAG, message.toString());
    }
    public static void i(Object message,Throwable throwable){
        if(message==null)message="";
        Log.i(Global.LOGTAG,message.toString(),throwable);
    }
    public static void e(Object message){
        if(message==null)return;
        Log.e(Global.LOGTAG, message.toString());
    }
    public static void e(Object message,Throwable throwable){
        if(message==null)message="";
        Log.e(Global.LOGTAG,message.toString(),throwable);
    }
}
