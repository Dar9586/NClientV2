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
import com.dar.nclientv2.api.components.Comment;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.ImageDownloadUtility;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
    private final List<Comment>comments;
    private final DateFormat format;
    private final AppCompatActivity context;
    public CommentAdapter(AppCompatActivity context, List<Comment> comments) {
        this.context=context;
        format=android.text.format.DateFormat.getDateFormat(context);
        this.comments =comments==null?new ArrayList<>():comments;
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

        holder.user.setText(c.getUsername());
        holder.body.setText(c.getBody());
        holder.date.setText(format.format(c.getDate()));
        if(c.getUserImageURL()==null||Global.getDownloadPolicy() != Global.DataUsageType.FULL)
            ImageDownloadUtility.loadImage(R.drawable.ic_person,holder.userImage);
        else
            ImageDownloadUtility.loadImage(context,c.getUserImageURL(),holder.userImage);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageButton userImage;
        final TextView user;
        final TextView body;
        final TextView date;
        final ConstraintLayout layout;
        public ViewHolder(@NonNull View v) {
            super(v);
            layout=v.findViewById(R.id.master_layout);
            userImage=v.findViewById(R.id.propic);
            user=v.findViewById(R.id.username);
            body=v.findViewById(R.id.body);
            date=v.findViewById(R.id.date);
        }
    }
}
