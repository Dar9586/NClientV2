package com.dar.nclientv2.utility;

import android.util.Log;

import com.dar.nclientv2.settings.Global;

public class LogUtility {
    public static void d(String message){
        Log.d(Global.LOGTAG, message);
    }
    public static void d(String message,Throwable throwable){
        Log.d(Global.LOGTAG,message,throwable);
    }
    public static void e(String message){
        Log.e(Global.LOGTAG, message);
    }
    public static void e(String message,Throwable throwable){
        Log.e(Global.LOGTAG,message,throwable);
    }
    public static void i(String message){
        Log.i(Global.LOGTAG, message);
    }
    public static void i(String message,Throwable throwable){
        Log.i(Global.LOGTAG,message,throwable);
    }

}
