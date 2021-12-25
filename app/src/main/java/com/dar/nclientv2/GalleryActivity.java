package com.dar.nclientv2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.dar.nclientv2.adapters.GalleryAdapter;
import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.components.status.Status;
import com.dar.nclientv2.components.status.StatusManager;
import com.dar.nclientv2.components.views.RangeSelector;
import com.dar.nclientv2.components.widgets.CustomGridLayoutManager;
import com.dar.nclientv2.settings.AuthRequest;
import com.dar.nclientv2.settings.Favorites;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import yuku.ambilwarna.AmbilWarnaDialog;

public class GalleryActivity extends BaseActivity {
    @NonNull
    private GenericGallery gallery = Gallery.emptyGallery();
    private boolean isLocal;
    private GalleryAdapter adapter;
    private int zoom;
    private boolean isLocalFavorite;
    private Toolbar toolbar;
    private MenuItem onlineFavoriteItem;
    private String statusString;

    private int newStatusColor;
    private String newStatusName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Global.initActivity(this);
        setContentView(R.layout.activity_gallery);
        if (Global.isLockScreen())
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        recycler = findViewById(R.id.recycler);
        refresher = findViewById(R.id.refresher);
        masterLayout = findViewById(R.id.master_layout);
        GenericGallery gal = getIntent().getParcelableExtra(getPackageName() + ".GALLERY");
        if (gal == null && !tryLoadFromURL()) {
            finish();
            return;
        }
        if (gal != null) this.gallery = gal;
        if (gallery.getType() != GenericGallery.Type.LOCAL) {
            Queries.HistoryTable.addGallery(((Gallery) gallery).toSimpleGallery());
        }
        LogUtility.d("" + gallery);
        if (Global.useRtl()) recycler.setRotationY(180);
        isLocal = getIntent().getBooleanExtra(getPackageName() + ".ISLOCAL", false);
        zoom = getIntent().getIntExtra(getPackageName() + ".ZOOM", 0);
        refresher.setEnabled(false);
        recycler.setLayoutManager(new CustomGridLayoutManager(this, Global.getColumnCount()));

