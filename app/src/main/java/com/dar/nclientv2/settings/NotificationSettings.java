package com.dar.nclientv2.settings;

import android.app.Notification;
import android.content.Context;

import androidx.core.app.NotificationManagerCompat;

import com.dar.nclientv2.R;
import com.dar.nclientv2.utility.LogUtility;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationSettings {

    private final NotificationManagerCompat notificationManager;

    private static final List<Integer> notificationArray= new CopyOnWriteArrayList<>();
    private static NotificationSettings notificationSettings;
    private static int notificationId=999,maximumNotification;

    private NotificationSettings(NotificationManagerCompat notificationManager) {
        this.notificationManager = notificationManager;
    }
    public static int getNotificationId() {
        return notificationId++;
    }

    public static void initializeNotificationManager(Context context){
        notificationSettings=new NotificationSettings(NotificationManagerCompat.from(context.getApplicationContext()));
        maximumNotification=context.getSharedPreferences("Settings",0).getInt(context.getString(R.string.key_maximum_notification),25);
        trimArray();
    }
    public static void notify(String channel, int notificationId, Notification notification){
        if(maximumNotification==0)return;
        notificationArray.remove(Integer.valueOf(notificationId));
        notificationArray.add(notificationId);
        trimArray();
        LogUtility.d("Notification count: "+notificationArray.size());
        notificationSettings.notificationManager.notify(null,notificationId,notification);
    }
    public static void cancel(String channel, int notificationId){
        notificationSettings.notificationManager.cancel(channel,notificationId);
        notificationArray.remove(Integer.valueOf(notificationId));
    }
    private static void trimArray(){
        while(notificationArray.size() > maximumNotification) {
            int first = notificationArray.remove(0);
            notificationSettings.notificationManager.cancel(first);
        }
    }
}
