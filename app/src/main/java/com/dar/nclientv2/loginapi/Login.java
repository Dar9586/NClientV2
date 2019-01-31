package com.dar.nclientv2.loginapi;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.dar.nclientv2.LoginActivity;
import com.dar.nclientv2.settings.Global;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Login {
    private static final String base_url="https://nhentai.net/";
    private static final String login_url=base_url+"login/";
    private static final String logout_url=base_url+"logout/";
    public static void clear(){
        ((PersistentCookieJar)Global.client.cookieJar()).clear();
    }
    public static void login(final LoginActivity activity, final String username, final String password) {
        activity.runOnUiThread(() -> activity.invalid.setVisibility(View.GONE));
        Global.client.newCall(new Request.Builder().url(login_url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call,@NonNull IOException e) { }
            @Override
            public void onResponse(@NonNull Call call,@NonNull Response response) throws IOException {
                Document jsoup=Jsoup.parse(response.body().byteStream(),null,login_url);
                Element x=jsoup.getElementsByAttributeValue("name","csrfmiddlewaretoken").first();
                if(x!=null) createCookieLogin(activity,username,password,x.attr("value"));
            }
        });
    }
    public static void logout(final Context context) {
        Global.client.newCall(new Request.Builder().url(logout_url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call,@NonNull IOException e) { }
            @Override
            public void onResponse(@NonNull Call call,@NonNull Response response) throws IOException {
                Document jsoup=Jsoup.parse(response.body().byteStream(),null,logout_url);
                createCookieLogout(context,jsoup.getElementsByAttributeValue("name","csrfmiddlewaretoken").first().attr("value"));
            }
        });
    }
    private static void createCookieLogout(final Context context, String token){
        RequestBody b=new FormBody.Builder().add("csrfmiddlewaretoken",token).build();
        Global.client.newCall(new Request.Builder().addHeader("Referer",logout_url).url(logout_url).post(b).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call,@NonNull IOException e) { }

            @Override
            public void onResponse(@NonNull Call call,@NonNull Response response) {
                Log.d(Global.LOGTAG,"Logged out: "+response.networkResponse().code());
                com.dar.nclientv2.settings.Login.logout();
                com.dar.nclientv2.settings.Login.updateUser(null);
            }
        });
    }
    private static void createCookieLogin(final LoginActivity activity, String username, String password, String token){
        RequestBody b=new FormBody.Builder()
                .add("csrfmiddlewaretoken",token)
                .add("username_or_email",username)
                .add("password",password)
                .build();
        Global.client.newCall(new Request.Builder().addHeader("Referer",login_url).url(login_url).post(b).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call,@NonNull IOException e) { }

            @Override
            public void onResponse(@NonNull Call call,@NonNull Response response) {
                Log.d(Global.LOGTAG,"Log in: "+response.networkResponse().code());
                if(com.dar.nclientv2.settings.Login.isLogged()) {
                    activity.finish();
                    User.createUser(user ->{
                        new LoadTags(null).start();
                        new DownloadFavorite(null).start();
                    } );
                }else activity.runOnUiThread(() -> activity.invalid.setVisibility(View.VISIBLE));
            }
        });
    }
}
