package com.dar.nclientv2.loginapi;

import androidx.annotation.NonNull;

import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class LoginThread extends Thread {
    private static final String LOGIN_URL = Utility.BASE_URL +"login/";
    private static final String LOGOUT_URL = Utility.BASE_URL +"logout/";
    public interface LoginResponse{
        void responseCode(int code);
    }
    private final FormBody.Builder requestBody=new FormBody.Builder();
    private final boolean login;
    private final LoginResponse loginResponse;
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
    private int makeRequest(String url) throws IOException {
        Response response = Global.getClient().newCall(new Request.Builder().url(url).build()).execute();
        Document doc=Jsoup.parse(response.body().byteStream(),null,Utility.BASE_URL);
        response.close();
        Element ele=doc.getElementsByAttributeValue("name","csrfmiddlewaretoken").first();
        if(ele==null)return 0;
        String token=ele.attr("value");
        requestBody.add("csrfmiddlewaretoken",token);
        LogUtility.d("Found token: "+token);
        LogUtility.d(requestBody);
        response=Global.getClient().newCall(
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
