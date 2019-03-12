package com.dar.nclientv2.loginapi;

import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class User {
    private final String username,codename;
    private final int id;
    private int totalPages=0;
    public interface CreateUser{
        void onCreateUser(User user);
    }
    public static void createUser(final CreateUser createUser){
        Global.client.newCall(new Request.Builder().url("https://nhentai.net/").build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                User user=null;
                Document doc= Jsoup.parse(response.body().byteStream(),null,"https://nhentai.net/");
                Elements elements=doc.getElementsByClass("fa-tachometer");
                if(elements.size()>0) {
                    Element x = elements.first().parent();
                    String username = x.text().trim();
                    String[] y = x.attr("href").split("/");
                    user = new User(username, y[2], y[3]);
                }
                Login.updateUser(user);
                if(createUser!=null)createUser.onCreateUser(Login.getUser());
            }
        });
    }
    private User(String username, String id, String codename) {
        this.username = username;
        this.id = Integer.parseInt(id);
        this.codename = codename;
    }

    @Override
    public String toString() {
        return username+'('+id+'/'+codename+')';
    }

    public String getUsername() {
        return username;
    }

    public int getId() {
        return id;
    }

    public String getCodename() {
        return codename;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalPages() {
        return totalPages;
    }
    public int getUsefulPageCount(){
        return totalPages==0?1:totalPages;
    }
}
