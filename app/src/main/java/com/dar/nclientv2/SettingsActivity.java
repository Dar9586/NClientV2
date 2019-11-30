package com.dar.nclientv2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
        GeneralPreferenceFragment.act=this;
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
                new GeneralPreferenceFragment()).commit();
    }
    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat{
        public static SettingsActivity act;


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey){
            getPreferenceManager().setSharedPreferencesName("Settings");
            addPreferencesFromResource(R.xml.settings);
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
                builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    Global.recursiveDelete(getActivity().getCacheDir());
                    act.runOnUiThread(() -> {
                        Toast.makeText(act, act.getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show();
                        double cSize=Global.recursiveSize(getActivity().getCacheDir())/((double)(2<<20));
                        findPreference(getString(R.string.key_cache)).setSummary(getString(R.string.cache_size_formatted,cSize));
                    });
                }).setNegativeButton(android.R.string.no,null).setCancelable(true);
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
