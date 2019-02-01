package com.dar.nclientv2.settings;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dar.nclientv2.R;

import java.util.Locale;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

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
            yesbtn=android.R.string.ok;
            nobtn=android.R.string.cancel;
            max=actual=1;
            min=0;
            dialogs=null;
        }

        public void setMin(int min){
            this.min = min;
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
        androidx.appcompat.app.AlertDialog.Builder build = new androidx.appcompat.app.AlertDialog.Builder(builder.context);
        if(builder.title!=0)build.setTitle(builder.context.getString(builder.title));
        if(builder.drawable!=0) build.setIcon(builder.drawable);
        View v=View.inflate(builder.context, R.layout.page_changer, null);
        build.setView(v);
        final SeekBar edt=v.findViewById(R.id.seekBar);
        final TextView pag=v.findViewById(R.id.page);
        final EditText editText=v.findViewById(R.id.edit_page);
        v.findViewById(R.id.prev).setOnClickListener(v12 -> {
            edt.setProgress(edt.getProgress()-1);
            editText.setText(String.format(Locale.US,"%d",edt.getProgress()+builder.min));

        });
        v.findViewById(R.id.next).setOnClickListener(v1 -> {
            edt.setProgress(edt.getProgress()+1);
            editText.setText(String.format(Locale.US,"%d",edt.getProgress()+builder.min));
        });
        edt.setMax(builder.max-builder.min);
        edt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser)editText.setText(String.format(Locale.US,"%d",progress+builder.min));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        editText.setText(String.format(Locale.US,"%d",builder.actual));
        edt.setProgress(builder.actual-builder.min-1);
        pag.setText(String.format(Locale.US,"/%d",builder.max));
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(Integer.toString(builder.max).length());
        editText.setFilters(filterArray);
        editText.addTextChangedListener(new TextWatcher() {
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
                if(x<builder.min)edt.setProgress(0);
                else edt.setProgress(x-builder.min);
            }
        });
        if(builder.dialogs!=null)
        build.setPositiveButton(builder.context.getString(builder.yesbtn), (dialog, id) -> builder.dialogs.positive(edt.getProgress()+builder.min)).setNegativeButton(builder.context.getString(builder.nobtn), (dialog, which) -> builder.dialogs.negative());
        build.setCancelable(true);
        build.show();
    }
}
