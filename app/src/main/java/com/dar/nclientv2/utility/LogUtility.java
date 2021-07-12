package com.dar.nclientv2.utility;

import android.util.Log;

import java.util.Arrays;

public class LogUtility {
    public static final String LOGTAG = "NCLIENTLOG";

    public static void d(Object... message) {
        if (message == null) return;
        if (message.length == 1) Log.d(LogUtility.LOGTAG, message[0].toString());
        else Log.d(LogUtility.LOGTAG, Arrays.toString(message));
    }

    public static void d(Object message, Throwable throwable) {
        if (message == null) message = "";
        Log.d(LogUtility.LOGTAG, message.toString(), throwable);
    }

    public static void i(Object... message) {
        if (message == null) return;
        if (message.length == 1) Log.i(LogUtility.LOGTAG, message[0].toString());
        else Log.i(LogUtility.LOGTAG, Arrays.toString(message));
    }

    public static void i(Object message, Throwable throwable) {
        if (message == null) message = "";
        Log.i(LogUtility.LOGTAG, message.toString(), throwable);
    }

    public static void e(Object... message) {
        if (message == null) return;
        if (message.length == 1) Log.e(LogUtility.LOGTAG, message[0].toString());
        else Log.e(LogUtility.LOGTAG, Arrays.toString(message));
    }

    public static void e(Object message, Throwable throwable) {
        if (message == null) message = "";
        Log.e(LogUtility.LOGTAG, message.toString(), throwable);
    }
}
