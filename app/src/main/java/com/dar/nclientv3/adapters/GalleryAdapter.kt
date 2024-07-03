package com.dar.nclientv3.adapters

import android.content.Intent
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.util.set
import androidx.recyclerview.widget.RecyclerView
import com.dar.nclientv3.*
import com.dar.nclientv3.api.components.*
import com.dar.nclientv3.api.enums.SpecialTagIds
import com.dar.nclientv3.api.enums.TagType
import com.dar.nclientv3.api.local.LocalGallery
import com.dar.nclientv3.async.database.Queries
import com.dar.nclientv3.components.classes.Size
import com.dar.nclientv3.components.widgets.CustomGridLayoutManager
import com.dar.nclientv3.files.GalleryFolder
import com.dar.nclientv3.github.chrisbanes.photoview.PhotoView
import com.dar.nclientv3.settings.Global
import com.dar.nclientv3.utility.ImageDownloadUtility
import com.dar.nclientv3.utility.LogUtility
import com.dar.nclientv3.utility.Utility
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.util.*

class GalleryAdapter(
    private val context: GalleryActivity,
    private val gallery: GenericGallery,
    colCount: Int
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    companion object {
        private val TAG_NAMES = intArrayOf(
            R.string.unknown,
            R.string.tag_parody_gallery,
            R.string.tag_character_gallery,
            R.string.tag_tag_gallery,
            R.string.tag_artist_gallery,
            R.string.tag_group_gallery,
            R.string.tag_language_gallery,
            R.string.tag_category_gallery
        )
        private const val TOLERANCE = 1000
    }

    private val maxSize: Size = gallery.maxSize
    private val minSize: Size = gallery.minSize
    private val angles = SparseIntArray()
    var directory: GalleryFolder? = null
    private var maxImageSize: Size? = null
    private lateinit var policy: Policy
    private var colCount: Int = 0

    init {
        setColCount(colCount)
        try {
            when {
                gallery is LocalGallery -> directory = gallery.galleryFolder
                Global.hasStoragePermission(context) -> {
                    if (gallery.id != -1) {
                        val f = Global.findGalleryFolder(context, gallery.id)
                        if (f != null) directory = GalleryFolder(f)
                    } else {
                        directory = GalleryFolder(gallery.title)
                    }
                }
            }
        } catch (ignore: IllegalArgumentException) {
            directory = null
        }
        LogUtility.d("Max maxSize: $maxSize, min maxSize: ${gallery.minSize}")
    }

    fun positionToType(pos: Int): Type = when {
        pos == 0 -> Type.TAG
        pos > gallery.pageCount -> Type.RELATED
        else -> Type.PAGE
    }

    fun setColCount(colCount: Int) {
        this.colCount = colCount
        applyProportionPolicy()
    }

    private fun applyProportionPolicy() {
        policy = when {
            colCount == 1 -> Policy.FULL
            maxSize.height - minSize.height < TOLERANCE -> Policy.MAX
            else -> Policy.PROPORTION
        }
        LogUtility.d("NEW POLICY: $policy")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val id = when (Type.entries[viewType]) {
            Type.TAG -> R.layout.tags_layout
            Type.PAGE -> when (policy) {
                Policy.MAX -> R.layout.image_void
                Policy.FULL -> R.layout.image_void_full
                Policy.PROPORTION -> R.layout.image_void_static
            }

            Type.RELATED -> R.layout.related_recycler
        }
        return ViewHolder(LayoutInflater.from(parent.context).inflate(id, parent, false), Type.entries[viewType])
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (positionToType(holder.bindingAdapterPosition)) {
            Type.TAG -> loadTagLayout(holder)
            Type.PAGE -> loadPageLayout(holder)
            Type.RELATED -> loadRelatedLayout(holder)
        }
    }

    private fun loadRelatedLayout(holder: ViewHolder) {
        LogUtility.d("Called RElated")
        val recyclerView: RecyclerView = holder.master.findViewById(R.id.recycler)
        if (gallery.isLocal) {
            holder.master.visibility = View.GONE
            return
        }
        val gallery = this.gallery as Gallery
        if (!gallery.isRelatedLoaded || gallery.related.isEmpty()) {
            holder.master.visibility = View.GONE
            return
        } else {
            holder.master.visibility = View.VISIBLE
        }
        recyclerView.layoutManager = CustomGridLayoutManager(context, 1, RecyclerView.HORIZONTAL, false)
        if (gallery.isRelatedLoaded) {
            val adapter = ListAdapter(context)
            adapter.addGalleries((ArrayList(gallery.related) as List<GenericGallery>?)!!)
            recyclerView.adapter = adapter
        }
    }

    private fun loadTagLayout(holder: ViewHolder) {
        val vg: ViewGroup = holder.master.findViewById(R.id.tag_master)
        val idContainer: TextView = holder.master.findViewById(R.id.id_num)
        initializeIdContainer(idContainer)

        if (!hasTags()) {
            val layoutParams: ViewGroup.LayoutParams = vg.layoutParams
            layoutParams.height = 0
            vg.layoutParams = layoutParams
            return
        }

        val inflater: LayoutInflater = context.layoutInflater

        val tagList: TagList = gallery.galleryData.tags
        for (type in TagType.values) {
            val idStringTagName = TAG_NAMES[type.id.toInt()]
            val tagCount = tagList.getCount(type)
            val lay: ViewGroup = vg.getChildAt(type.id.toInt()) as ViewGroup
            val cg: ChipGroup = lay.findViewById(R.id.chip_group)
            if (cg.childCount != 0) continue
            lay.visibility = if (tagCount == 0) View.GONE else View.VISIBLE
            lay.findViewById<TextView>(R.id.title).text = idStringTagName.toString()
            for (a in 0 until tagCount) {
                val tag: Tag = tagList.getTag(type, a)
                val c: Chip = inflater.inflate(R.layout.chip_layout, cg, false) as Chip
                c.text = tag.name
                c.setOnClickListener {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra(context.packageName + ".TAG", tag)
                        putExtra(context.packageName + ".ISBYTAG", true)
                    }
                    context.startActivity(intent)
                }
                c.setOnLongClickListener {
                    CopyToClipboardActivity.copyTextToClipboard(context, tag.name)
                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                    true
                }
                cg.addView(c)
            }
            addInfoLayout(holder, gallery.galleryData)
        }
    }

    private fun initializeIdContainer(idContainer: TextView) {
        if (gallery.id <= 0) {
            idContainer.visibility = View.GONE
            return
        }
        val id = gallery.id.toString()
        idContainer.text = id
        idContainer.visibility = if (gallery.id.toShort() != SpecialTagIds.INVALID_ID) View.VISIBLE else View.GONE
        idContainer.setOnClickListener {
            CopyToClipboardActivity.copyTextToClipboard(context, id)
            context.runOnUiThread {
                Toast.makeText(context, R.string.id_copied_to_clipboard, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addInfoLayout(holder: ViewHolder, gallery: GalleryData) {
        var text = holder.master.findViewById<TextView>(R.id.page_count)
        text.text = context.getString(R.string.page_count_format, gallery.pageCount)
        text = holder.master.findViewById(R.id.upload_date)
        text.text = context.getString(
            R.string.upload_date_format,
            android.text.format.DateFormat.getDateFormat(context).format(gallery.uploadDate),
            android.text.format.DateFormat.getTimeFormat(context).format(gallery.uploadDate)
        )
        text = holder.master.findViewById(R.id.favorite_count)
        text.text = context.getString(R.string.favorite_count_format, gallery.favoriteCount)
    }

    fun setMaxImageSize(maxImageSize: Size) {
        this.maxImageSize = maxImageSize
        context.runOnUiThread { notifyItemRangeChanged(0, itemCount) }
    }

    private fun loadPageLayout(holder: ViewHolder) {
        val pos = holder.bindingAdapterPosition
        val imgView = holder.master.findViewById<ImageView>(R.id.image)

        imgView.setOnClickListener { startGallery(holder.bindingAdapterPosition) }
        imgView.setOnLongClickListener(null)
        holder.master.setOnClickListener { startGallery(holder.bindingAdapterPosition) }
        holder.master.setOnLongClickListener(null)

        holder.pageNumber!!.text = String.format(Locale.US, "%d", pos)

        if (policy == Policy.MAX) {
            holder.itemView.post {
                if (maxImageSize != null) return@post
                val cellWidth = holder.itemView.width
                if (maxSize.width > 10 && maxSize.height > 10) {
                    val hei = (maxSize.height * cellWidth) / maxSize.width
                    if (hei >= 100) setMaxImageSize(Size(cellWidth, hei))
                }
            }
        }

        if (policy == Policy.MAX && maxImageSize != null) {
            val params = imgView.layoutParams
            params.height = maxImageSize!!.height
            params.width = maxImageSize!!.width
            imgView.layoutParams = params
        }

        if (policy == Policy.FULL) {
            val photoView = imgView as PhotoView
            photoView.isZoomable = Global.isZoomOneColumn()

            photoView.setOnMatrixChangeListener {
                if (photoView.scale <= 1f) {
                    photoView.setAllowParentInterceptOnEdge(photoView.scale <= 1f)
                }
            }

            photoView.setOnClickListener {
                if (photoView.scale <= 1f) startGallery(holder.bindingAdapterPosition)
            }
            val listener = View.OnLongClickListener {
                optionDialog(imgView, pos)
                true
            }
            imgView.setOnLongClickListener(listener)
            holder.master.setOnLongClickListener(listener)
        }

        loadImageOnPolicy(imgView, pos)
    }

    private fun optionDialog(imgView: ImageView, pos: Int) {
        val items = arrayOf(
            context.getString(R.string.share),
            context.getString(R.string.rotate_image),
            context.getString(R.string.bookmark_here)
        )
        if (Global.hasStoragePermission(context)) items.plus(context.getString(R.string.save_page))

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings)
            .setIcon(R.drawable.ic_share)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> openSendImageDialog(imgView, pos)
                    1 -> rotate(pos)
                    2 -> Queries.ResumeTable.insert(gallery.id, pos)
                    3 -> {
                        val name = String.format(Locale.US, "%d-%d.jpg", gallery.id, pos)
                        Utility.saveImage(imgView.drawable, File(Global.SCREENFOLDER, name))
                    }
                }
            }
            .show()
    }

    private fun openSendImageDialog(img: ImageView, pos: Int) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.send_with_title)
            .setMessage(R.string.caption_send_with_title)
            .setPositiveButton(R.string.yes) { _, _ -> sendImage(img, pos, true) }
            .setNegativeButton(R.string.no) { _, _ -> sendImage(img, pos, false) }
            .setCancelable(true)
            .show()
    }

    private fun sendImage(img: ImageView, pos: Int, text: Boolean) {
        Utility.sendImage(context, img.drawable, if (text) gallery.sharePageUrl(pos - 1) else null)
    }

    private fun rotate(pos: Int) {
        angles[pos] = (angles[pos] + 270) % 360
        context.runOnUiThread { notifyItemChanged(pos) }
    }

    private fun startGallery(page: Int) {
        if (!gallery.isLocal && Global.getDownloadPolicy() == Global.DataUsageType.NONE) {
            context.runOnUiThread {
                Toast.makeText(context, R.string.enable_network_to_continue, Toast.LENGTH_SHORT).show()
            }
            return
        }
        val intent = Intent(context, ZoomActivity::class.java).apply {
            putExtra(context.packageName + ".GALLERY", gallery)
            putExtra(context.packageName + ".DIRECTORY", directory)
            putExtra(context.packageName + ".PAGE", page)
        }
        context.startActivity(intent)
    }

    private fun loadImageOnPolicy(imgView: ImageView, pos: Int) {
        val file = directory?.getPage(pos)
        val angle = angles[pos]

        if (policy == Policy.FULL) {
            if (file != null && file.exists()) {
                ImageDownloadUtility.loadImageOp(context, imgView, file, angle)
            } else if (!gallery.isLocal) {
                val ent = gallery as Gallery
                ImageDownloadUtility.loadImageOp(context, imgView, ent, pos - 1, angle)
            } else {
                ImageDownloadUtility.loadImage(R.mipmap.ic_launcher, imgView)
            }
        } else {
            if (file != null && file.exists()) {
                ImageDownloadUtility.loadImage(context, file, imgView)
            } else if (!gallery.isLocal) {
                val ent = gallery as Gallery
                ImageDownloadUtility.downloadPage(context, imgView, ent, pos - 1, false)
            } else {
                ImageDownloadUtility.loadImage(R.mipmap.ic_launcher, imgView)
            }
        }
    }

    private fun hasTags(): Boolean {
        return gallery.hasGalleryData()
    }

    override fun getItemViewType(position: Int): Int {
        return positionToType(position).ordinal
    }

    override fun getItemCount(): Int {
        return gallery.pageCount + 2
    }

    enum class Type { TAG, PAGE, RELATED }
    enum class Policy { PROPORTION, MAX, FULL }

    class ViewHolder(v: View, type: Type) : RecyclerView.ViewHolder(v) {
        val master: View = v.findViewById(R.id.master)
        val pageNumber: TextView? = v.findViewById(R.id.page_number)

        init {
            if (Global.useRtl()) v.rotationY = 180f
            if (type == Type.RELATED) Global.applyFastScroller(master.findViewById(R.id.recycler))
        }
    }
}
