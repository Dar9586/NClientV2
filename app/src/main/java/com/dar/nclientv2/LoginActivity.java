package com.dar.nclientv2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dar.nclientv2.loginapi.Login;
import com.dar.nclientv2.settings.Global;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    public TextView invalid;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        Global.initHttpClient(this);
        setContentView(R.layout.activity_login);
        final EditText username=findViewById(R.id.username);
        final EditText password=findViewById(R.id.password);
        final Button button=findViewById(R.id.sign_in);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(Global.LOGTAG,username.getText().toString()+","+password.getText().toString());
                Login.login(LoginActivity.this,username.getText().toString(),password.getText().toString());

            }
        });
        findViewById(R.id.forgot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://nhentai.net/reset/"));
                startActivity(i);
            }
        });
        findViewById(R.id.register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://nhentai.net/register/"));
                startActivity(i);
            }
        });
        invalid=findViewById(R.id.invalid);
    }
}

