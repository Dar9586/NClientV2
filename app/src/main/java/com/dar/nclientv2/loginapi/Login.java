package com.dar.nclientv2.loginapi;

import android.view.View;

import com.dar.nclientv2.LoginActivity;

public class Login {
    public static void login(final LoginActivity activity, final String username, final String password) {
        activity.runOnUiThread(() -> activity.invalid.setVisibility(View.GONE));
        new LoginThread(true, code -> {
            if(com.dar.nclientv2.settings.Login.isLogged()){
                activity.runOnUiThread(activity::finish);
                User.createUser(user -> new LoadTags(null).start());
            } else {
                activity.runOnUiThread(() -> activity.invalid.setVisibility(View.VISIBLE));
            }
        }).setCredential(username,password).start();
    }
    public static void logout() {
        new LoginThread(false, code -> {
            com.dar.nclientv2.settings.Login.logout();
        }).start();
    }
}
