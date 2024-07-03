package com.dar.nclientv3.adapters

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.dar.nclientv3.R
import com.dar.nclientv3.SearchActivity
import com.dar.nclientv3.components.classes.History
import com.dar.nclientv3.settings.Global
import com.dar.nclientv3.utility.ImageDownloadUtility
import java.util.*

class HistoryAdapter(private val context: SearchActivity) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    private val history: MutableList<History>
    private var remove = -1

    init {
        if (!Global.isKeepHistory()) {
            context.getSharedPreferences("History", 0).edit().clear().apply()
        }
        val prefs: SharedPreferences = context.getSharedPreferences("History", 0)
        history = if (Global.isKeepHistory()) {
            History.setToList(prefs.getStringSet("history", HashSet())!!)
        } else {
            ArrayList()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.entry_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val historyItem = history[holder.bindingAdapterPosition]
        ImageDownloadUtility.loadImage(if (remove == holder.bindingAdapterPosition) R.drawable.ic_close else R.drawable.ic_mode_edit, holder.imageButton)
        holder.text.text = historyItem.value
        holder.master.setOnClickListener { context.setQuery(historyItem.value, true) }
        holder.imageButton.setOnLongClickListener {
            context.runOnUiThread {
                if (remove == holder.bindingAdapterPosition) {
                    remove = -1
                    notifyItemChanged(holder.bindingAdapterPosition)
                } else {
                    if (remove != -1) {
                        val lastRemove = remove
                        remove = -1
                        notifyItemChanged(lastRemove)
                    }
                    remove = holder.bindingAdapterPosition
                    notifyItemChanged(holder.bindingAdapterPosition)
                }
            }
            true
        }
        holder.imageButton.setOnClickListener {
            if (remove == holder.bindingAdapterPosition) {
                removeHistory(remove)
                remove = -1
            } else {
                context.setQuery(historyItem.value, false)
            }
        }
    }

    override fun getItemCount(): Int {
        return history.size
    }

    fun addHistory(value: String) {
        if (!Global.isKeepHistory()) return
        val newHistory = History(value, false)
        val pos = history.indexOf(newHistory)
        if (pos >= 0) {
            history[pos] = newHistory
        } else {
            history.add(newHistory)
        }
        context.getSharedPreferences("History", 0).edit().putStringSet("history", History.listToSet(history)).apply()
    }

    private fun removeHistory(pos: Int) {
        if (pos < 0 || pos >= history.size) return
        history.removeAt(pos)
        context.getSharedPreferences("History", 0).edit().putStringSet("history", History.listToSet(history)).apply()
        context.runOnUiThread { notifyItemRemoved(pos) }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val master: ConstraintLayout = itemView.findViewById(R.id.master_layout)
        val text: TextView = itemView.findViewById(R.id.text)
        val imageButton: ImageButton = itemView.findViewById(R.id.edit)
    }
}
