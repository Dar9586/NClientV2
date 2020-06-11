package com.dar.nclientv2.loginapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.CSRFGet;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class LoginThread extends Thread {
    private static final Object lock=new Object();
    private static final String LOGIN_URL = Utility.getBaseUrl() +"login/";
    private static final String LOGOUT_URL = Utility.getBaseUrl() +"logout/";
    public interface LoginResponse{
        void responseCode(int code);
    }
    private final FormBody.Builder requestBody=new FormBody.Builder();
    private final boolean login;
    private final LoginResponse loginResponse;
    @Nullable private String token=null;
    public LoginThread(boolean login,@NonNull LoginResponse loginResponse) {
        this.login = login;
        this.loginResponse=loginResponse;
    }
    public LoginThread setCredential(String username,String password){
            requestBody
                .add("username_or_email",username)
                .add("password",password);
        return this;
    }
    @Override
    public void run() {
        int code=-1;
        try {
            code=login?login():logout();
        }catch (Exception e){
            LogUtility.e(e,e);
        }
        loginResponse.responseCode(code);
    }

    private void lockWait(){
        synchronized (lock){
            try {
                lock.wait(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private void lockNotify(){
        synchronized (lock){
            lock.notify();
        }
    }

    private void getToken(String url){
        new CSRFGet(new CSRFGet.Response() {
            @Override
            public void onResponse(String token) {
                LoginThread.this.token=token;
                lockNotify();
            }

            @Override
            public void onError(Exception e) {
                LoginThread.this.token=null;
                lockNotify();
            }
        },url,"csrfmiddlewaretoken").start();
    }

    private int makeRequest(String url) throws IOException {
        getToken(url);
        lockWait();
        if(token==null)return 0;
        requestBody.add("csrfmiddlewaretoken",token);
        LogUtility.d("Found token: "+token);
        LogUtility.d(requestBody);
        return makeEffectiveRequest(url);
    }

    private int makeEffectiveRequest(String url) throws IOException {
        Response response=Global.getClient().newCall(
                new Request.Builder()
                        .addHeader("Referer", url)
                        .url(url)
                        .post(requestBody.build())
                        .build()
        ).execute();
        int responseCode=response.networkResponse().code();
        response.close();
        return responseCode;
    }

    private int login() throws IOException {
        return makeRequest(LOGIN_URL);
    }
    private int logout() throws IOException{
        return makeRequest(LOGOUT_URL);
    }


}
