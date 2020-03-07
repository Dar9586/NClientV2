package com.dar.nclientv2.utility;

import android.util.Log;

import com.dar.nclientv2.settings.Global;

public class LogUtility {
    public static void d(Object message){
        Log.d(Global.LOGTAG, message.toString());
    }
    public static void d(Object message,Throwable throwable){
        Log.d(Global.LOGTAG,message.toString(),throwable);
    }
    public static void e(Object message){
        Log.e(Global.LOGTAG, message.toString());
    }
    public static void e(Object message,Throwable throwable){
        Log.e(Global.LOGTAG,message.toString(),throwable);
    }
    public static void i(Object message){
        Log.i(Global.LOGTAG, message.toString());
    }
    public static void i(Object message,Throwable throwable){
        Log.i(Global.LOGTAG,message.toString(),throwable);
    }

}
