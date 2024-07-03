package com.dar.nclientv3.adapters

import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.dar.nclientv3.FavoriteActivity
import com.dar.nclientv3.GalleryActivity
import com.dar.nclientv3.R
import com.dar.nclientv3.api.components.Gallery
import com.dar.nclientv3.async.database.Queries
import com.dar.nclientv3.settings.Global
import com.dar.nclientv3.utility.ImageDownloadUtility
import com.dar.nclientv3.utility.LogUtility
import com.dar.nclientv3.utility.Utility
import java.io.IOException
import java.util.*

class FavoriteAdapter(private val activity: FavoriteActivity) : RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder>(), Filterable {
    private val perPage = FavoriteActivity.getEntryPerPage()
    private val statuses = SparseIntArray()
    private var galleries: Array<Gallery?> = emptyArray()
    private var lastQuery: CharSequence = ""
    private var cursor: Cursor? = null
    private var force = false
    private var sortByTitle = false

    init {
        setHasStableIds(true)
    }

    // Wrapper class for GenericAdapter.ViewHolder
    class FavoriteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Assuming GenericAdapter.ViewHolder has these properties
        val imgView: View = view.findViewById(R.id.image)
        val pages: View = view.findViewById(R.id.pages)
        val title: View = view.findViewById(R.id.title)
        val flag: View = view.findViewById(R.id.flag)
        val layout: View = view.findViewById(R.id.layout)
    }

    override fun getItemId(position: Int): Long {
        cursor?.moveToPosition(position)
        return cursor?.getInt(cursor?.getColumnIndex(Queries.GalleryTable.IDGALLERY) ?: -1)?.toLong() ?: -1L
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.entry_layout, parent, false)
        return FavoriteViewHolder(view)
    }

    private fun galleryFromPosition(position: Int): Gallery? {
        galleries[position]?.let { return it }
        cursor?.moveToPosition(position)
        return try {
            val g = Queries.GalleryTable.cursorToGallery(cursor)
            galleries[position] = g
            g
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val ent = galleryFromPosition(holder.bindingAdapterPosition) ?: return
        ImageDownloadUtility.loadImage(activity, ent.thumbnail, holder.imgView as ImageView)
        (holder.pages as? android.widget.TextView)?.text = String.format(Locale.US, "%d", ent.pageCount)
        (holder.title as? android.widget.TextView)?.text = ent.title
        (holder.flag as? android.widget.TextView)?.text = Global.getLanguageFlag(ent.language)
        holder.title.setOnClickListener {
            val layout = (holder.title as? android.widget.TextView)?.layout
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 && layout != null) {
                if (layout.getEllipsisCount(layout.lineCount - 1) > 0)
                    (holder.title as? android.widget.TextView)?.maxLines = 7
                else if ((holder.title as? android.widget.TextView)?.maxLines == 7) (holder.title as? android.widget.TextView)?.maxLines = 3
                else holder.layout.performClick()
            } else holder.layout.performClick()
        }
        holder.layout.setOnClickListener { startGallery(ent) }
        holder.layout.setOnLongClickListener {
            holder.title.animate().alpha(if (holder.title.alpha == 0f) 1f else 0f).setDuration(100).start()
            holder.flag.animate().alpha(if (holder.flag.alpha == 0f) 1f else 0f).setDuration(100).start()
            holder.pages.animate().alpha(if (holder.pages.alpha == 0f) 1f else 0f).setDuration(100).start()
            true
        }
        var statusColor = statuses.get(ent.id, 0)
        if (statusColor == 0) {
            statusColor = Queries.StatusMangaTable.getStatus(ent.id).color
            statuses.put(ent.id, statusColor)
        }
        holder.title.setBackgroundColor(statusColor)
    }

    private fun startGallery(ent: Gallery) {
        val intent = Intent(activity, GalleryActivity::class.java)
        LogUtility.d(ent.toString())
        intent.putExtra("${activity.packageName}.GALLERY", ent)
        intent.putExtra("${activity.packageName}.UNKNOWN", true)
        activity.startActivity(intent)
    }

    fun changePage() {
        forceReload()
    }

    fun updateColor(position: Int) {
        val ent = galleryFromPosition(position) ?: return
        val id = ent.id
        statuses.put(id, Queries.StatusMangaTable.getStatus(id).color)
        notifyItemChanged(position)
    }

    override fun getItemCount(): Int = cursor?.count ?: 0

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults? {
                val lowercaseConstraint = constraint.toString().lowercase(Locale.US)
                if (!force && lastQuery == lowercaseConstraint) return null
                LogUtility.d("FILTERING")
                setRefresh(true)
                val results = FilterResults()
                lastQuery = lowercaseConstraint
                LogUtility.d("$lastQuery LAST QUERY")
                force = false
                val c = Queries.FavoriteTable.getAllFavoriteGalleriesCursor(lastQuery, sortByTitle, perPage, (activity.actualPage - 1) * perPage)
                results.count = c.count
                results.values = c
                LogUtility.d("FILTERING3")
                LogUtility.e("${results.count};${results.values}")
                setRefresh(false)
                return results
            }

            override fun publishResults(constraint: CharSequence, results: FilterResults?) {
                results ?: return
                setRefresh(true)
                LogUtility.d("After called2")
                val oldSize = itemCount
                val newSize = results.count
                updateCursor(results.values as Cursor?)
                if (oldSize > newSize) notifyItemRangeRemoved(newSize, oldSize - newSize)
                else notifyItemRangeInserted(oldSize, newSize - oldSize)
                notifyItemRangeChanged(0, minOf(newSize, oldSize))
                setRefresh(false)
            }
        }
    }

    fun setSortByTitle(sortByTitle: Boolean) {
        this.sortByTitle = sortByTitle
        forceReload()
    }

    fun forceReload() {
        force = true
        activity.runOnUiThread { filter.filter(lastQuery) }
    }

    fun setRefresh(refresh: Boolean) {
        activity.runOnUiThread { activity.refresher.isRefreshing = refresh }
    }

    fun clearGalleries() {
        Queries.FavoriteTable.removeAllFavorite()
        val s = itemCount
        updateCursor(null)
        activity.runOnUiThread { notifyItemRangeRemoved(0, s) }
    }

    private fun updateCursor(c: Cursor?) {
        cursor?.close()
        galleries = arrayOfNulls(c?.count ?: 0)
        cursor = c
        statuses.clear()
    }

    fun getAllGalleries(): Collection<Gallery> {
        val cursor = cursor ?: return emptyList()
        return List(cursor.count) { galleryFromPosition(it) }.filterNotNull()
    }

    fun randomGallery() {
        val cursor = cursor ?: return
        if (cursor.count < 1) return
        startGallery(galleryFromPosition(Utility.RANDOM.nextInt(cursor.count)) ?: return)
    }
}
