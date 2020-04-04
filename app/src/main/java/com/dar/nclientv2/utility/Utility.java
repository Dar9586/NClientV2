package com.dar.nclientv2.utility;

import java.util.Random;

public class Utility {
    public static final Random RANDOM=new Random(System.nanoTime());
    public static final String BASE_URL="https://nhentai.net/";
    public static void threadSleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
