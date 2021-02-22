package com.dar.nclientv2;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.ImageViewCompat;

import com.dar.nclientv2.api.RandomLoader;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.components.activities.GeneralActivity;
import com.dar.nclientv2.settings.Favorites;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.ImageDownloadUtility;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class RandomActivity extends GeneralActivity {
    public static Gallery loadedGallery = null;
    private TextView language;
    private ImageButton thumbnail;
    private ImageButton favorite;
    private TextView title;
    private TextView page;
    private View censor;
    private RandomLoader loader = null;
    private boolean isFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.activity_random);
        loader = new RandomLoader(this);


        //init components id
        Toolbar toolbar = findViewById(R.id.toolbar);
        FloatingActionButton shuffle = findViewById(R.id.shuffle);
        ImageButton share = findViewById(R.id.share);
        censor = findViewById(R.id.censor);
        language = findViewById(R.id.language);
        thumbnail = findViewById(R.id.thumbnail);
        favorite = findViewById(R.id.favorite);
        title = findViewById(R.id.title);
        page = findViewById(R.id.pages);

        //init toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.random_manga);


        if (loadedGallery != null) loadGallery(loadedGallery);

        shuffle.setOnClickListener(v -> loader.requestGallery());

        thumbnail.setOnClickListener(v -> {
            if (loadedGallery != null) {
                Intent intent = new Intent(RandomActivity.this, GalleryActivity.class);
                intent.putExtra(RandomActivity.this.getPackageName() + ".GALLERY", loadedGallery);
                RandomActivity.this.startActivity(intent);
            }
        });
        share.setOnClickListener(v -> {
            if (loadedGallery != null) Global.shareGallery(RandomActivity.this, loadedGallery);
        });
        censor.setOnClickListener(v -> censor.setVisibility(View.GONE));

        favorite.setOnClickListener(v -> {
            if (loadedGallery != null) {
                if (isFavorite) {
                    if (Favorites.removeFavorite(loadedGallery)) isFavorite = false;
                } else if (Favorites.addFavorite(loadedGallery)) isFavorite = true;
            }
            favoriteUpdateButton();
        });

        ColorStateList colorStateList = ColorStateList.valueOf(Global.getTheme() == Global.ThemeScheme.LIGHT ? Color.WHITE : Color.BLACK);

        ImageViewCompat.setImageTintList(shuffle, colorStateList);
        ImageViewCompat.setImageTintList(share, colorStateList);
        ImageViewCompat.setImageTintList(favorite, colorStateList);

        Global.setTint(shuffle.getContentBackground());
        Global.setTint(share.getDrawable());
        Global.setTint(favorite.getDrawable());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void loadGallery(Gallery gallery) {
        loadedGallery = gallery;
        if (Global.isDestroyed(this)) return;
        ImageDownloadUtility.loadImage(this, gallery.getCover(), thumbnail);
        language.setText(Global.getLanguageFlag(gallery.getLanguage()));
        isFavorite = Favorites.isFavorite(loadedGallery);
        favoriteUpdateButton();
        title.setText(gallery.getTitle());
        page.setText(getString(R.string.page_count_format, gallery.getPageCount()));
        censor.setVisibility(gallery.hasIgnoredTags() ? View.VISIBLE : View.GONE);
    }

    private void favoriteUpdateButton() {
        runOnUiThread(() -> {
            ImageDownloadUtility.loadImage(isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border, favorite);
            Global.setTint(favorite.getDrawable());
        });
    }

    @Override
    public void onBackPressed() {
        loadedGallery = null;
        super.onBackPressed();
    }
}
