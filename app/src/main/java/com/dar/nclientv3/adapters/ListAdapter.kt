package com.dar.nclientv3.adapters

import android.content.Intent
import android.os.Build
import android.text.Layout
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.dar.nclientv3.GalleryActivity
import com.dar.nclientv3.MainActivity
import com.dar.nclientv3.R
import com.dar.nclientv3.api.InspectorV3
import com.dar.nclientv3.api.SimpleGallery
import com.dar.nclientv3.api.components.GenericGallery
import com.dar.nclientv3.api.enums.Language
import com.dar.nclientv3.async.database.Queries
import com.dar.nclientv3.components.activities.BaseActivity
import com.dar.nclientv3.settings.Global
import com.dar.nclientv3.settings.TagV2
import com.dar.nclientv3.utility.ImageDownloadUtility
import com.dar.nclientv3.utility.LogUtility
import com.google.android.material.snackbar.Snackbar
import java.util.*
import kotlin.collections.ArrayList

class ListAdapter(private val context: BaseActivity) : RecyclerView.Adapter<GenericAdapter.ViewHolder>() {
    private val statuses = SparseIntArray()
    private val mDataset = ArrayList<SimpleGallery>()
    private val storagePermission: Boolean = Global.hasStoragePermission(context)
    private val queryString: String? = TagV2.getAvoidedTags()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericAdapter.ViewHolder {
        return GenericAdapter.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.entry_layout, parent, false))
    }

    private fun loadGallery(holder: GenericAdapter.ViewHolder, ent: SimpleGallery?) {
        if (context.isFinishing) return
        try {
            if (Global.isDestroyed(context)) return

            ImageDownloadUtility.loadImage(context, ent?.thumbnail, holder.imgView)
        } catch (ignore: VerifyError) {
        }
    }

    override fun onBindViewHolder(holder: GenericAdapter.ViewHolder, position: Int) {
        val holderPos = holder.bindingAdapterPosition
        if (holderPos >= mDataset.size) return
        val ent = mDataset[holderPos]
        if (!Global.showTitles()) {
            holder.title.alpha = 0f
            holder.flag.alpha = 0f
        } else {
            holder.title.alpha = 1f
            holder.flag.alpha = 1f
        }
        if (context is GalleryActivity) {
            val card = holder.layout.parent as CardView
            val params = card.layoutParams
            params.width = Global.getGalleryWidth()
            params.height = Global.getGalleryHeight()
            card.layoutParams = params
        }
        holder.overlay.visibility = if (queryString != null && ent.hasIgnoredTags(queryString)) View.VISIBLE else View.GONE
        loadGallery(holder, ent)
        holder.pages.visibility = View.GONE
        holder.title.text = ent.title
        holder.flag.visibility = View.VISIBLE
        if (Global.getOnlyLanguage() == Language.ALL || context is GalleryActivity) {
            holder.flag.text = Global.getLanguageFlag(ent.language)
        } else holder.flag.visibility = View.GONE
        holder.title.setOnClickListener {
            val layout: Layout? = holder.title.layout
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if ((layout?.getEllipsisCount(layout.lineCount - 1) ?: 0) > 0)
                    holder.title.maxLines = 7
                else if (holder.title.maxLines == 7) holder.title.maxLines = 3
                else holder.layout.performClick()
            } else holder.layout.performClick()
        }
        holder.layout.setOnClickListener {
            /*val intent = Intent(context, GalleryActivity::class.java)
            intent.putExtra(context.packageName + ".ID", ent.id)
            context.startActivity(intent)*/
            if (context is MainActivity) {
                context.setIdOpenedGallery(ent.id)
            }
            downloadGallery(ent)
            holder.overlay.visibility = if (queryString != null && ent.hasIgnoredTags(queryString)) View.VISIBLE else View.GONE
        }
        holder.overlay.setOnClickListener { holder.overlay.visibility = View.GONE }
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

    fun updateColor(id: Int) {
        if (id < 0) return
        var position = -1
        statuses.put(id, Queries.StatusMangaTable.getStatus(id).color)
        for (i in mDataset.indices) {
            val gallery = mDataset[i]
            if (gallery.id == id) {
                position = id
                break
            }
        }
        if (position >= 0) notifyItemChanged(position)
    }

    private fun downloadGallery(ent: SimpleGallery) {
        InspectorV3.galleryInspector(context, ent.id, object : InspectorV3.DefaultInspectorResponse() {
            override fun onFailure(e: Exception) {
                super.onFailure(e)
                val file = Global.findGalleryFolder(context, ent.id)
                if (file != null) {
                    // LocalAdapter.startGallery(context, file) // Uncomment if LocalAdapter is defined
                } else if (context.masterLayout != null) {
                    context.runOnUiThread {
                        val snackbar = Snackbar.make(context.masterLayout, R.string.unable_to_connect_to_the_site, Snackbar.LENGTH_SHORT)
                        snackbar.setAction(R.string.retry) { downloadGallery(ent) }
                        snackbar.show()
                    }
                }
            }

            override fun onSuccess(galleries: List<GenericGallery>) {
                if (galleries.size != 1) {
                    if (context.masterLayout != null) {
                        context.runOnUiThread {
                            Snackbar.make(context.masterLayout, R.string.no_entry_found, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    return
                }
                val intent = Intent(context, GalleryActivity::class.java)
                LogUtility.d(galleries[0].toString())
                intent.putExtra(context.packageName + ".GALLERY", galleries[0])
                context.runOnUiThread { context.startActivity(intent) }
            }
        }).start()
    }

    override fun getItemCount(): Int {
        return mDataset.size
    }

    fun addGalleries(galleries: List<GenericGallery>) {
        val c = mDataset.size
        for (g in galleries) {
            mDataset.add(g as SimpleGallery)
            LogUtility.d("Simple: $g")
        }
        LogUtility.d(String.format(Locale.US, "%s,old:%d,new:%d,len%d", this, c, mDataset.size, galleries.size))
        context.runOnUiThread { notifyItemRangeInserted(c, galleries.size) }
    }

    fun restartDataset(galleries: List<GenericGallery>) {
        /*val c = mDataset.size
        if (c > 0) {
            mDataset.clear()
            context.runOnUiThread { notifyItemRangeRemoved(0, c) }
        }
        mDataset.addAll(galleries)
        context.runOnUiThread { notifyItemRangeInserted(0, galleries.size) }*/
        mDataset.clear()
        for (g in galleries) {
            if (g is SimpleGallery) {
                mDataset.add(g)
            }
        }
        context.runOnUiThread { notifyDataSetChanged() }
    }

    fun resetStatuses() {
        statuses.clear()
    }
}
