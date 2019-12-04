package com.dar.nclientv2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.dar.nclientv2.async.VersionChecker;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadThemeAndLanguage(this);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        GeneralPreferenceFragment.act=this;
        getSupportFragmentManager().beginTransaction().replace(R.id.view,
                new GeneralPreferenceFragment()).commit();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat{
        public static SettingsActivity act;


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey){
            getPreferenceManager().setSharedPreferencesName("Settings");
            addPreferencesFromResource(R.xml.settings);
            findPreference(getString(R.string.key_column_count_general)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    View v=View.inflate(getActivity(), R.layout.column_selector, null);
                    builder.setView(v);
                    TextView progL,progP;
                    AppCompatSeekBar seekBarL,seekBarP;
                    progL=v.findViewById(R.id.textViewLandscape);
                    progP=v.findViewById(R.id.textViewPortait);
                    seekBarL=v.findViewById(R.id.seekBarLandscape);
                    seekBarP=v.findViewById(R.id.seekBarPortait);

                    seekBarL.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            progL.setText(""+(progress+1));
                        }
                        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                    });
                    seekBarP.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            progP.setText(""+(progress+1));
                        }
                        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                    });
                    seekBarL.setProgress(Global.landColumnCount()-1);
                    seekBarP.setProgress(Global.portColumnCount()-1);
                    builder.setPositiveButton(R.string.ok, (dialog, which) -> Global.updateMainColumnCount(getActivity(),seekBarP.getProgress()+1,seekBarL.getProgress()+1));
                    builder.setNegativeButton(R.string.cancel,null);
                    builder.setCancelable(true);
                    builder.setTitle(R.string.select_column_count);
                    builder.create().show();
                    return true;
                }
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
            findPreference("version").setTitle(getString(R.string.app_version_format,Global.getVersionName(getContext())));
            double cacheSize=Global.recursiveSize(getActivity().getCacheDir())/((double)(2<<20));

            findPreference(getString(R.string.key_cache)).setSummary(getString(R.string.cache_size_formatted,cacheSize));
            findPreference(getString(R.string.key_cache)).setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.clear_cache);
                builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                    Global.recursiveDelete(getActivity().getCacheDir());
                    act.runOnUiThread(() -> {
                        Toast.makeText(act, act.getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show();
                        double cSize=Global.recursiveSize(getActivity().getCacheDir())/((double)(2<<20));
                        findPreference(getString(R.string.key_cache)).setSummary(getString(R.string.cache_size_formatted,cSize));
                    });
                }).setNegativeButton(R.string.no,null).setCancelable(true);
                builder.show();

                return true;
            });
            findPreference(getString(R.string.key_update)).setOnPreferenceClickListener(preference -> {
                new VersionChecker(GeneralPreferenceFragment.this.getActivity(),false);
                return true;
            });
            findPreference("bug").setOnPreferenceClickListener(preference -> {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Dar9586/NClientV2/issues/new"));
                startActivity(i);
                return true;
            });
            findPreference(getString(R.string.key_favorite_limit)).setOnPreferenceClickListener(preference -> {
                DefaultDialogs.pageChangerDialog(
                        new DefaultDialogs.Builder(getActivity())
                                .setDrawable(R.drawable.ic_hashtag)
                                .setTitle(R.string.favorite_count)
                                .setMax(20)
                                .setActual(Global.getFavoriteLimit(getActivity()))
                                .setMin(0)
                                .setDialogs(new DefaultDialogs.DialogResults() {
                                    @Override
                                    public void positive(int actual) {
                                        Global.updateFavoriteLimit(getActivity(),actual);
                                    }

                                    @Override public void negative() {}
                                })
                );
                return true;
            });
        }
    }

}
