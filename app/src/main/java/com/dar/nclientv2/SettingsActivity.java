package com.dar.nclientv2;

import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.dar.nclientv2.settings.Global;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        setContentView(R.layout.activity_settings);
        GeneralPreferenceFragment.act=this;
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new GeneralPreferenceFragment()).commit();
    }
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        static SettingsActivity act;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final PreferenceManager prefMgr = getPreferenceManager();
            prefMgr.setSharedPreferencesName("Settings");
            addPreferencesFromResource(R.xml.settings);

            findPreference(getString(R.string.key_theme_select)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    act.recreate();
                    return true;
                }
            });
            setHasOptionsMenu(true);
        }
    }
}
