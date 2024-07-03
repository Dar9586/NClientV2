package com.dar.nclientv3.adapters

import android.database.Cursor
import android.util.JsonWriter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.dar.nclientv3.TagFilterActivity
import com.dar.nclientv3.api.components.Tag
import com.dar.nclientv3.api.enums.TagStatus
import com.dar.nclientv3.api.enums.TagType
import com.dar.nclientv3.async.database.Queries
import com.dar.nclientv3.settings.AuthRequest
import com.dar.nclientv3.settings.Global
import com.dar.nclientv3.settings.Login
import com.dar.nclientv3.settings.TagV2
import com.dar.nclientv3.utility.ImageDownloadUtility
import com.dar.nclientv3.utility.LogUtility
import com.dar.nclientv3.utility.Utility
import com.dar.nclientv3.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException
import java.io.StringWriter
import java.util.*

class TagsAdapter(private val context: TagFilterActivity, query: String, online: Boolean) : RecyclerView.Adapter<TagsAdapter.ViewHolder>(), Filterable {
    private var logged: Boolean = Login.isLogged()
    private var type: TagType?
    private var tagMode: TagMode
    private var lastQuery: String? = null
    private var wasSortedByName: Boolean = false
    private var cursor: Cursor? = null
    private var force: Boolean = false

    init {
        type = null
        tagMode = if (online) TagMode.ONLINE else TagMode.OFFLINE
        filter.filter(query)
    }

    constructor(cont: TagFilterActivity, query: String, type: TagType) : this(cont, query, false) {
        this.type = type
        tagMode = TagMode.TYPE
    }

    private fun writeTag(jw: JsonWriter, tag: Tag) {
        jw.beginObject()
        jw.name("id").value(tag.id)
        jw.name("name").value(tag.name)
        jw.name("type").value(tag.typeSingleName)
        jw.endObject()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val query = constraint ?: ""
                force = false
                wasSortedByName = TagV2.isSortedByName()
                lastQuery = query.toString()
                val tags = Queries.TagTable.getFilterCursor(lastQuery!!, type, tagMode == TagMode.ONLINE, TagV2.isSortedByName())
                results.count = tags.count
                results.values = tags
                LogUtility.d("${results.count},${results.values}")
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results?.count == -1) return
                val newCursor = results?.values as Cursor?
                val oldCount = itemCount
                val newCount = results?.count
                cursor?.close()
                cursor = newCursor
                if (newCount != null) {
                    if (newCount > oldCount) notifyItemRangeInserted(oldCount, newCount - oldCount)
                    else notifyItemRangeRemoved(newCount, oldCount - newCount)
                }
                notifyItemRangeChanged(0, minOf(newCount!!, oldCount))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.entry_tag_layout, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        cursor?.moveToPosition(position)
        val ent = Queries.TagTable.cursorToTag(cursor!!)
        holder.title.text = ent.name
        holder.count.text = String.format(Locale.US, "%d", ent.count)
        holder.master.setOnClickListener {
            when (tagMode) {
                TagMode.OFFLINE, TagMode.TYPE -> {
                    if (TagV2.maxTagReached() && ent.status == TagStatus.DEFAULT) {
                        context.runOnUiThread { Toast.makeText(context, context.getString(R.string.tags_max_reached, TagV2.MAXTAGS), Toast.LENGTH_LONG).show() }
                    } else {
                        TagV2.updateStatus(ent)
                        updateLogo(holder.imgView, ent.status)
                    }
                }

                TagMode.ONLINE -> {
                    try {
                        onlineTagUpdate(ent, !Login.isOnlineTags(ent), holder.imgView)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        if (tagMode != TagMode.ONLINE && logged) {
            holder.master.setOnLongClickListener {
                if (!Login.isOnlineTags(ent)) showBlacklistDialog(ent, holder.imgView)
                else Toast.makeText(context, R.string.tag_already_in_blacklist, Toast.LENGTH_SHORT).show()
                true
            }
        }
        updateLogo(holder.imgView, if (tagMode == TagMode.ONLINE) TagStatus.AVOIDED else ent.status)
    }

    override fun getItemCount(): Int {
        return cursor?.count ?: 0
    }

    private fun showBlacklistDialog(tag: Tag, imgView: ImageView) {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setIcon(R.drawable.ic_star_border).setTitle(R.string.add_to_online_blacklist).setMessage(R.string.are_you_sure)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            try {
                onlineTagUpdate(tag, true, imgView)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.setNegativeButton(R.string.no, null).show()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Throws(IOException::class)
    private fun onlineTagUpdate(tag: Tag, add: Boolean, imgView: ImageView) {
        if (!Login.isLogged() || Login.user == null) return
        val sw = StringWriter()
        val jw = JsonWriter(sw)
        jw.beginObject().name("added").beginArray()
        if (add) writeTag(jw, tag)
        jw.endArray().name("removed").beginArray()
        if (!add) writeTag(jw, tag)
        jw.endArray().endObject()
        val url = String.format(Locale.US, Utility.getBaseUrl() + "users/%d/%s/blacklist", Login.user!!.id, Login.user!!.codename)
        val ss = RequestBody.create(MediaType.get("application/json"), sw.toString())
        val request = AuthRequest(url, url, object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                if (response.body()?.string()?.contains("ok") == true) {
                    GlobalScope.launch(Dispatchers.Main) {
                        if (add) Login.addOnlineTag(tag)
                        else Login.removeOnlineTag(tag)
                        if (tagMode == TagMode.ONLINE) updateLogo(imgView, if (add) TagStatus.AVOIDED else TagStatus.DEFAULT)
                    }
                }
            }
        }).setMethod("POST", ss)
        request.start()
    }

    private fun updateLogo(img: ImageView, s: TagStatus) {
        context.runOnUiThread {
            when (s) {
                TagStatus.DEFAULT -> img.setImageDrawable(null)
                TagStatus.ACCEPTED -> {
                    ImageDownloadUtility.loadImage(R.drawable.ic_check, img)
                    Global.setTint(img.drawable)
                }

                TagStatus.AVOIDED -> {
                    ImageDownloadUtility.loadImage(R.drawable.ic_close, img)
                    Global.setTint(img.drawable)
                }
            }
        }
    }

    fun addItem() {
        force = true
        filter.filter(lastQuery)
    }

    private enum class TagMode { ONLINE, OFFLINE, TYPE }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imgView: ImageView = v.findViewById(R.id.image)
        val title: TextView = v.findViewById(R.id.title)
        val count: TextView = v.findViewById(R.id.count)
        val master: ConstraintLayout = v.findViewById(R.id.master_layout)
    }
}
