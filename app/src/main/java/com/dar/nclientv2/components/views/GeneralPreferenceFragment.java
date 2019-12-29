package com.dar.nclientv2.components.views;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.dar.nclientv2.R;
import com.dar.nclientv2.SettingsActivity;
import com.dar.nclientv2.async.VersionChecker;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;

public class GeneralPreferenceFragment extends PreferenceFragmentCompat {
    private Activity act;

    public void setAct(Activity act) {
        this.act = act;
    }

    public void setType(SettingsActivity.Type type) {
        switch (type){
            case MAIN:mainMenu();break;
            case COLUMN:columnMenu();break;
        }
    }

    private void mainMenu(){
        addPreferencesFromResource(R.xml.settings);
        findPreference("col_screen").setOnPreferenceClickListener(preference -> {
            Intent i=new Intent(act,SettingsActivity.class);
            i.putExtra(act.getPackageName()+".TYPE", SettingsActivity.Type.COLUMN.ordinal());
            act.runOnUiThread(() -> act.startActivity(i));
            return false;
        });

        findPreference(getString(R.string.key_use_account_tag)).setEnabled(Login.isLogged());
        findPreference(getString(R.string.key_theme_select)).setOnPreferenceChangeListener((preference, newValue) -> {
            act.recreate();
            return true;
        });
        findPreference(getString(R.string.key_language)).setOnPreferenceChangeListener((preference, newValue) -> {
            act.recreate();
            return true;
        });
        findPreference("version").setTitle(getString(R.string.app_version_format, Global.getVersionName(getContext())));
        double cacheSize=Global.recursiveSize(act.getCacheDir())/((double)(2<<20));

        findPreference(getString(R.string.key_cache)).setSummary(getString(R.string.cache_size_formatted,cacheSize));
        findPreference(getString(R.string.key_cache)).setOnPreferenceClickListener(preference -> {
            AlertDialog.Builder builder=new AlertDialog.Builder(act);
            builder.setTitle(R.string.clear_cache);
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                Global.recursiveDelete(act.getCacheDir());
                act.runOnUiThread(() -> {
                    Toast.makeText(act, act.getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show();
                    double cSize=Global.recursiveSize(act.getCacheDir())/((double)(2<<20));
                    findPreference(getString(R.string.key_cache)).setSummary(getString(R.string.cache_size_formatted,cSize));
                });
            }).setNegativeButton(R.string.no,null).setCancelable(true);
            builder.show();

            return true;
        });
        findPreference(getString(R.string.key_update)).setOnPreferenceClickListener(preference -> {
            new VersionChecker(act,false);
            return true;
        });
        findPreference("bug").setOnPreferenceClickListener(preference -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Dar9586/NClientV2/issues/new"));
            startActivity(i);
            return true;
        });
        findPreference(getString(R.string.key_favorite_limit)).setOnPreferenceClickListener(preference -> {
            DefaultDialogs.pageChangerDialog(
                    new DefaultDialogs.Builder(act)
                            .setDrawable(R.drawable.ic_hashtag)
                            .setTitle(R.string.favorite_count)
                            .setMax(20)
                            .setActual(Global.getFavoriteLimit(act))
                            .setMin(0)
                            .setDialogs(new DefaultDialogs.DialogResults() {
                                @Override
                                public void positive(int actual) {
                                    Global.updateFavoriteLimit(act,actual);
                                }

                                @Override public void negative() {}
                            })
            );
            return true;
        });
    }


    private Preference.OnPreferenceClickListener preferenceClickListener(int what){return preference -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        View v = View.inflate(act, R.layout.column_selector, null);
        builder.setView(v);
        TextView progL, progP;
        AppCompatSeekBar seekBarL, seekBarP;
        progL = v.findViewById(R.id.prog2);
        progP = v.findViewById(R.id.prog1);
        seekBarL = v.findViewById(R.id.seekBar2);
        seekBarP = v.findViewById(R.id.seekBar1);

        seekBarL.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progL.setText(String.valueOf(progress + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        seekBarP.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progP.setText(String.valueOf(progress + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        switch (what){
            case 1:
                seekBarL.setProgress(Global.getColLandMain() - 1);
                seekBarP.setProgress(Global.getColPortMain() - 1);
                break;
            case 2:
                seekBarL.setProgress(Global.getColPortDownload() - 1);
                seekBarP.setProgress(Global.getColPortDownload() - 1);
                break;
            case 4:
                seekBarL.setProgress(Global.getColPortFavorite() - 1);
                seekBarP.setProgress(Global.getColPortFavorite() - 1);
                break;
        }

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                    switch (what) {
                        case 1:
                            Global.updateMainColumnCount(act, seekBarP.getProgress() + 1, seekBarL.getProgress() + 1);
                            break;
                        case  2:
                            Global.updateDownloadColumnCount(act, seekBarP.getProgress() + 1, seekBarL.getProgress() + 1);
                            break;
                        case 4:
                            Global.updateFavoriteColumnCount(act, seekBarP.getProgress() + 1, seekBarL.getProgress() + 1);
                            break;
                    }
                }
        );

        builder.setNegativeButton(R.string.cancel, null);
        builder.setCancelable(true);
        builder.setTitle(R.string.select_column_count);
        builder.create().show();
        return true;
    };
    }


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey){
        getPreferenceManager().setSharedPreferencesName("Settings");
    }

    private void columnMenu() {
        addPreferencesFromResource(R.xml.settings_column);
        findPreference("col_main").setOnPreferenceClickListener(preferenceClickListener(1));
        findPreference("col_download").setOnPreferenceClickListener(preferenceClickListener(2));
        findPreference("col_favorite").setOnPreferenceClickListener(preferenceClickListener(4));
    }
}
