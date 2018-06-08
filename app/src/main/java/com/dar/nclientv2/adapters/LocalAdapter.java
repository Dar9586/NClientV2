package com.dar.nclientv2.adapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.LocalActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.settings.Global;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocalAdapter extends RecyclerView.Adapter<LocalAdapter.ViewHolder> {
    private final ArrayList<LocalGallery> mDataset,filterDataset;
    private final LocalActivity context;
    static class ViewHolder extends RecyclerView.ViewHolder {

        final ImageView imgView,flag;
        final TextView title,pages;
        final ConstraintLayout layout;
        ViewHolder(View v) {
            super(v);
            imgView = v.findViewById(R.id.image);
            title = v.findViewById(R.id.title);
            pages = v.findViewById(R.id.pages);
            layout = v.findViewById(R.id.master_layout);
            flag=v.findViewById(R.id.flag);
        }
    }

    public LocalAdapter(LocalActivity cont, ArrayList<LocalGallery> myDataset) {
        this.context=cont;
        this.mDataset = myDataset;
        filterDataset=new ArrayList<>();
        filterDataset.addAll(mDataset);
    }

    @NonNull
    @Override
    public LocalAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LocalAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_layout, parent, false));
    }
    @Override
    public void onBindViewHolder(@NonNull final LocalAdapter.ViewHolder holder, int position) {
        final LocalGallery ent = filterDataset.get(holder.getAdapterPosition());
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
        final LocalGallery gallery=filterDataset.get(pos);
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_gallery).setMessage(context.getString(R.string.delete_gallery_format,gallery.getTitle()));
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                filterDataset.remove(gallery);
                mDataset.remove(gallery);
                Global.recursiveDelete(gallery.getDirectory());
                notifyItemRemoved(pos);
            }
        }).setNegativeButton(R.string.no,null).setCancelable(true);
        builder.show();

    }
    @Override
    public int getItemCount() {
        return filterDataset.size();
    }

    private List<LocalGallery> getDataset() {
        return filterDataset;
    }
    public void filter(String query){
        filterDataset.clear();
        for(LocalGallery x:mDataset)if(x.getTitle().toLowerCase(Locale.US).contains(query.toLowerCase(Locale.US)))filterDataset.add(x);
        notifyDataSetChanged();
    }
}
