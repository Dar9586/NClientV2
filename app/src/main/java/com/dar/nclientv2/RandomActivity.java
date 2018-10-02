package com.dar.nclientv2;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dar.nclientv2.api.RandomLoader;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.settings.Favorites;
import com.dar.nclientv2.settings.Global;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;

public class RandomActivity extends AppCompatActivity {
    private TextView language;
    private ImageButton thumbnail;
    private ImageButton share;
    private ImageButton favorite;
    private TextView title;
    private TextView page;
    private RandomLoader loader=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        Global.initHttpClient(this);
        Global.initImageQuality(this);
        Favorites.countFavorite(this);
        Global.initLoadImages(this);
        setContentView(R.layout.activity_random);
        FloatingActionButton shuffle = findViewById(R.id.shuffle);
        language=findViewById(R.id.language);
        thumbnail=findViewById(R.id.thumbnail);
        share=findViewById(R.id.share);
        favorite=findViewById(R.id.favorite);
        title=findViewById(R.id.title);
        page=findViewById(R.id.pages);
        loader=new RandomLoader(this);
        if(loadedGallery!=null)loadGallery(loadedGallery);
        shuffle.setOnClickListener(v -> loader.requestGallery());
        thumbnail.setOnClickListener(v -> {
            if(loadedGallery!=null) {
                Intent intent = new Intent(RandomActivity.this, GalleryActivity.class);
                intent.putExtra(RandomActivity.this.getPackageName() + ".GALLERY", loadedGallery);
                RandomActivity.this.startActivity(intent);
            }
        });
        share.setOnClickListener(v -> {
            if(loadedGallery!=null&&loadedGallery.isValid())Global.shareGallery(RandomActivity.this,loadedGallery);
        });
        favorite.setOnClickListener(v -> {
            if(loadedGallery!=null&&loadedGallery.isValid()){
                if(isFavorite){
                    if(Favorites.removeFavorite(RandomActivity.this,loadedGallery)){
                        isFavorite=false;
                        favorite.setImageResource(R.drawable.ic_favorite_border);
                    }
                }else{
                    if(Favorites.addFavorite(RandomActivity.this,loadedGallery)){
                        isFavorite=true;
                        favorite.setImageResource(R.drawable.ic_favorite);
                    }
                }
            }
        });
        shuffle.setImageTintList (ColorStateList.valueOf(Global.getTheme()== Global.ThemeScheme.LIGHT? Color.WHITE:Color.BLACK));
        share.setImageTintList   (ColorStateList.valueOf(Global.getTheme()== Global.ThemeScheme.LIGHT? Color.BLACK:Color.WHITE));
        favorite.setImageTintList(ColorStateList.valueOf(Global.getTheme()== Global.ThemeScheme.LIGHT? Color.BLACK:Color.WHITE));
        Global.setTint(shuffle.getContentBackground());
        Global.setTint(favorite.getDrawable());
        Global.setTint(share.getDrawable());
    }
    public static Gallery loadedGallery=null;
    private boolean isFavorite;
    public void loadGallery(Gallery gallery){
        loadedGallery=gallery;
        Global.loadImage(gallery.getCover().getUrl(),thumbnail);
        switch (gallery.getLanguage()){
            case CHINESE :language.setText("\uD83C\uDDE8\uD83C\uDDF3");break;
            case ENGLISH :language.setText("\uD83C\uDDEC\uD83C\uDDE7");break;
            case JAPANESE:language.setText("\uD83C\uDDEF\uD83C\uDDF5");break;
            case UNKNOWN :language.setText("\uD83C\uDFF3"); break;
        }
        isFavorite=Favorites.isFavorite(this,loadedGallery);
        favorite.setImageResource(isFavorite?R.drawable.ic_favorite:R.drawable.ic_favorite_border);
        title.setText(gallery.getTitle());
        page.setText(getString(R.string.page_count_format,gallery.getPageCount()));
    }

    @Override
    public void onBackPressed() {
        loadedGallery=null;
        super.onBackPressed();
    }
}
