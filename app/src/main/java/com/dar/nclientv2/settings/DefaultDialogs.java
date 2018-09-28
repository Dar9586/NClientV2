package com.dar.nclientv2.settings;

import android.content.Context;
import android.content.DialogInterface;
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
    public static class Builder{
        private final Context context;
        private @StringRes int title,yesbtn,nobtn;
        private @DrawableRes int drawable;
        private int max,actual;
        DialogResults dialogs;

        public Builder(Context context) {
            this.context = context;
            title=drawable=0;
            yesbtn=android.R.string.ok;
            nobtn=android.R.string.cancel;
            max=actual=1;
            dialogs=null;
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
        v.findViewById(R.id.prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edt.setProgress(edt.getProgress()-1);
                editText.setText(String.format(Locale.US,"%d",edt.getProgress()+1));

            }
        });
        v.findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edt.setProgress(edt.getProgress()+1);
                editText.setText(String.format(Locale.US,"%d",edt.getProgress()+1));
            }
        });
        edt.setMax(builder.max-1);
        edt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser)editText.setText(String.format(Locale.US,"%d",progress+1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        editText.setText(String.format(Locale.US,"%d",builder.actual));
        edt.setProgress(builder.actual-1);
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
                }catch (NumberFormatException e){x=0;}
                if(x<1)edt.setProgress(0);
                else edt.setProgress(x-1);
            }
        });
        if(builder.dialogs!=null)
        build.setPositiveButton(builder.context.getString(builder.yesbtn), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                builder.dialogs.positive(edt.getProgress()+1);
            }
        }).setNegativeButton(builder.context.getString(builder.nobtn), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                builder.dialogs.negative();
            }
        });
        build.setCancelable(true);
        build.show();
    }
}
