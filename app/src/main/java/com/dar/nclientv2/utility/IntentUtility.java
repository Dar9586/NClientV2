package com.dar.nclientv2.utility;

import android.app.Activity;
import android.content.Intent;

public class IntentUtility extends Intent {
    public static String PACKAGE_NAME;

    public static void startAnotherActivity(Activity activity, Intent intent){
        activity.runOnUiThread(()->activity.startActivity(intent));
    }
    public static void endActivity(Activity activity){
        activity.runOnUiThread(activity::finish);
    }
}
