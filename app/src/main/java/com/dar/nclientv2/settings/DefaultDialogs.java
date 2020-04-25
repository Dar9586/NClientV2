package com.dar.nclientv2.settings;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.dar.nclientv2.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class DefaultDialogs {
    public interface DialogResults{
        void positive(int actual);
        void negative();
    }
    @SuppressWarnings("UnusedReturnValue")
    public static class Builder{
        private final Context context;
        private @StringRes int title,yesbtn,nobtn;
        private @DrawableRes int drawable;
        private int max,actual,min;
        DialogResults dialogs;

        public Builder(Context context) {
            this.context = context;
            title=drawable=0;
            yesbtn=R.string.ok;
            nobtn=R.string.cancel;
            max=actual=1;
            min=0;
            dialogs=null;
        }

        public Builder setMin(int min){
            this.min = min;
            return this;
        }

        public Builder setTitle(int title) {
            this.title = title;
            return this;
        }

        public Builder setYesbtn(int yesbtn) {
            this.yesbtn = yesbtn;
            return this;
        }

        public Builder setNobtn(int nobtn) {
            this.nobtn = nobtn;
            return this;
        }

        public Builder setDrawable(int drawable) {
            this.drawable = drawable;
            return this;
        }

        public Builder setMax(int max) {
            this.max = max;
            return this;
        }

        public Builder setActual(int actual) {
            this.actual = actual;
            return this;
        }

        public Builder setDialogs(DialogResults dialogs) {
            this.dialogs = dialogs;
            return this;
        }
    }
    public static void pageChangerDialog(final Builder builder){
        MaterialAlertDialogBuilder build = new MaterialAlertDialogBuilder(builder.context);
        if(builder.title!=0)build.setTitle(builder.context.getString(builder.title));
        if(builder.drawable!=0) build.setIcon(builder.drawable);
        View v=View.inflate(builder.context, R.layout.page_changer, null);
        build.setView(v);
        final SeekBar seekBar=v.findViewById(R.id.seekBar);
        if(Global.useRtl())seekBar.setRotationY(180);
        final TextView totalPage=v.findViewById(R.id.page);
        final EditText actualPage=v.findViewById(R.id.edit_page);
        v.findViewById(R.id.prev).setOnClickListener(v12 -> {
            seekBar.setProgress(seekBar.getProgress()-1);
            actualPage.setText(String.format(Locale.US,"%d",seekBar.getProgress()+builder.min));

        });
        v.findViewById(R.id.next).setOnClickListener(v1 -> {
            seekBar.setProgress(seekBar.getProgress()+1);
            actualPage.setText(String.format(Locale.US,"%d",seekBar.getProgress()+builder.min));
        });
        seekBar.setMax(builder.max-builder.min);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser)actualPage.setText(String.format(Locale.US,"%d",progress+builder.min));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        actualPage.setText(String.format(Locale.US,"%d",builder.actual));
        seekBar.setProgress(builder.actual-builder.min);
        totalPage.setText(String.format(Locale.US,"%d",builder.max));
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(Integer.toString(builder.max).length());
        actualPage.setFilters(filterArray);
        actualPage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                int x;
                try {
                    x = Integer.parseInt(s.toString());
                }catch (NumberFormatException e){x=-1;}
                if(x<builder.min)seekBar.setProgress(0);
                else seekBar.setProgress(x-builder.min);
            }
        });
        if(builder.dialogs!=null)
        build.setPositiveButton(builder.context.getString(builder.yesbtn), (dialog, id) -> builder.dialogs.positive(seekBar.getProgress()+builder.min)).setNegativeButton(builder.context.getString(builder.nobtn), (dialog, which) -> builder.dialogs.negative());
        build.setCancelable(true);
        build.show();
    }
}
