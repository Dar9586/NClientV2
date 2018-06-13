package com.dar.nclientv2.adapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.view.View;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.LocalActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.settings.Global;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocalAdapter extends GenericAdapter<LocalGallery>{
    private final LocalActivity context;

    public LocalAdapter(LocalActivity cont, ArrayList<LocalGallery> myDataset) {
        super(myDataset);
        this.context=cont;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final LocalGallery ent = filter.get(holder.getAdapterPosition());
        holder.flag.setVisibility(View.GONE);
        Global.loadImage(context,ent.getPage(ent.getMin()),holder.imgView);
        holder.title.setText(ent.getTitle());
        holder.pages.setText(String.format(Locale.US, "%d", ent.getPageCount()));
        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Layout layout = holder.title.getLayout();
                if(layout.getEllipsisCount(layout.getLineCount()-1)>0)holder.title.setMaxLines(5);
                else if(holder.title.getMaxLines()==5)holder.title.setMaxLines(2);
                else holder.layout.performClick();
            }
        });
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Global.setLoadedGallery(ent);
                Intent intent = new Intent(context, GalleryActivity.class);
                intent.putExtra(context.getPackageName()+ ".GALLERY",ent);
                intent.putExtra(context.getPackageName()+ ".ISLOCAL",true);
                context.startActivity(intent);
            }
        });
        holder.layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showDialog(holder.getAdapterPosition());
                return true;
            }
        });
    }

    private void showDialog(final int pos){
        final LocalGallery gallery=filter.get(pos);
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_gallery).setMessage(context.getString(R.string.delete_gallery_format,gallery.getTitle()));
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                filter.remove(gallery);
                dataset.remove(gallery);
                Global.recursiveDelete(gallery.getDirectory());
                notifyItemRemoved(pos);
            }
        }).setNegativeButton(R.string.no,null).setCancelable(true);
        builder.show();

    }
    @Override
    public int getItemCount() {
        return filter.size();
    }

    private List<LocalGallery> getDataset() {
        return filter;
    }
}
