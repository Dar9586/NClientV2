package com.dar.nclientv2.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.BookmarkActivity;
import com.dar.nclientv2.MainActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.classes.Bookmark;
import com.dar.nclientv2.settings.Database;

import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {
    private final List<Bookmark>bookmarks;
    private final BookmarkActivity context;
    public BookmarkAdapter(BookmarkActivity context) {
        this.context=context;
        bookmarks= Queries.BookmarkTable.getBookmarks(Database.getDatabase());
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BookmarkAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.bookmark_layout, parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bookmark bookmark=bookmarks.get(holder.getAdapterPosition());
        holder.text.setText(bookmark.toString());
        holder.page.setText(context.getString(R.string.bookmark_page_format,bookmark.page));
        holder.imgButton.setOnClickListener(v -> {
            bookmark.deleteBookmark();
            bookmarks.remove(bookmark);
            context.runOnUiThread(() -> notifyItemRemoved(holder.getAdapterPosition()));
        });
        holder.layout.setOnClickListener(v -> {
            Intent i=new Intent(context, MainActivity.class);
            i.putExtra(context.getPackageName()+".BYBOOKMARK",true);
            i.putExtra(context.getPackageName()+".INSPECTOR",bookmark.createInspector(context,null));
            context.runOnUiThread(()->{
                context.startActivity(i);
                context.finish();
            });
        });
    }

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageButton imgButton;
        TextView text,page;
        ConstraintLayout layout;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgButton=itemView.findViewById(R.id.remove_button);
            page=itemView.findViewById(R.id.page);
            text=itemView.findViewById(R.id.title);
            layout=itemView.findViewById(R.id.master_layout);
        }
    }

}
