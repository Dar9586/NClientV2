package com.dar.nclientv2.adapters;

import android.os.Build;
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
import com.dar.nclientv2.api.comments.Comment;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.utility.ImageDownloadUtility;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
    private final List<Comment>comments;
    private final DateFormat format;
    private final int userId;
    private final int galleryId;
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
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CommentAdapter.ViewHolder holder, int position) {
        Comment c=comments.get(holder.getAdapterPosition());
        holder.layout.setOnClickListener(v1 -> {
            if(Build.VERSION.SDK_INT>Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
                context.runOnUiThread(() -> holder.body.setMaxLines(holder.body.getMaxLines()==7?999:7));
            }
        });
        holder.close.setVisibility(c.getPosterId()!=userId?View.GONE:View.VISIBLE);
        holder.user.setText(c.getUsername());
        holder.body.setText(c.getComment());
        holder.date.setText(format.format(c.getPostDate()));
        if(c.getAvatarUrl()==null||Global.getDownloadPolicy() != Global.DataUsageType.FULL)
            ImageDownloadUtility.loadImage(R.drawable.ic_person,holder.userImage);
        else
            ImageDownloadUtility.loadImage(context,c.getAvatarUrl(),holder.userImage);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void addComment(Comment c) {
        comments.add(0,c);
        context.runOnUiThread(()->notifyItemInserted(0));
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageButton userImage;
        final ImageButton close;
        final TextView user;
        final TextView body;
        final TextView date;
        final ConstraintLayout layout;
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
