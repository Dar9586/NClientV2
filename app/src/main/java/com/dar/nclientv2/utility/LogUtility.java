package com.dar.nclientv2.utility;

import android.util.Log;

public class LogUtility {
    public static final String LOGTAG = "NCLIENTLOG";

    public static void d(Object message) {
        if (message == null) return;
        Log.d(LogUtility.LOGTAG, message.toString());
    }

    public static void d(Object message, Throwable throwable) {
        if (message == null) message = "";
        Log.d(LogUtility.LOGTAG, message.toString(), throwable);
    }

    public static void i(Object message) {
        if (message == null) return;
        Log.i(LogUtility.LOGTAG, message.toString());
    }

    public static void i(Object message, Throwable throwable) {
        if (message == null) message = "";
        Log.i(LogUtility.LOGTAG, message.toString(), throwable);
    }

    public static void e(Object message) {
        if (message == null) return;
        Log.e(LogUtility.LOGTAG, message.toString());
    }

    public static void e(Object message, Throwable throwable) {
        if (message == null) message = "";
        Log.e(LogUtility.LOGTAG, message.toString(), throwable);
    }
}
