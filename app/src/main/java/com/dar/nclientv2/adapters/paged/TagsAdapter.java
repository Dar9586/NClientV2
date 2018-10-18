package com.dar.nclientv2.adapters.paged;

import android.graphics.Color;
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dar.nclientv2.R;
import com.dar.nclientv2.TagFilter;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.settings.TagV2;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TagsAdapter extends ListAdapter<Tag,TagsAdapter.ViewHolder> implements Filterable{
    private final TagFilter context;
    private final boolean logged=Login.isLogged(),black=Global.getTheme()==Global.ThemeScheme.BLACK,online;
    private String lastQuery=null;
    private final TagType type;
    private boolean wasSortedByName;
    public TagsAdapter(TagFilter cont, String query, TagType type, boolean online){
        super(CALLBACK);
        context=cont;
        this.type=type;
        this.online=online;
        getFilter().filter(query);

    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results=new FilterResults();
                if(!force&&constraint.toString().equals(lastQuery)&&TagV2.isSortedByName()==wasSortedByName){
                    results.count=-1;
                }else{
                    force=false;
                    wasSortedByName=TagV2.isSortedByName();
                    lastQuery = constraint.toString();
                    Tag[] tags = Queries.TagTable.filterTags(Database.getDatabase(), lastQuery, type, online, TagV2.isSortedByName());
                    results.count = tags.length;
                    results.values = Arrays.asList(tags);
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                //if(filterTags.size()>results.count)notifyItemRangeInserted(results.count,filterTags.size()-results.count);
                //else if(filterTags.size()<results.count)notifyItemRangeRemoved(filterTags.size(),results.count-filterTags.size());
                //sortDataset();
                if(results.count!=-1) submitList((List<Tag>)results.values);
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
    private static final DiffUtil.ItemCallback<Tag> CALLBACK=new DiffUtil.ItemCallback<Tag>(){
        @Override
        public boolean areItemsTheSame(@NonNull Tag oldItem, @NonNull Tag newItem){
            return oldItem.getId()==newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Tag oldItem, @NonNull Tag newItem){
            return oldItem.getStatus()==newItem.getStatus();
        }
    };

    @NonNull
    @Override
    public TagsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TagsAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_tag_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final TagsAdapter.ViewHolder holder, int position) {
        if(black)holder.master.setBackgroundColor(Color.BLACK);
        final Tag ent=getItem(position);
        holder.title.setText(ent.getName());
        holder.count.setText(String.format(Locale.US,"%d",ent.getCount()));
        holder.master.setOnClickListener(v -> {
            if(!online) {
                if (!TagV2.maxTagReached() || ent.getStatus() != TagStatus.DEFAULT) updateLogo(holder.imgView, TagV2.updateStatus(ent));
                else Snackbar.make(context.getViewPager(), context.getString(R.string.tags_max_reached, TagV2.MAXTAGS), Snackbar.LENGTH_LONG).show();
            }else{
                try {
                    onlineTagUpdate(ent,!Login.isOnlineTags(ent),holder.imgView);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        if(!online&&logged)holder.master.setOnLongClickListener(view -> {
            if(!Login.isOnlineTags(ent)) showBlacklistDialog(ent,holder.imgView);
            else Toast.makeText(context, R.string.tag_already_in_blacklist, Toast.LENGTH_SHORT).show();
            return true;
        });
        updateLogo(holder.imgView,online?TagStatus.AVOIDED:ent.getStatus());
    }
    private void showBlacklistDialog(final Tag tag,final ImageView imgView) {
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setIcon(R.drawable.ic_star_border).setTitle(R.string.add_to_online_blacklist).setMessage(R.string.are_you_sure);
        builder.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
            try {
                onlineTagUpdate(tag,true,imgView);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).setNegativeButton(android.R.string.no,null).show();
    }
    private void onlineTagUpdate(final Tag tag, final boolean add,final ImageView imgView) throws IOException{
        StringWriter sw=new StringWriter();
        JsonWriter jw=new JsonWriter(sw);
        jw.beginObject().name("added").beginArray();
        if(add)writeTag(jw,tag);
        jw.endArray().name("removed").beginArray();
        if(!add)writeTag(jw,tag);
        jw.endArray().endObject();
        final String url=String.format(Locale.US,"https://nhentai.net/users/%s/%s/blacklist",Login.getUser().getId(),Login.getUser().getCodename());
        final RequestBody ss=RequestBody.create(MediaType.get("application/json"),sw.toString());
        Global.client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String token=response.body().string();
                token=token.substring(token.lastIndexOf("csrf_token"));
                token=token.substring(token.indexOf('"')+1);
                token=token.substring(0,token.indexOf('"'));
                Global.client.newCall(new Request.Builder().addHeader("Referer",url).addHeader("X-CSRFToken",token).url(url).post(ss).build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String s=response.body().string();
                        Log.d(Global.LOGTAG,"Response: "+s);
                        if(s.equals("{\"status\": \"ok\"}")) {
                            if (add) Login.addOnlineTag(tag);
                            else Login.removeOnlineTag(tag);
                            if(online)updateLogo(imgView, add ? TagStatus.AVOIDED : TagStatus.DEFAULT);
                        }
                    }
                });
            }
        });
    }
    private static void writeTag(JsonWriter jw, Tag tag) throws IOException{
        jw.beginObject();
        jw.name("id").value(tag.getId());
        jw.name("name").value(tag.getName());
        jw.name("type").value(tag.getTypeString());
        jw.endObject();
    }
    private static void updateLogo(ImageView img, TagStatus s){
        switch (s){
            case DEFAULT:img.setImageResource(R.drawable.ic_void);break;
            case ACCEPTED:img.setImageResource(R.drawable.ic_check);break;
            case AVOIDED:img.setImageResource(R.drawable.ic_close);break;
        }
        Global.setTint(img.getDrawable());
    }

    private boolean force=false;
    public void addItem(){
        force=true;
        getFilter().filter(lastQuery);
    }


}