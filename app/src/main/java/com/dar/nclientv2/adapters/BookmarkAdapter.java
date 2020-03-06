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
import com.dar.nclientv2.utility.IntentUtility;

import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {
    private static final int LAYOUT=R.layout.bookmark_layout;

    private final List<Bookmark>bookmarks;
    private final BookmarkActivity bookmarkActivity;

    public BookmarkAdapter(BookmarkActivity bookmarkActivity) {
        this.bookmarkActivity = bookmarkActivity;
        this.bookmarks= Queries.BookmarkTable.getBookmarks(Database.getDatabase());
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(LAYOUT, parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
        int position=holder.getAdapterPosition();
        Bookmark bookmark=bookmarks.get(position);

        holder.queryText.setText(bookmark.toString());
        holder.pageLabel.setText(bookmarkActivity.getString(R.string.bookmark_page_format,bookmark.page));

        holder.deleteButton.setOnClickListener(v -> removeBookmarkAtPosition(position));

        holder.rootLayout.setOnClickListener(v -> loadBookmark(bookmark));
    }
    /**
     * Start an {@link MainActivity} with <code>bookmark</code> as query and page
     * @param bookmark bookmark to load
     * */
    private void loadBookmark(Bookmark bookmark) {
        Intent i=new Intent(bookmarkActivity, MainActivity.class);
        i.putExtra(bookmarkActivity.getPackageName()+".BYBOOKMARK",true);
        i.putExtra(bookmarkActivity.getPackageName()+".INSPECTOR",bookmark.createInspector(bookmarkActivity,null));
        IntentUtility.startAnotherActivity(bookmarkActivity,i);
    }

    /**
     * remove bookmark from the adapter at <code>position</code>
     * @param position index to delete
     * */
    private void removeBookmarkAtPosition(int position) {
        Bookmark bookmark=bookmarks.get(position);
        bookmark.deleteBookmark();
        bookmarks.remove(bookmark);
        bookmarkActivity.runOnUiThread(() -> notifyItemRemoved(position));
    }

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageButton deleteButton;
        TextView queryText, pageLabel;
        ConstraintLayout rootLayout;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            deleteButton = itemView.findViewById(R.id.remove_button);
            pageLabel    = itemView.findViewById(R.id.page);
            queryText    = itemView.findViewById(R.id.title);
            rootLayout   = itemView.findViewById(R.id.master_layout);
        }
    }

}
