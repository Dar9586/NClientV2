package com.dar.nclientv2.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Comment;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
    private List<Comment>comments;
    private SimpleDateFormat format=new SimpleDateFormat("dd/MM/yyyy", Locale.US);
    private int userId,galleryId;
    private final Activity context;
    public CommentAdapter(Activity context, List<Comment> comments,int galleryId) {
        this.context=context;
        this.galleryId=galleryId;
        this.comments = comments;
        if(Login.isLogged()&&Login.getUser()!=null){
            userId=Login.getUser().getId();
        }else userId=-1;
    }

    @NonNull
    @Override
    public CommentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CommentAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CommentAdapter.ViewHolder holder, int position) {
        Comment c=comments.get(holder.getAdapterPosition());
        Log.d(Global.LOGTAG,"CONFRONTO "+c.getUsername()+": "+c.getPosterId()+","+userId+","+(c.getPosterId()!=userId?"GONE":"VISIBLE"));
        //holder.close.setVisibility(c.getPosterId()!=userId?View.GONE:View.VISIBLE);
        holder.close.setVisibility(View.GONE);
        holder.close.setOnClickListener(v -> {
            Comment cr=comments.get(holder.getAdapterPosition());
            Global.client.newCall(new Request.Builder().url("https://nhentai.net/g/"+galleryId).build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String token=response.body().string();
                    token=token.substring(token.lastIndexOf("csrf_token"));
                    token=token.substring(token.indexOf('"')+1);
                    token=token.substring(0,token.indexOf('"'));
                    Log.d(Global.LOGTAG,"TOKEN: "+token);
                    Global.client.newCall(new Request.Builder().addHeader("Referer","https://nhentai.net/g/"+galleryId).addHeader("X-Requested-With","XMLHttpRequest").addHeader("X-CSRFToken",token).url("https://nhentai.net/api/comments/"+cr.getId()+"/delete").build()).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String xxx=response.body().string();
                            Log.d(Global.LOGTAG,"RESULTTT: "+xxx);
                            Log.d(Global.LOGTAG,"https://nhentai.net/api/comments/"+cr.getId()+"/delete");

                            //JsonReader reader =new JsonReader(response.body().charStream());
                            JsonReader reader =new JsonReader(new StringReader(xxx));
                            boolean success=false;
                            reader.beginObject();
                            while(reader.peek()!= JsonToken.END_OBJECT){
                                switch (reader.nextName()){
                                    case "success":success=reader.nextBoolean();break;
                                    default:reader.skipValue();
                                }
                            }
                            reader.close();
                            if(success){
                                comments.remove(holder.getAdapterPosition());
                                context.runOnUiThread(()->notifyItemInserted(holder.getAdapterPosition()));
                            }
                        }
                    });
                }
            });

        });
        holder.user.setText(c.getUsername());
        holder.body.setText(c.getBody());
        holder.date.setText(format.format(c.getDate()));
        if(c.getUserImageURL()==null)Global.loadImage(R.drawable.ic_person,holder.userImage);
        else Global.loadImage(c.getUserImageURL(),holder.userImage);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void addComment(Comment c) {
        comments.add(0,c);
        context.runOnUiThread(()->notifyItemInserted(0));
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageButton userImage,close;
        AppCompatTextView user,body,date;
        public ViewHolder(@NonNull View v) {
            super(v);
            userImage=v.findViewById(R.id.propic);
            close=v.findViewById(R.id.close);
            user=v.findViewById(R.id.username);
            body=v.findViewById(R.id.body);
            date=v.findViewById(R.id.date);
        }
    }
}
