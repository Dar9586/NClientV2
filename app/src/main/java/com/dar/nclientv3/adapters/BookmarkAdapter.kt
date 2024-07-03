package com.dar.nclientv3.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.dar.nclientv3.BookmarkActivity
import com.dar.nclientv3.MainActivity
import com.dar.nclientv3.R
import com.dar.nclientv3.async.database.Queries
import com.dar.nclientv3.components.classes.Bookmark
import com.dar.nclientv3.utility.IntentUtility

class BookmarkAdapter(private val bookmarkActivity: BookmarkActivity) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {
    private val bookmarks: MutableList<Bookmark> = Queries.BookmarkTable.getBookmarks()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.bookmark_layout, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val position = holder.bindingAdapterPosition
        val bookmark = bookmarks[position]

        holder.queryText.text = bookmark.toString()
        holder.pageLabel.text = bookmarkActivity.getString(R.string.bookmark_page_format, bookmark.page)

        holder.deleteButton.setOnClickListener { removeBookmarkAtPosition(position) }

        holder.rootLayout.setOnClickListener { loadBookmark(bookmark) }
    }

    private fun loadBookmark(bookmark: Bookmark) {
        val i = Intent(bookmarkActivity, MainActivity::class.java)
        i.putExtra("${bookmarkActivity.packageName}.BYBOOKMARK", true)
        i.putExtra("${bookmarkActivity.packageName}.INSPECTOR", bookmark.createInspector(bookmarkActivity, null))
        IntentUtility.startAnotherActivity(bookmarkActivity, i)
    }

    private fun removeBookmarkAtPosition(position: Int) {
        if (position >= bookmarks.size) return
        val bookmark = bookmarks[position]

        bookmark.deleteBookmark()
        bookmarks.remove(bookmark)
        bookmarkActivity.runOnUiThread { notifyItemRemoved(position) }
    }

    override fun getItemCount(): Int {
        return bookmarks.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deleteButton: AppCompatImageButton = itemView.findViewById(R.id.remove_button)
        val queryText: TextView = itemView.findViewById(R.id.title)
        val pageLabel: TextView = itemView.findViewById(R.id.page)
        val rootLayout: ConstraintLayout = itemView.findViewById(R.id.master_layout)
    }
}
