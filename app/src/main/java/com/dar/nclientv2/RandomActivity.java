package com.dar.nclientv2;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.dar.nclientv2.api.RandomLoader;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.settings.Global;

public class RandomActivity extends AppCompatActivity {
    private FloatingActionButton shuffle;
    private ImageView language;
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
        Global.initImageQuality(this);
        Global.countFavorite(this);
        Global.initLoadImages(this);
        setContentView(R.layout.activity_random);
        shuffle=findViewById(R.id.shuffle);
        language=findViewById(R.id.language);
        thumbnail=findViewById(R.id.thumbnail);
        share=findViewById(R.id.share);
        favorite=findViewById(R.id.favorite);
        title=findViewById(R.id.title);
        page=findViewById(R.id.pages);
        loader=new RandomLoader(this);
        if(loadedGallery!=null)loadGallery(loadedGallery);
        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loader.requestGallery();
            }
        });
        thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(loadedGallery!=null) {
                    Intent intent = new Intent(RandomActivity.this, GalleryActivity.class);
                    intent.putExtra(RandomActivity.this.getPackageName() + ".GALLERY", loadedGallery);
                    RandomActivity.this.startActivity(intent);
                }
            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(loadedGallery!=null&&loadedGallery.isValid())Global.shareGallery(RandomActivity.this,loadedGallery);
            }
        });
        favorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(loadedGallery!=null&&loadedGallery.isValid()){
                    if(isFavorite){
                        if(Global.removeFavorite(RandomActivity.this,loadedGallery)){
                            isFavorite=false;
                            favorite.setImageResource(R.drawable.ic_favorite_border);
                        }
                    }else{
                        if(Global.addFavorite(RandomActivity.this,loadedGallery)){
                            isFavorite=true;
                            favorite.setImageResource(R.drawable.ic_favorite);
                        }
                    }
                }
            }
        });
        shuffle.setImageTintList(ColorStateList.valueOf(Global.getTheme()== Global.ThemeScheme.LIGHT? Color.WHITE:Color.BLACK));
        Global.setTint(shuffle.getContentBackground());
    }
    public static Gallery loadedGallery=null;
    private boolean isFavorite;
    public void loadGallery(Gallery gallery){
        loadedGallery=gallery;
        Global.loadImage(gallery.getCover().getUrl(),thumbnail);
        switch (gallery.getLanguage()){
            case CHINESE :language.setImageResource(R.drawable.ic_cn);break;
            case ENGLISH :language.setImageResource(R.drawable.ic_gb);break;
            case JAPANESE:language.setImageResource(R.drawable.ic_jp);break;
            case UNKNOWN :language.setImageResource(R.drawable.ic_help);break;
        }
        isFavorite=Global.isFavorite(this,loadedGallery);
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
