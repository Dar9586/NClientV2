package com.dar.nclientv2.utility;

public class Utility {
    public static final String BASE_URL="https://nhentai.net/";
    public static void threadSleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