        loadGallery(gallery, zoom);//if already has gallery
    }

    private boolean tryLoadFromURL() {
        Uri data = getIntent().getData();
        if (data != null && data.getPathSegments().size() >= 2) {//if using an URL
            List<String> params = data.getPathSegments();
            LogUtility.d(params.size() + ": " + params);
            int id;
            try {//if not an id return
                id = Integer.parseInt(params.get(1));
            } catch (NumberFormatException ignore) {
                return false;
            }
            if (params.size() > 2) {//check if it has a specific page
                try {
                    zoom = Integer.parseInt(params.get(2));
                } catch (NumberFormatException e) {
                    LogUtility.e(e.getLocalizedMessage(), e);
                    zoom = 0;
                }
            }
            InspectorV3.galleryInspector(this, id, new InspectorV3.DefaultInspectorResponse() {
                @Override
                public void onSuccess(List<GenericGallery> galleries) {
                    if (galleries.size() > 0) {
                        Intent intent = new Intent(GalleryActivity.this, GalleryActivity.class);
                        intent.putExtra(getPackageName() + ".GALLERY", galleries.get(0));
                        intent.putExtra(getPackageName() + ".ZOOM", zoom);
                        startActivity(intent);
                    }
                    finish();
                }
            }).start();
            return true;
        }
        return false;
    }

    private void lookup() {
        CustomGridLayoutManager manager = (CustomGridLayoutManager) recycler.getLayoutManager();
        GalleryAdapter adapter = (GalleryAdapter) recycler.getAdapter();
        manager.setSpanSizeLookup(new CustomGridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.positionToType(position) == GalleryAdapter.Type.PAGE ? 1 : manager.getSpanCount();
            }
        });
    }

    private void loadGallery(GenericGallery gall, int zoom) {
        this.gallery = gall;
        if (getSupportActionBar() != null) {
            applyTitle();
        }
        adapter = new GalleryAdapter(this, gallery, Global.getColumnCount());
        recycler.setAdapter(adapter);
        lookup();
        if (zoom > 0 && Global.getDownloadPolicy() != Global.DataUsageType.NONE) {
            Intent intent = new Intent(this, ZoomActivity.class);
            intent.putExtra(getPackageName() + ".GALLERY", this.gallery);
            intent.putExtra(getPackageName() + ".DIRECTORY", adapter.getDirectory());
            intent.putExtra(getPackageName() + ".PAGE", zoom);
            startActivity(intent);
        }
        checkBookmark();
    }

    private void checkBookmark() {
        int page = Queries.ResumeTable.pageFromId(gallery.getId());
        if (page < 0) return;
        Snackbar snack = Snackbar.make(toolbar, getString(R.string.resume_from_page, page), Snackbar.LENGTH_LONG);
        //Should be already compensated
        snack.setAction(R.string.resume, v -> new Thread(() -> {
            runOnUiThread(() -> recycler.scrollToPosition(page));
            if (Global.getColumnCount() != 1) return;
            Utility.threadSleep(500);
            runOnUiThread(() -> recycler.scrollToPosition(page));
        }).start());
        snack.show();
    }

    private void applyTitle() {
        CollapsingToolbarLayout collapsing = findViewById(R.id.collapsing);
        ActionBar actionBar = getSupportActionBar();
        final String title = gallery.getTitle();
        if (collapsing == null || actionBar == null) return;
        View.OnLongClickListener listener = v -> {
            CopyToClipboardActivity.copyTextToClipboard(GalleryActivity.this, title);
            GalleryActivity.this.runOnUiThread(
                () -> Toast.makeText(GalleryActivity.this, R.string.title_copied_to_clipboard, Toast.LENGTH_SHORT).show()
            );
            return true;
        };

        collapsing.setOnLongClickListener(listener);
        findViewById(R.id.toolbar).setOnLongClickListener(listener);
        if (title.length() > 100) {
            collapsing.setExpandedTitleTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
            collapsing.setMaxLines(5);
        } else {
            collapsing.setExpandedTitleTextAppearance(android.R.style.TextAppearance_DeviceDefault_Large);
            collapsing.setMaxLines(4);
        }
        actionBar.setTitle(title);

    }

    @Override
    protected int getPortraitColumnCount() {
        return 0;
    }

    @Override
    protected int getLandscapeColumnCount() {
        return 0;
    }


    public void initFavoriteIcon(Menu menu) {
        boolean onlineFavorite = !isLocal && ((Gallery) gallery).isOnlineFavorite();
        boolean unknown = getIntent().getBooleanExtra(getPackageName() + ".UNKNOWN", false);
        MenuItem item = menu.findItem(R.id.add_online_gallery);

        item.setIcon(onlineFavorite ? R.drawable.ic_star : R.drawable.ic_star_border);

        if (unknown) item.setTitle(R.string.toggle_online_favorite);
        else if (onlineFavorite) item.setTitle(R.string.remove_from_online_favorites);
        else item.setTitle(R.string.add_to_online_favorite);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery, menu);
        isLocalFavorite = Favorites.isFavorite(gallery);

        menu.findItem(R.id.favorite_manager).setIcon(isLocalFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        menuItemsVisible(menu);
        initFavoriteIcon(menu);
        Utility.tintMenu(menu);
        updateColumnCount(false);
        return true;
    }

    private void menuItemsVisible(Menu menu) {
        boolean isLogged = Login.isLogged();
        boolean isValidOnline = gallery.isValid() && !isLocal;
        onlineFavoriteItem = menu.findItem(R.id.add_online_gallery);
        onlineFavoriteItem.setVisible(isValidOnline && isLogged);
        menu.findItem(R.id.favorite_manager).setVisible(isValidOnline);
        menu.findItem(R.id.download_gallery).setVisible(isValidOnline);
        menu.findItem(R.id.related).setVisible(isValidOnline);
        menu.findItem(R.id.comments).setVisible(isValidOnline);
        menu.findItem(R.id.download_torrent).setVisible(isLogged);

        menu.findItem(R.id.share).setVisible(gallery.isValid());
        menu.findItem(R.id.load_internet).setVisible(isLocal && gallery.isValid());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateColumnCount(false);
        if (isLocal) supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.download_gallery) {
            if (Global.hasStoragePermission(this))
                new RangeSelector(this, (Gallery) gallery).show();
            else
                requestStorage();
        } else if (id == R.id.add_online_gallery) addToFavorite(item);
        else if (id == R.id.change_view) updateColumnCount(true);
        else if (id == R.id.download_torrent) downloadTorrent();
        else if (id == R.id.load_internet) toInternet();
        else if (id == R.id.manage_status) updateStatus();
        else if (id == R.id.share) Global.shareGallery(this, gallery);
        else if (id == R.id.comments) {
            Intent i = new Intent(this, CommentActivity.class);
            i.putExtra(getPackageName() + ".GALLERYID", gallery.getId());
            startActivity(i);
        } else if (id == R.id.related) {
            recycler.smoothScrollToPosition(recycler.getAdapter().getItemCount());
        } else if (id == R.id.favorite_manager) {
            if (isLocalFavorite) {
                if (Favorites.removeFavorite(gallery)) isLocalFavorite = !isLocalFavorite;
            } else if (Favorites.addFavorite((Gallery) gallery)) {
                isLocalFavorite = !isLocalFavorite;
            }
            item.setIcon(isLocalFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            Global.setTint(item.getIcon());
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void downloadTorrent() {
        if(!Global.hasStoragePermission(this)){
            return;
        }

        String url = String.format(Locale.US, Utility.getBaseUrl() + "g/%d/download", gallery.getId());
        String referer = String.format(Locale.US, Utility.getBaseUrl() + "g/%d/", gallery.getId());

        new AuthRequest(referer, url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call,@NonNull  IOException e) {
                GalleryActivity.this.runOnUiThread(() ->
                    Toast.makeText(GalleryActivity.this, R.string.failed, Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call,@NonNull Response response) throws IOException {
                File file=new File(Global.TORRENTFOLDER,gallery.getId()+".torrent");
                Utility.writeStreamToFile(response.body().byteStream(), file);
                Intent intent=new Intent(Intent.ACTION_VIEW);
                Uri torrentUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    torrentUri = FileProvider.getUriForFile(GalleryActivity.this, GalleryActivity.this.getPackageName() + ".provider", file);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }else{
                    torrentUri=Uri.fromFile(file);
                }
                intent.setDataAndType(torrentUri, "application/x-bittorrent");
                GalleryActivity.this.startActivity(intent);
                file.deleteOnExit();
            }
        }).setMethod("GET",null).start();
    }

    private void updateStatus() {
        List<String> statuses = StatusManager.getNames();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        statusString = Queries.StatusMangaTable.getStatus(gallery.getId()).name;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, statuses) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                CheckedTextView textView = (CheckedTextView) super.getView(position, convertView, parent);
                textView.setTextColor(StatusManager.getByName(statuses.get(position)).opaqueColor());
                return textView;
            }
        };
        builder.setSingleChoiceItems(adapter, statuses.indexOf(statusString), (dialog, which) -> statusString = statuses.get(which));
        builder
            .setNeutralButton(R.string.add, (dialog, which) -> createNewStatusDialog())
            .setNegativeButton(R.string.remove_status, (dialog, which) -> Queries.StatusMangaTable.remove(gallery.getId()))
            .setPositiveButton(R.string.ok, (dialog, which) -> Queries.StatusMangaTable.insert(gallery, statusString))
            .setTitle(R.string.change_status_title)
            .show();
    }

    private void createNewStatusDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LinearLayout layout = (LinearLayout) View.inflate(this, R.layout.dialog_add_status, null);
        EditText name = layout.findViewById(R.id.name);
        Button btnColor = layout.findViewById(R.id.color);
        do {
            newStatusColor = Utility.RANDOM.nextInt() | 0xff000000;
        } while (newStatusColor == Color.BLACK || newStatusColor == Color.WHITE);
        btnColor.setBackgroundColor(newStatusColor);
        btnColor.setOnClickListener(v -> new AmbilWarnaDialog(GalleryActivity.this, newStatusColor, false, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                if (color == Color.WHITE || color == Color.BLACK) {
                    Toast.makeText(GalleryActivity.this, R.string.invalid_color_selected, Toast.LENGTH_SHORT).show();
                    return;
                }
                newStatusColor = color;
                btnColor.setBackgroundColor(color);
            }
        }).show());
        builder.setView(layout);
        builder.setTitle(R.string.create_new_status);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String newName = name.getText().toString();
            if (newName.length() < 2) {
                Toast.makeText(this, R.string.name_too_short, Toast.LENGTH_SHORT).show();
                return;
            }
            if (StatusManager.getByName(newName) != null) {
                Toast.makeText(this, R.string.duplicated_name, Toast.LENGTH_SHORT).show();
                return;
            }
            Status status = StatusManager.add(name.getText().toString(), newStatusColor);
            Queries.StatusMangaTable.insert(gallery, status);
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> updateStatus());
        builder.setOnCancelListener(dialog -> updateStatus());
        builder.show();
    }

    private void updateIcon(boolean nowIsFavorite) {
        GalleryActivity.this.runOnUiThread(() -> {
            onlineFavoriteItem.setIcon(!nowIsFavorite ? R.drawable.ic_star_border : R.drawable.ic_star);
            onlineFavoriteItem.setTitle(!nowIsFavorite ? R.string.add_to_online_favorite : R.string.remove_from_online_favorites);
        });
    }

    private void addToFavorite(final MenuItem item) {

        boolean wasFavorite = onlineFavoriteItem.getTitle().equals(getString(R.string.remove_from_online_favorites));
        String url = String.format(Locale.US, Utility.getBaseUrl() + "api/gallery/%d/%sfavorite", gallery.getId(), wasFavorite ? "un" : "");
        String galleryUrl = String.format(Locale.US, Utility.getBaseUrl() + "g/%d/", gallery.getId());
        LogUtility.d("Calling: " + url);
        new AuthRequest(galleryUrl, url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                assert response.body() != null;
                String responseString = response.body().string();
                boolean nowIsFavorite = responseString.contains("true");
                updateIcon(nowIsFavorite);
            }
        }).setMethod("POST", AuthRequest.EMPTY_BODY).start();
    }

    private void updateColumnCount(boolean increase) {
        int x = Global.getColumnCount();
        CustomGridLayoutManager manager = (CustomGridLayoutManager) recycler.getLayoutManager();
        if (manager == null) return;
        MenuItem item = ((Toolbar) findViewById(R.id.toolbar)).getMenu().findItem(R.id.change_view);
        if (increase || manager.getSpanCount() != x) {
            if (increase) x = x % 4 + 1;
            int pos = manager.findFirstVisibleItemPosition();
            Global.updateColumnCount(this, x);

            recycler.setLayoutManager(new CustomGridLayoutManager(this, x));
            LogUtility.d("Span count: " + manager.getSpanCount());
            if (adapter != null) {
                adapter.setColCount(Global.getColumnCount());
                recycler.setAdapter(adapter);
                lookup();
                recycler.scrollToPosition(pos);
                adapter.setMaxImageSize(null);

            }
        }

        if (item != null) {
            switch (x) {
                case 1:
                    item.setIcon(R.drawable.ic_view_1);
                    break;
                case 2:
                    item.setIcon(R.drawable.ic_view_2);
                    break;
                case 3:
                    item.setIcon(R.drawable.ic_view_3);
                    break;
                case 4:
                    item.setIcon(R.drawable.ic_view_4);
                    break;
            }
            Global.setTint(item.getIcon());

        }
    }

    private void toInternet() {
        refresher.setEnabled(true);
        InspectorV3.galleryInspector(this, gallery.getId(), new InspectorV3.DefaultInspectorResponse() {
            @Override
            public void onSuccess(List<GenericGallery> galleries) {
                if (galleries.size() == 0) return;
                Intent intent = new Intent(GalleryActivity.this, GalleryActivity.class);
                LogUtility.d(galleries.get(0).toString());
                intent.putExtra(getPackageName() + ".GALLERY", galleries.get(0));
                runOnUiThread(() -> startActivity(intent));
            }
        }).start();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestStorage() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Global.initStorage(this);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            new RangeSelector(this, (Gallery) gallery).show();
    }
}
