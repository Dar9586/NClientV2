package com.dar.nclientv2.adapters;

import android.database.Cursor;
import android.util.JsonWriter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.TagFilterActivity;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.settings.TagV2;
import com.dar.nclientv2.utility.LogUtility;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.ViewHolder> implements Filterable{
    private enum TagMode{ONLINE,OFFLINE,TYPE}
    private final TagFilterActivity context;
    private final boolean logged=Login.isLogged();
    private String lastQuery=null;
    private final TagType type;
    private final TagMode tagMode;
    private boolean wasSortedByName;
    private Cursor cursor=null;
    public TagsAdapter(TagFilterActivity cont,String query,boolean online){
        this.context=cont;
        this.type=null;
        this.tagMode=online?TagMode.ONLINE:TagMode.OFFLINE;
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
                Cursor tags = Queries.TagTable.getFilterCursor( lastQuery, type, tagMode==TagMode.ONLINE, TagV2.isSortedByName());
                results.count = tags.getCount();
                results.values = tags;

                LogUtility.d(results.count+","+results.values);
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if(results.count==-1)return;
                LogUtility.d("PASSED");
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
                case ONLINE:
                    try {
                        onlineTagUpdate(ent,!Login.isOnlineTags(ent),holder.imgView);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }

        });
        if(tagMode!=TagMode.ONLINE&&logged)holder.master.setOnLongClickListener(view -> {
            if(!Login.isOnlineTags(ent)) showBlacklistDialog(ent,holder.imgView);
            else Toast.makeText(context, R.string.tag_already_in_blacklist, Toast.LENGTH_SHORT).show();
            return true;
        });
        updateLogo(holder.imgView,tagMode==TagMode.ONLINE?TagStatus.AVOIDED:ent.getStatus());
        LogUtility.d("PASSED: "+ent);
    }

    @Override
    public int getItemCount(){
        return cursor==null?0:cursor.getCount();
    }

    private void showBlacklistDialog(final Tag tag,final ImageView imgView) {
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setIcon(R.drawable.ic_star_border).setTitle(R.string.add_to_online_blacklist).setMessage(R.string.are_you_sure);
        builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
            try {
                onlineTagUpdate(tag,true,imgView);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).setNegativeButton(R.string.no,null).show();
    }
    private void onlineTagUpdate(final Tag tag, final boolean add,final ImageView imgView) throws IOException{
        StringWriter sw=new StringWriter();
        JsonWriter jw=new JsonWriter(sw);
        jw.beginObject().name("added").beginArray();
        if(add)writeTag(jw,tag);
        jw.endArray().name("removed").beginArray();
        if(!add)writeTag(jw,tag);
        jw.endArray().endObject();
        final String url=String.format(Locale.US,"https://nhentai.net/users/%d/%s/blacklist",Login.getUser().getId(),Login.getUser().getCodename());
        final RequestBody ss=RequestBody.create(MediaType.get("application/json"),sw.toString());
        Global.getClient(context).newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String token=response.body().string();
                token=token.substring(token.lastIndexOf("csrf_token"));
                token=token.substring(token.indexOf('"')+1);
                token=token.substring(0,token.indexOf('"'));
                Global.getClient(context).newCall(new Request.Builder().addHeader("Referer",url).addHeader("X-CSRFToken",token).url(url).post(ss).build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String s=response.body().string();
                        if(s.equals("{\"status\": \"ok\"}")) {
                            if (add) Login.addOnlineTag(tag);
                            else Login.removeOnlineTag(tag);
                            if(tagMode==TagMode.ONLINE)updateLogo(imgView, add ? TagStatus.AVOIDED : TagStatus.DEFAULT);
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
        jw.name("type").value(tag.getTypeSingleName());
        jw.endObject();
    }
    private void updateLogo(ImageView img, TagStatus s){
        context.runOnUiThread(() -> {
            switch (s){
                case DEFAULT:img.setImageDrawable(null);break;//Global.loadImage(R.drawable.ic_void,img); break;
                case ACCEPTED:Global.loadImage(R.drawable.ic_check,img);Global.setTint(img.getDrawable());break;
                case AVOIDED:Global.loadImage(R.drawable.ic_close,img);Global.setTint(img.getDrawable());break;
            }
        });

    }

    private boolean force=false;
    public void addItem(){
        force=true;
        getFilter().filter(lastQuery);
    }


}
