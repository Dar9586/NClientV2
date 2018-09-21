package com.dar.nclientv2.adapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.view.View;
import android.widget.ArrayAdapter;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.LocalActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.async.CreatePDF;
import com.dar.nclientv2.settings.Global;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocalAdapter extends GenericAdapter<LocalGallery>{
    private final LocalActivity context;
    private final boolean black;
    public LocalAdapter(LocalActivity cont, ArrayList<LocalGallery> myDataset) {
        super(myDataset);
        this.context=cont;
        black=Global.getTheme()== Global.ThemeScheme.BLACK;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final LocalGallery ent = filter.get(holder.getAdapterPosition());
        if(black)holder.layout.setBackgroundColor(Color.BLACK);
        holder.flag.setVisibility(View.GONE);
        Global.loadImage(ent.getPage(ent.getMin()),holder.imgView);
        holder.title.setText(ent.getTitle());
        holder.pages.setText(String.format(Locale.US, "%d", ent.getPageCount()));
        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Layout layout = holder.title.getLayout();
                if(layout.getEllipsisCount(layout.getLineCount()-1)>0)holder.title.setMaxLines(7);
                else if(holder.title.getMaxLines()==7)holder.title.setMaxLines(3);
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
                createPDF(holder.getAdapterPosition());
                return true;
            }
        });
    }

    private void showDialogDelete(final int pos){
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
    private void showDialogPDF(final int pos){
        final LocalGallery gallery=filter.get(pos);
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle(R.string.create_pdf).setMessage(context.getString(R.string.create_pdf_format,gallery.getTitle()));
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i=new Intent(context.getApplicationContext(),CreatePDF.class);
                i.putExtra(context.getPackageName()+".PATH",gallery.getDirectory().getAbsolutePath());
                i.putExtra(context.getPackageName()+".PAGES",gallery.getPageCount());
                context.startService(i);
            }
        }).setNegativeButton(R.string.no,null).setCancelable(true);
        builder.show();
    }
    private void createPDF(final int pos){
        ArrayAdapter<String>adapter=new ArrayAdapter<>(context,android.R.layout.select_dialog_item);
        adapter.add(context.getString(R.string.delete_gallery));
        adapter.add(context.getString(R.string.create_pdf));
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle(R.string.settings).setIcon(R.drawable.ic_settings);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:showDialogDelete(pos);break;
                    case 1:showDialogPDF(pos);break;
                }
            }
        }).show();
    }
    @Override
    public int getItemCount() {
        return filter.size();
    }

    private List<LocalGallery> getDataset() {
        return filter;
    }
}
