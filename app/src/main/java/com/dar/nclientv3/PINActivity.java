package com.dar.nclientv3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dar.nclientv3.components.activities.GeneralActivity;
import com.dar.nclientv3.settings.Global;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PINActivity extends GeneralActivity {
    private static final int PIN_LENGTH = 4;
    private List<TextView> texts;
    private TextView text;
    private String pin = "";
    private String confirmPin = null;
    private boolean setMode;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Global.initActivity(this);
        setContentView(R.layout.activity_pin);
        preferences = getSharedPreferences("Settings", 0);
        setMode = getIntent().getBooleanExtra(getPackageName() + ".SET", false);
        if (!setMode && hasNoPin()) {
            finish();
            return;
        }
        ImageView logo = findViewById(R.id.imageView);
        logo.setImageResource(Global.getTheme() == Global.ThemeScheme.LIGHT ? R.drawable.ic_logo_dark : R.drawable.ic_logo);
        LinearLayout linear = findViewById(R.id.linearLayout);
        text = findViewById(R.id.textView);
        ImageButton cancelButton = findViewById(R.id.cancelButton);
        texts = new ArrayList<>(PIN_LENGTH);
        for (int i = 0; i < PIN_LENGTH; i++) texts.add((TextView) linear.getChildAt(i));
        List<Button> numbers = Arrays.asList(
            findViewById(R.id.btn0), findViewById(R.id.btn1), findViewById(R.id.btn2),
            findViewById(R.id.btn3), findViewById(R.id.btn4), findViewById(R.id.btn5),
            findViewById(R.id.btn6), findViewById(R.id.btn7), findViewById(R.id.btn8),
            findViewById(R.id.btn9)
        );
        for (int i = 0; i < numbers.size(); i++) {
            final int ind = i;
            numbers.get(i).setOnClickListener(v -> {
                pin += ind;
                applyPinMask();
                if (pin.length() == 4)
                    checkPin();
            });
        }
        cancelButton.setOnClickListener(v -> {
            if (pin.isEmpty()) return;
            pin = pin.substring(0, pin.length() - 1);
            applyPinMask();
        });
        cancelButton.setOnLongClickListener(v -> {
            pin = "";
            applyPinMask();
            return true;
        });
        ImageButton utility = findViewById(R.id.utility);
        utility.setOnClickListener(v -> {
            if (setMode && isConfirming()) {
                checkPin();//will go in wrong branch
            } else {
                finish();
            }
        });

    }

    private boolean isConfirming() {
        return confirmPin != null;
    }

    private boolean hasNoPin() {
        return !preferences.getBoolean("has_pin", false);
    }

    private void setPin(String pin) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("has_pin", true);
        editor.putString("pin", pin);
        editor.apply();
    }

    @Override
    public void finish() {
        Intent i = new Intent(this, setMode ? SettingsActivity.class : MainActivity.class);
        if (setMode || hasNoPin() || pin.equals(getTruePin())) startActivity(i);
        super.finish();
    }

    private String getTruePin() {
        return preferences.getString("pin", null);
    }

    private void checkPin() {
        if (setMode) {//if password should be set
            if (!isConfirming()) {//now password must be confirmed
                confirmPin = pin;
                pin = "";
                text.setText(R.string.confirm_pin);
            } else if (confirmPin.equals(pin)) {//password confirmed
                setPin(confirmPin);
                finish();
            } else {//wrong confirmed password
                confirmPin = null;
                text.setText(R.string.insert_pin);
                pin = "";
            }
        } else if (pin.equals(getTruePin())) {//right password
            finish();
        } else {//wrong password
            text.setText(R.string.wrong_pin);
            pin = "";
        }
        applyPinMask();
    }

    private void applyPinMask() {
        int i, len = Math.min(pin.length(), PIN_LENGTH);
        for (i = 0; i < len; i++) texts.get(i).setText(R.string.full_circle);
        for (; i < PIN_LENGTH; i++) texts.get(i).setText(R.string.empty_circle);
    }
}
