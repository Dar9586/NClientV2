package com.dar.nclientv2;

import android.content.DialogInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
            findPreference(getString(R.string.key_hide_saved_images)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Global.saveNoMedia(getActivity());
                    MediaScannerConnection.scanFile(getActivity().getApplicationContext(), Global.GALLERYFOLDER.list(), new String[]{"image/jpeg"}, new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.d(Global.LOGTAG,path);
                        }
                    });

                    return false;
                }
            });
            findPreference(getString(R.string.key_theme_select)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    act.recreate();
                    return true;
                }
            });
            findPreference(getString(R.string.key_cache)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.clear_cache);
                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Global.recursiveDelete(getActivity().getCacheDir());
                        }
                    }).setNegativeButton(R.string.no,null).setCancelable(true);
                    builder.show();

                    return false;
                }
            });
            setHasOptionsMenu(true);
        }
    }
}
