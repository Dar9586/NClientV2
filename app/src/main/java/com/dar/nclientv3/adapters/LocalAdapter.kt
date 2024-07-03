package com.dar.nclientv3.adapters

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.dar.nclientv3.GalleryActivity
import com.dar.nclientv3.LocalActivity
import com.dar.nclientv3.R
import com.dar.nclientv3.api.local.LocalGallery
import com.dar.nclientv3.api.local.LocalSortType
import com.dar.nclientv3.async.converters.CreatePDF
import com.dar.nclientv3.async.converters.CreateZIP
import com.dar.nclientv3.async.database.Queries
import com.dar.nclientv3.async.downloader.DownloadGalleryV2
import com.dar.nclientv3.async.downloader.DownloadObserver
import com.dar.nclientv3.async.downloader.DownloadQueue
import com.dar.nclientv3.async.downloader.GalleryDownloaderV2
import com.dar.nclientv3.components.classes.MultichoiceAdapter
import com.dar.nclientv3.settings.Global
import com.dar.nclientv3.utility.ImageDownloadUtility
import com.dar.nclientv3.utility.LogUtility
import com.dar.nclientv3.utility.Utility

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import java.io.File
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

class LocalAdapter(
    private val cont: LocalActivity,
    myDataset: ArrayList<LocalGallery>
) : MultichoiceAdapter<Any, RecyclerView.ViewHolder>(), Filterable {
    private val statuses = SparseIntArray()
    private val context: LocalActivity = cont
    private val dataset: List<LocalGallery> = CopyOnWriteArrayList(myDataset)
    private val galleryDownloaders: List<GalleryDownloaderV2> = DownloadQueue.getDownloaders()
    private val comparatorByName = Comparator { o1: Any, o2: Any ->
        if (o1 === o2) return@Comparator 0
        val b1 = o1 is LocalGallery
        val b2 = o2 is LocalGallery
        val s1 = if (b1) (o1 as LocalGallery).title else (o1 as GalleryDownloaderV2).pathTitle
        val s2 = if (b2) (o2 as LocalGallery).title else (o2 as GalleryDownloaderV2).pathTitle
        s1.compareTo(s2)
    }
    private val comparatorBySize = Comparator { o1: Any, o2: Any ->
        if (o1 === o2) return@Comparator 0
        val page1 = if (o1 is LocalGallery) Global.recursiveSize((o1 as LocalGallery).directory) else 0
        val page2 = if (o2 is LocalGallery) Global.recursiveSize((o2 as LocalGallery).directory) else 0
        page1.compareTo(page2)
    }
    private val comparatorByPageCount = Comparator { o1: Any, o2: Any ->
        if (o1 === o2) return@Comparator 0
        val page1 = if (o1 is LocalGallery) (o1 as LocalGallery).pageCount else 0
        val page2 = if (o2 is LocalGallery) (o2 as LocalGallery).pageCount else 0
        page1 - page2
    }
    private val comparatorByDate = Comparator { o1: Any, o2: Any ->
        if (o1 === o2) return@Comparator 0
        val b1 = o1 is LocalGallery
        val b2 = o2 is LocalGallery
        // downloading manga are newer
        val d1 = if (b1) (o1 as LocalGallery).directory.lastModified() else Long.MAX_VALUE
        val d2 = if (b2) (o2 as LocalGallery).directory.lastModified() else Long.MAX_VALUE
        if (d1 != d2) return@Comparator d1.compareTo(d2)
        comparatorByName.compare(o1, o2)
    }

    private var filter: List<Any> = ArrayList()

    private var lastQuery: String = ""
    private val observer = object : DownloadObserver {
        private fun updatePosition(downloader: GalleryDownloaderV2) {
            val id = filter.indexOf(downloader)
            if (id >= 0) cont.runOnUiThread { notifyItemChanged(id) }
        }

        override fun triggerStartDownload(downloader: GalleryDownloaderV2) {
            updatePosition(downloader)
        }

        override fun triggerUpdateProgress(downloader: GalleryDownloaderV2, reach: Int, total: Int) {
            updatePosition(downloader)
        }

        override fun triggerEndDownload(downloader: GalleryDownloaderV2) {
            val l = downloader.localGallery()
            galleryDownloaders.toMutableList().remove(downloader)
            if (l != null) {
                dataset.toMutableList().remove(l)
                dataset.toMutableList().add(l)
                LogUtility.d(l)
                sortElements()
            }
            cont.runOnUiThread { notifyItemRangeChanged(0, itemCount) }
        }

        override fun triggerCancelDownload(downloader: GalleryDownloaderV2) {
            removeDownloader(downloader)
        }

        override fun triggerPauseDownload(downloader: GalleryDownloaderV2) {
            cont.runOnUiThread { notifyItemChanged(filter.indexOf(downloader)) }
        }
    }
    private var colCount: Int = 0

    init {
        colCount = cont.colCount
        lastQuery = cont.query
        filter = MutableList<Any>(dataset.size + galleryDownloaders.size) { 0 }
        (filter as MutableList<Any>).addAll(galleryDownloaders)

        DownloadQueue.addObserver(observer)
        sortElements()
    }

    companion object {
        fun startGallery(context: Activity, directory: File) {
            if (!directory.isDirectory) return
            val ent = LocalGallery(directory)
            ent.calculateSizes()
            Thread {
                val intent = Intent(context, GalleryActivity::class.java)
                intent.putExtra(context.packageName + ".GALLERY", ent)
                intent.putExtra(context.packageName + ".ISLOCAL", true)
                context.runOnUiThread { context.startActivity(intent) }
            }.start()
        }
    }

    fun getMaster(holder: ViewHolder): ViewGroup {
        return holder.layout
    }

    override fun getItemAt(position: Int): Any {
        return filter[position]
    }

    override fun getMaster(holder: RecyclerView.ViewHolder?): ViewGroup {
        TODO("Not yet implemented")
    }

    private fun createHash(galleryDownloaders: List<GalleryDownloaderV2>, dataset: List<LocalGallery>): CopyOnWriteArrayList<Any> {
        val hashMap = HashMap<String, Any>(dataset.size + galleryDownloaders.size)
        for (gall in dataset) {
            if (gall.title.lowercase(Locale.US).contains(lastQuery))
                hashMap[gall.trueTitle] = gall
        }

        for (gall in galleryDownloaders) {
            if (gall.pathTitle.lowercase(Locale.US).contains(lastQuery))
                hashMap[gall.truePathTitle] = gall
        }

        val arr = ArrayList<Any>(hashMap.values)

        sortItems(arr)

        return CopyOnWriteArrayList(arr)
    }

    private fun sortItems(arr: ArrayList<Any>) {
        val type = Global.getLocalSortType()
        if (type.type == LocalSortType.Type.RANDOM) {
            arr.shuffle(Utility.RANDOM)
        } else {
            try {
                Collections.sort(arr, getComparator(type.type))
            } catch (ignore: IllegalArgumentException) {
            }
            if (type.descending) arr.reverse()
        }
    }

    private fun getComparator(type: LocalSortType.Type): Comparator<Any> {
        return when (type) {
            LocalSortType.Type.DATE -> comparatorByDate
            LocalSortType.Type.TITLE -> comparatorByName
            LocalSortType.Type.PAGE_COUNT -> comparatorByPageCount
            LocalSortType.Type.RANDOM -> comparatorByDate
        }
    }

    fun setColCount(colCount: Int) {
        this.colCount = colCount
    }

    private fun sortElements() {
        filter = createHash(galleryDownloaders, dataset)
    }

    override fun onCreateMultichoiceViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var id = 0
        when (viewType) {
            0 -> id = if (colCount == 1) R.layout.entry_layout_single else R.layout.entry_layout
            1 -> id = if (colCount == 1) R.layout.entry_download_layout else R.layout.entry_download_layout_compact
        }
        return ViewHolder(LayoutInflater.from(parent.context).inflate(id, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return if (filter[position] is LocalGallery) 0 else 1
    }

    private fun bindGallery(holder: ViewHolder, position: Int, ent: LocalGallery) {
        holder.flag?.visibility = View.GONE
        ImageDownloadUtility.loadImage(context, ent.getPage(ent.min), holder.imgView)
        holder.title.text = ent.title
        if (colCount == 1)
            holder.pages.text = context.getString(R.string.page_count_format, ent.pageCount)
        else holder.pages.text = String.format(Locale.US, "%d", ent.pageCount)

        holder.title.setOnClickListener {
            val layout = holder.title.layout
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (layout.getEllipsisCount(layout.lineCount - 1) > 0)
                    holder.title.maxLines = 7
                else if (holder.title.maxLines == 7) holder.title.maxLines = 3
                else holder.layout.performClick()
            } else holder.layout.performClick()
        }

        /*holder.layout.setOnLongClickListener {
            createContextualMenu(position)
            true
        }*/

        var statusColor = statuses.get(ent.id, 0)
        if (statusColor == 0) {
            statusColor = Queries.StatusMangaTable.getStatus(ent.id).color
            statuses.put(ent.id, statusColor)
        }
        holder.title.setBackgroundColor(statusColor)
    }

    fun updateColor(id: Int) {
        if (id < 0) return
        statuses.put(id, Queries.StatusMangaTable.getStatus(id).color)
        for (i in filter.indices) {
            val o = filter[i]
            if (o !is LocalGallery) continue
            val lg = o as LocalGallery
            if (lg.id == id) notifyItemChanged(i)
        }
    }

    override fun defaultMasterAction(position: Int) {
        if (position < 0 || filter.size <= position) return
        if (filter[position] !is LocalGallery) return
        val lg = filter[position] as LocalGallery
        startGallery(context, lg.directory)
        context.setIdGalleryPosition(lg.id)
    }

    override fun onBindMultichoiceViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        TODO("Not yet implemented")
    }

    private fun bindDownload(holder: ViewHolder, position: Int, downloader: GalleryDownloaderV2) {
        val percentage = downloader.percentage
        // if (!downloader.hasData())return;
        ImageDownloadUtility.loadImage(context, downloader.thumbnail, holder.imgView)
        holder.title.text = downloader.pathTitle
        holder.cancelButton.setOnClickListener { removeDownloader(downloader) }
        when (downloader.status) {
            GalleryDownloaderV2.Status.PAUSED -> {
                holder.playButton.setImageResource(R.drawable.ic_play)
                holder.playButton.setOnClickListener {
                    downloader.status = GalleryDownloaderV2.Status.NOT_STARTED
                    DownloadGalleryV2.startWork(context)
                    notifyItemChanged(position)
                }
            }

            GalleryDownloaderV2.Status.DOWNLOADING -> {
                holder.playButton.setImageResource(R.drawable.ic_pause)
                holder.playButton.setOnClickListener {
                    downloader.status = GalleryDownloaderV2.Status.PAUSED
                    notifyItemChanged(position)
                }
            }

            GalleryDownloaderV2.Status.NOT_STARTED -> {
                holder.playButton.setImageResource(R.drawable.ic_play)
                holder.playButton.setOnClickListener { DownloadQueue.givePriority(downloader) }
            }

            else -> {}
        }
        holder.progress.text = context.getString(R.string.percentage_format, percentage)
        holder.progress.visibility = if (downloader.status == GalleryDownloaderV2.Status.NOT_STARTED) View.GONE else View.VISIBLE
        holder.progressBar.progress = percentage
        holder.progressBar.isIndeterminate = downloader.status == GalleryDownloaderV2.Status.NOT_STARTED
        Global.setTint(holder.playButton.drawable)
        Global.setTint(holder.cancelButton.drawable)
    }

    private fun removeDownloader(downloader: GalleryDownloaderV2) {
        val position = filter.indexOf(downloader)
        if (position < 0) return
        filter.toMutableList().removeAt(position)
        DownloadQueue.remove(downloader, true)
        galleryDownloaders.toMutableList().remove(downloader)
        cont.runOnUiThread { notifyItemRemoved(position) }
    }

    override fun getItemId(position: Int): Long {
        return if (position == -1) -1 else filter[position].hashCode().toLong()
    }

    fun onBindMultichoiceViewHolder(holder: ViewHolder, position: Int) {
        if (filter[position] is LocalGallery)
            bindGallery(holder, position, filter[position] as LocalGallery)
        else
            bindDownload(holder, position, filter[position] as GalleryDownloaderV2)
    }

    private fun showDialogDelete() {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.delete_galleries).setMessage(getAllGalleries())
        builder.setPositiveButton(R.string.yes) { _, _ ->
            val coll = ArrayList(selected)
            for (o in coll) {
                filter.toMutableList().remove(o)
                if (o is LocalGallery) {
                    dataset.toMutableList().remove(o)
                    Global.recursiveDelete((o as LocalGallery).directory)
                } else if (o is GalleryDownloaderV2) {
                    DownloadQueue.remove(o, true)
                }
            }
            cont.runOnUiThread { notifyDataSetChanged() }
        }.setNegativeButton(R.string.no, null).setCancelable(true)
        builder.show()
    }

    private fun getAllGalleries(): String {
        val builder = StringBuilder()
        for (o in selected) {
            builder.append(if (o is LocalGallery) (o as LocalGallery).title else (o as GalleryDownloaderV2).title)
            builder.append('\n')
        }
        return builder.toString()
    }

    private fun showDialogPDF() {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.create_pdf).setMessage(getAllGalleries())
        builder.setPositiveButton(R.string.yes) { _, _ ->
            for (o in selected) {
                if (o !is LocalGallery) continue
                CreatePDF.startWork(context, o)
            }
        }.setNegativeButton(R.string.no, null).setCancelable(true)
        builder.show()
    }

    private fun showDialogZip() {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.create_zip).setMessage(getAllGalleries())
        builder.setPositiveButton(R.string.yes) { _, _ ->
            for (o in selected) {
                if (o !is LocalGallery) continue
                CreateZIP.startWork(context, o)
            }
        }.setNegativeButton(R.string.no, null).setCancelable(true)
        builder.show()
    }

    fun hasSelectedClass(c: Class<*>): Boolean {
        for (x in selected) if (x.javaClass == c) return true
        return false
    }

    override fun getItemCount(): Int {
        return filter.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults {
                val query = constraint.toString().lowercase(Locale.US)
                if (lastQuery == query) return FilterResults()
                val results = FilterResults()
                lastQuery = query
                results.values = createHash(galleryDownloaders, dataset)
                return results
            }

            override fun publishResults(constraint: CharSequence, results: FilterResults) {
                if (results.values != null) {
                    filter = results.values as CopyOnWriteArrayList<*>
                    cont.runOnUiThread { notifyDataSetChanged() }
                }
            }
        }
    }

    fun removeObserver() {
        DownloadQueue.removeObserver(observer)
    }

    fun viewRandom() {
        if (dataset.isEmpty()) return
        val x = Utility.RANDOM.nextInt(dataset.size)
        startGallery(context, dataset[x].directory)
    }

    fun sortChanged() {
        sortElements()
        cont.runOnUiThread { notifyItemRangeChanged(0, itemCount) }
    }

    fun startSelected() {
        for (o in selected) {
            if (o !is GalleryDownloaderV2) continue
            val d = o as GalleryDownloaderV2
            if (d.status == GalleryDownloaderV2.Status.PAUSED)
                d.status = GalleryDownloaderV2.Status.NOT_STARTED
        }
        cont.runOnUiThread { notifyDataSetChanged() }
    }

    fun pauseSelected() {
        for (o in selected) {
            if (o !is GalleryDownloaderV2) continue
            val d = o as GalleryDownloaderV2
            d.status = GalleryDownloaderV2.Status.PAUSED
        }
        cont.runOnUiThread { notifyDataSetChanged() }
    }

    fun deleteSelected() {
        showDialogDelete()
    }

    fun zipSelected() {
        showDialogZip()
    }

    fun pdfSelected() {
        showDialogPDF()
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imgView: ImageView = v.findViewById(R.id.image)
        val overlay: View = v.findViewById(R.id.overlay)
        val title: TextView = v.findViewById(R.id.title)
        val pages: TextView = v.findViewById(R.id.pages)
        val flag: TextView? = v.findViewById(R.id.flag)
        val progress: TextView = itemView.findViewById(R.id.progress)
        val layout: ViewGroup = v.findViewById(R.id.master_layout)
        val playButton: ImageButton = itemView.findViewById(R.id.playButton)
        val cancelButton: ImageButton = itemView.findViewById(R.id.cancelButton)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)

        init {
            Global.setTint(playButton.drawable)
            Global.setTint(cancelButton.drawable)
        }
    }
}
