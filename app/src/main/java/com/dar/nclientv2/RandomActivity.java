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
    FloatingActionButton shuffle;
    ImageView language;
    ImageButton thumbnail;
    TextView title,page;
    RandomLoader loader;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        setContentView(R.layout.activity_random);
        shuffle=findViewById(R.id.shuffle);
        language=findViewById(R.id.language);
        thumbnail=findViewById(R.id.thumbnail);
        title=findViewById(R.id.title);
        page=findViewById(R.id.pages);
        loader=new RandomLoader(this);
        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loader.requestGallery();
            }
        });
        loader.requestGallery();
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
        shuffle.setImageTintList(ColorStateList.valueOf(Global.getTheme()== Global.ThemeScheme.LIGHT? Color.WHITE:Color.BLACK));
        Global.setTint(shuffle.getContentBackground());
    }
    private Gallery loadedGallery=null;
    public void loadGallery(Gallery gallery){
        loadedGallery=gallery;
        Global.loadImage(this,gallery.getThumbnail().getUrl(),thumbnail);
        switch (gallery.getLanguage()){
            case CHINESE :language.setImageResource(R.drawable.ic_cn);break;
            case ENGLISH :language.setImageResource(R.drawable.ic_gb);break;
            case JAPANESE:language.setImageResource(R.drawable.ic_jp);break;
            case UNKNOWN :language.setImageResource(R.drawable.ic_help);break;
        }
        title.setText(gallery.getTitle());
        page.setText(getString(R.string.page_count_format,gallery.getPageCount()));
    }
}
