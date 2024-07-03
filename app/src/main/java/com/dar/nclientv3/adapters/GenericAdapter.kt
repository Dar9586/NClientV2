package com.dar.nclientv3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dar.nclientv3.R
import com.dar.nclientv3.api.components.GenericGallery
import java.util.*
import kotlin.collections.ArrayList

abstract class GenericAdapter<T : GenericGallery>(private val dataset: List<T>) : RecyclerView.Adapter<GenericAdapter.ViewHolder>(), Filterable {
    private var filter: MutableList<T> = ArrayList(dataset)
    private var lastQuery: String = ""

    init {
        Collections.sort(dataset) { o1, o2 -> o1.title.compareTo(o2.title) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.entry_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return filter.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint.toString().lowercase(Locale.US)
                if (lastQuery == query) return FilterResults().apply { count = filter.size }
                val results = FilterResults()
                lastQuery = query
                val filteredList = ArrayList<T>()
                for (gallery in dataset) {
                    if (gallery.title.lowercase(Locale.US).contains(query)) {
                        filteredList.add(gallery)
                    }
                }
                results.values = filteredList
                results.count = filteredList.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                results?.let {
                    filter = results.values as MutableList<T>
                    val count = results.count
                    if (count > itemCount) {
                        notifyItemRangeInserted(itemCount, count - itemCount)
                    } else if (count < itemCount) {
                        notifyItemRangeRemoved(count, itemCount - count)
                    }
                    notifyItemRangeChanged(0, filter.size - 1)
                }
            }
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imgView: ImageView = v.findViewById(R.id.image)
        val overlay: View = v.findViewById(R.id.overlay)
        val title: TextView = v.findViewById(R.id.title)
        val pages: TextView = v.findViewById(R.id.pages)
        val layout: View = v.findViewById(R.id.master_layout)
        val flag: TextView = v.findViewById(R.id.flag)
    }
}
