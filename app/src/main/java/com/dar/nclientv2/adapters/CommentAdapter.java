package com.dar.nclientv2.adapters;

import android.os.Build;
import android.util.JsonReader;
import android.util.JsonToken;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Comment;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
    private List<Comment>comments;
    private DateFormat format;
    private int userId,galleryId;
    private final AppCompatActivity context;
    public CommentAdapter(AppCompatActivity context, List<Comment> comments, int galleryId) {
        this.context=context;
        format=android.text.format.DateFormat.getDateFormat(context);
        this.galleryId=galleryId;
        this.comments =comments==null?new ArrayList<>():comments;
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
        holder.layout.setOnClickListener(v1 -> {
            if(Build.VERSION.SDK_INT>Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
                context.runOnUiThread(() -> {
                    holder.body.setMaxLines(holder.body.getMaxLines()==7?999:7);
                });
            }
        });
        holder.close.setVisibility(c.getPosterId()!=userId?View.GONE:View.VISIBLE);
        holder.close.setOnClickListener(v -> {
            Comment cr=comments.get(holder.getAdapterPosition());
            Global.getClient(context).newCall(new Request.Builder().post(new FormBody.Builder().build()).url("https://nhentai.net/g/"+galleryId).build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String token=response.body().string();
                    token=token.substring(token.lastIndexOf("csrf_token"));
                    token=token.substring(token.indexOf('"')+1);
                    token=token.substring(0,token.indexOf('"'));
                    Request.Builder builder=new Request.Builder()
                            .addHeader("Referer","https://nhentai.net/g/"+galleryId)
                            .addHeader("X-Requested-With","XMLHttpRequest")
                            .addHeader("X-CSRFToken",token)
                            .post(new FormBody.Builder().add("x","x").build())
                            .url("https://nhentai.net/api/comments/"+cr.getId()+"/delete");

                    StringBuilder builder1=new StringBuilder();
                    for (Cookie cookie:Global.getClient(context).cookieJar().loadForRequest(HttpUrl.get("https://nhentai.net/api/comments/"+cr.getId()+"/delete"))){
                        builder1.append(cookie.name()).append('=').append(cookie.value()).append("; ");
                    }
                    builder.addHeader("Cookie",builder1.toString());
                    Global.getClient(context).newCall(builder.build()).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String xxx=response.body().string();
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
                                context.runOnUiThread(()->notifyItemRemoved(holder.getAdapterPosition()));
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
        else Global.loadImage(context,c.getUserImageURL(),holder.userImage);
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
        ImageButton userImage,close;
        TextView user,body,date;
        ConstraintLayout layout;
        public ViewHolder(@NonNull View v) {
            super(v);
            layout=v.findViewById(R.id.master_layout);
            userImage=v.findViewById(R.id.propic);
            close=v.findViewById(R.id.close);
            user=v.findViewById(R.id.username);
            body=v.findViewById(R.id.body);
            date=v.findViewById(R.id.date);
        }
    }
}
