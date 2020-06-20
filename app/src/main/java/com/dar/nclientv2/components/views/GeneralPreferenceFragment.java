package com.dar.nclientv2.components.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.JsonWriter;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.dar.nclientv2.CopyToClipboardActivity;
import com.dar.nclientv2.FileChooserActivity;
import com.dar.nclientv2.PINActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.SettingsActivity;
import com.dar.nclientv2.async.VersionChecker;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.files.MasterFileManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class GeneralPreferenceFragment extends PreferenceFragmentCompat {
    private AppCompatActivity act;

    public void setAct(AppCompatActivity act) {
        this.act = act;
    }

    public void setType(SettingsActivity.Type type) {
        switch (type){
            case MAIN:mainMenu();break;
            case COLUMN:columnMenu();break;
            case DATA:dataMenu();
        }
    }

    private void dataMenu() {
        addPreferencesFromResource(R.xml.settings_data);
        SeekBarPreference mobile=findPreference(getString(R.string.key_mobile_usage));
        SeekBarPreference wifi=findPreference(getString(R.string.key_wifi_usage));
        mobile.setOnPreferenceChangeListener((preference, newValue) -> {
            mobile.setTitle(getDataUsageString((Integer) newValue));
            return true;
        });
        wifi.setOnPreferenceChangeListener((preference, newValue) -> {
            wifi.setTitle(getDataUsageString((Integer) newValue));
            return true;
        });
        mobile.setTitle(getDataUsageString(mobile.getValue()));
        wifi.setTitle(getDataUsageString(wifi.getValue()));
        mobile.setUpdatesContinuously(true);
        wifi.setUpdatesContinuously(true);
    }
    private int getDataUsageString(int val){
        switch (val){
            case 0:return R.string.data_usage_no;
            case 1:return R.string.data_usage_thumb;
            case 2:return R.string.data_usage_full;
        }
        return R.string.data_usage_full;
    }
    private void mainMenu(){
        addPreferencesFromResource(R.xml.settings);

        findPreference("col_screen").setOnPreferenceClickListener(preference -> {
            Intent i=new Intent(act,SettingsActivity.class);
            i.putExtra(act.getPackageName()+".TYPE", SettingsActivity.Type.COLUMN.ordinal());
            act.runOnUiThread(() -> act.startActivity(i));
            return false;
        });
        findPreference("data_screen").setOnPreferenceClickListener(preference -> {
            Intent i=new Intent(act,SettingsActivity.class);
            i.putExtra(act.getPackageName()+".TYPE", SettingsActivity.Type.DATA.ordinal());
            act.runOnUiThread(() -> act.startActivity(i));
            return false;
        });
        findPreference(getString(R.string.key_use_account_tag)).setEnabled(Login.isLogged());
        storageLocationManager(findPreference("storage_location"));
        findPreference(getString(R.string.key_theme_select)).setOnPreferenceChangeListener((preference, newValue) -> {
            act.recreate();
            return true;
        });
        findPreference(getString(R.string.key_language)).setOnPreferenceChangeListener((preference, newValue) -> {
            act.recreate();
            return true;
        });
        findPreference("has_pin").setOnPreferenceChangeListener((preference, newValue) -> {
            if(newValue.equals(Boolean.TRUE)) {
                Intent i = new Intent(act, PINActivity.class);
                i.putExtra(act.getPackageName() + ".SET", true);
                startActivity(i);
                act.finish();
                return false;
            }
            act.getSharedPreferences("Settings",Context.MODE_PRIVATE).edit().remove("pin").apply();
            return true;
        });

        findPreference("version").setTitle(getString(R.string.app_version_format, Global.getVersionName(getContext())));

        double cacheSize=Global.recursiveSize(MasterFileManager.getCacheDir(act))/((double)(1<<20));

        //clear cache if pressed
        findPreference(getString(R.string.key_cache)).setSummary(getString(R.string.cache_size_formatted,cacheSize));
        findPreference(getString(R.string.key_cache)).setOnPreferenceClickListener(preference -> {
            MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(act);
            builder.setTitle(R.string.clear_cache);
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                Global.recursiveDelete(MasterFileManager.getCacheDir(act));
                act.runOnUiThread(() -> {
                    Toast.makeText(act, act.getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show();
                    double cSize=Global.recursiveSize(MasterFileManager.getCacheDir(act))/((double)(2<<20));
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
        findPreference("donate").setOnPreferenceClickListener(preference -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Dar9586/NClientV2#donation"));
            startActivity(i);
            return true;
        });
        findPreference("copy_settings").setOnPreferenceClickListener(preference -> {
            try {
                CopyToClipboardActivity.copyTextToClipboard(getContext(),getDataSettings(getContext()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        });
    }


    private void storageLocationManager(Preference storage) {
        boolean shouldShow= Global.hasStoragePermission(getContext())&&
                (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP||
                        android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT);

        storage.setVisible(shouldShow);
        if(!shouldShow)return;
        storage.setSummary(MasterFileManager.getAbsolutePath(act));
        storage.setOnPreferenceClickListener(preference -> {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)showStorageModeSelector();
            else fileModeStorage();
            return false;
        });
    }

    private void fileModeStorage() {
        Intent intent=new Intent(act,FileChooserActivity.class);
        act.startActivityForResult(intent,SettingsActivity.RESPONSE_CHANGE_FOLDER);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void showStorageModeSelector() {
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(act);
        builder.setTitle(R.string.chose_staorage_mode);
        ArrayAdapter<String>adapter=new ArrayAdapter<>(act,android.R.layout.select_dialog_item);
        adapter.add(getString(R.string.file_mode_description));
        adapter.add(getString(R.string.document_mode_description));
        builder.setAdapter(adapter, (dialog, which) -> {
            switch (which) {
                case 0: fileModeStorage();break;
                case 1: grantSdAccess();break;
            }
        }).setNeutralButton(R.string.help, (dialog, which) -> showHelpDialog())
                .setNegativeButton(R.string.cancel,null);
        builder.show();
    }
    private void showHelpDialog(){
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(act);
        builder.setTitle(R.string.help)
                .setMessage(R.string.help_storage_message)
                .setPositiveButton(R.string.ok,null)
                .show();
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void grantSdAccess() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        act.startActivityForResult(intent,SettingsActivity.RESPONSE_CHANGE_FOLDER_TREE);
    }
    private String getDataSettings(Context context)throws IOException{
        String[]names=new String[]{"Settings","ScrapedTags"};
        StringWriter sw=new StringWriter();
        JsonWriter writer=new JsonWriter(sw);
        writer.setIndent("\t");

        writer.beginObject();
        for(String name:names)
            processSharedFromName(writer,context,name);
        writer.endObject();

        writer.flush();
        String settings=sw.toString();
        writer.close();

        LogUtility.d(settings);
        return settings;
    }
    private void processSharedFromName(JsonWriter writer, Context context, String name)throws IOException{
        writer.name(name);
        writer.beginObject();
        SharedPreferences preferences=context.getSharedPreferences(name,Context.MODE_PRIVATE);
        for(Map.Entry<String,?> entry: preferences.getAll().entrySet()){
            writeEntry(writer,entry);
        }
        writer.endObject();
    }
    private void writeEntry(JsonWriter writer, Map.Entry<String,?> entry)throws IOException {
        writer.name(entry.getKey());
             if(entry.getValue() instanceof Integer) writer.value((Integer)entry.getValue());
        else if(entry.getValue() instanceof Boolean) writer.value((Boolean)entry.getValue());
        else if(entry.getValue() instanceof String ) writer.value((String) entry.getValue());
        else if(entry.getValue() instanceof Long   ) writer.value((Long)   entry.getValue());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey){
        getPreferenceManager().setSharedPreferencesName("Settings");
    }

    private void columnMenu() {
        addPreferencesFromResource(R.xml.settings_column);
    }

}
