package com.dar.nclientv2;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.dar.nclientv2.async.VersionChecker;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        Global.initHideFromGallery(this);
        Global.initHttpClient(this);
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
            findPreference(getString(R.string.key_hide_saved_images)).setOnPreferenceClickListener(preference -> {
                if(Global.hasStoragePermission(getActivity())) {
                    Global.saveNoMedia(GeneralPreferenceFragment.this.getActivity());
                    if (!((SwitchPreference) preference).isChecked()) galleryAddPics();
                    else removePic();
                }
                return true;
            });
            findPreference(getString(R.string.key_theme_select)).setOnPreferenceChangeListener((preference, newValue) -> {
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
            findPreference(getString(R.string.key_image_quality)).setOnPreferenceClickListener(preference -> {
                DefaultDialogs.pageChangerDialog(
                        new DefaultDialogs.Builder(getActivity())
                                .setDrawable(R.drawable.ic_image)
                                .setTitle(R.string.image_quality)
                                .setMax(100)
                                .setActual(Global.initImageQuality(getActivity()))
                                .setDialogs(new DefaultDialogs.DialogResults() {
                                    @Override
                                    public void positive(int actual) {
                                        Log.d(Global.LOGTAG,"progress: "+actual);
                                        Global.updateImageQuality(getActivity(),actual);
                                    }

                                    @Override public void negative() {}
                                })
                );
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

        private void removePic(){
            Log.i(Global.LOGTAG,"Removing");
            String[] retCol = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA };
            Cursor cur = getActivity().getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    retCol,
                    MediaStore.MediaColumns.DATA+" LIKE  '"+Global.GALLERYFOLDER.getAbsolutePath()+"%'", null, null
            );
            Log.i(Global.LOGTAG,"Count: "+cur.getCount());
            if (cur.getCount() == 0) {

                return;
            }
            while(cur.moveToNext()){
                Log.i(Global.LOGTAG,"DATA: "+cur.getString(1));
                deleteId(cur.getString(1),cur.getInt(0));
            }
            cur.close();
        }
        private void deleteId(String file,int id){
            try{
                File f = new File(file);
                File dest=File.createTempFile("temp",".jpg");
                copyFile(f,dest);
                Uri uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                );
                getActivity().getContentResolver().delete(uri, null, null);
                copyFile(dest,f);
                dest.delete();
            } catch (IOException e) {
                Log.e(Global.LOGTAG, e.getLocalizedMessage(), e);
            }

        }
        private void copyFile(File source,File dest) throws IOException{
            try (FileChannel sourceChannel = new FileInputStream(source).getChannel(); FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
                destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            }
        }

        private void galleryAddPics() {
            Log.i(Global.LOGTAG,"Adding");
            for(File file:Global.GALLERYFOLDER.listFiles((dir, name) -> name.endsWith(".jpg")||name.endsWith(".png")||name.endsWith(".gif")))
                Global.addToGallery(getActivity(),file);

        }
    }

}
