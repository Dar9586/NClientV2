package com.dar.nclientv2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.loginapi.Login;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.Utility;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    public TextView invalid;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.activity_login);
        final EditText username=findViewById(R.id.username);
        final EditText password=findViewById(R.id.password);
        final Button button=findViewById(R.id.sign_in);
        final Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.title_activity_login);
        assert getSupportActionBar()!=null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        button.setOnClickListener(view -> {
            //LogUtility.d(username.getText().toString()+","+password.getText().toString());
            Login.login(LoginActivity.this,username.getText().toString(),password.getText().toString());

        });
        findViewById(R.id.forgot).setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://"+ Utility.getHost()+"/reset/"));
            startActivity(i);
        });
        findViewById(R.id.register).setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(Utility.getBaseUrl()+"register/"));
            startActivity(i);
        });

        password.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if(event.getAction() == MotionEvent.ACTION_UP) {
                if(event.getRawX() >= (password.getRight() - password.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    password.setInputType(password.getInputType()^InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    return true;
                }
            }
            return false;
        });
        password.setOnKeyListener((v, keyCode, event) -> {
            if(event.getAction()==KeyEvent.ACTION_DOWN&&keyCode==KeyEvent.KEYCODE_ENTER){
                Login.login(LoginActivity.this,username.getText().toString(),password.getText().toString());
                return true;
            }
            return false;
        });
        invalid=findViewById(R.id.invalid);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==android.R.id.home)finish();
        return super.onOptionsItemSelected(item);
    }
}

