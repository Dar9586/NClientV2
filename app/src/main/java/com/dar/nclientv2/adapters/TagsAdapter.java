package com.dar.nclientv2.adapters;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.TagFilterActivity;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;
import com.dar.nclientv2.utility.ImageDownloadUtility;
import com.dar.nclientv2.utility.LogUtility;

import java.util.Locale;

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.ViewHolder> implements Filterable{
    private enum TagMode{OFFLINE,TYPE}
    private final TagFilterActivity context;
    private String lastQuery=null;
    private final TagType type;
    private final TagMode tagMode;
    private boolean wasSortedByName;
    private Cursor cursor=null;
    public TagsAdapter(TagFilterActivity cont,String query){
        this.context=cont;
        this.type=null;
        this.tagMode=TagMode.OFFLINE;
        getFilter().filter(query);
    }
    public TagsAdapter(TagFilterActivity cont, String query,TagType type){
        this.context=cont;
        this.type=type;
        this.tagMode=TagMode.TYPE;
        getFilter().filter(query);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results=new FilterResults();
                if(constraint==null)constraint="";
                force=false;
                wasSortedByName=TagV2.isSortedByName();

                lastQuery = constraint.toString();
                Cursor tags = Queries.TagTable.getFilterCursor( lastQuery, type, TagV2.isSortedByName());
                results.count = tags.getCount();
                results.values = tags;

                LogUtility.d(results.count+","+results.values);
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if(results.count==-1)return;
                Cursor newCursor=(Cursor)results.values;
                int oldCount=getItemCount(),newCount=results.count;
                if(cursor!=null)cursor.close();
                cursor=newCursor;
                if(newCount>oldCount)notifyItemRangeInserted(oldCount,newCount-oldCount);
                else notifyItemRangeRemoved(newCount,oldCount-newCount);
                notifyItemRangeChanged(0,Math.min(newCount,oldCount));
            }
        };
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgView;
        final TextView title,count;
        final ConstraintLayout master;
        ViewHolder(View v) {
            super(v);
            imgView = v.findViewById(R.id.image);
            title = v.findViewById(R.id.title);
            count = v.findViewById(R.id.count);
            master=v.findViewById(R.id.master_layout);
        }
    }


    @NonNull
    @Override
    public TagsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TagsAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_tag_layout, parent, false));
    }
    @Override
    public void onBindViewHolder(@NonNull final TagsAdapter.ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        final Tag ent=Queries.TagTable.cursorToTag(cursor);
        holder.title.setText(ent.getName());
        holder.count.setText(String.format(Locale.US,"%d",ent.getCount()));
        holder.master.setOnClickListener(v -> {
            switch (tagMode){
                case OFFLINE:case TYPE:
                    if(TagV2.maxTagReached()&&ent.getStatus()==TagStatus.DEFAULT){
                        context.runOnUiThread(()-> Toast.makeText(context,context.getString(R.string.tags_max_reached, TagV2.MAXTAGS), Toast.LENGTH_LONG).show());
                    }else {
                        TagV2.updateStatus(ent);
                        updateLogo(holder.imgView, ent.getStatus());
                    }
                    break;
            }

        });

        updateLogo(holder.imgView,ent.getStatus());
    }

    @Override
    public int getItemCount(){
        return cursor==null?0:cursor.getCount();
    }

    private void updateLogo(ImageView img, TagStatus s){
        context.runOnUiThread(() -> {
            switch (s){
                case DEFAULT:img.setImageDrawable(null);break;//ImageDownloadUtility.loadImage(R.drawable.ic_void,img); break;
                case ACCEPTED: ImageDownloadUtility.loadImage(R.drawable.ic_check,img);Global.setTint(img.getDrawable());break;
                case AVOIDED:  ImageDownloadUtility.loadImage(R.drawable.ic_close,img);Global.setTint(img.getDrawable());break;
            }
        });

    }

    private boolean force=false;
    public void addItem(){
        force=true;
        getFilter().filter(lastQuery);
    }


}
