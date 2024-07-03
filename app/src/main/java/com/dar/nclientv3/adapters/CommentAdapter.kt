package com.dar.nclientv3.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.dar.nclientv3.R
import com.dar.nclientv3.api.comments.Comment
import com.dar.nclientv3.settings.AuthRequest
import com.dar.nclientv3.settings.Global
import com.dar.nclientv3.settings.Login
import com.dar.nclientv3.utility.ImageDownloadUtility
import com.dar.nclientv3.utility.Utility
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.text.DateFormat
import java.util.*

class CommentAdapter(
    private val context: AppCompatActivity,
    comments: List<Comment>?,
    private val galleryId: Int
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    private val comments: MutableList<Comment> = comments?.toMutableList() ?: mutableListOf()
    private val format: DateFormat = android.text.format.DateFormat.getDateFormat(context)
    private val userId: Int = if (Login.isLogged() && Login.getUser() != null) Login.getUser().id else -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val position = holder.bindingAdapterPosition
        val c = comments[position]
        holder.layout.setOnClickListener {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                context.runOnUiThread {
                    holder.body.maxLines = if (holder.body.maxLines == 7) 999 else 7
                }
            }
        }
        holder.close.visibility = if (c.posterId != userId) View.GONE else View.VISIBLE
        holder.user.text = c.username
        holder.body.text = c.comment
        holder.date.text = format.format(c.postDate)
        holder.close.setOnClickListener {
            val refererUrl = String.format(Locale.US, "${Utility.getBaseUrl()}g/%d/", galleryId)
            val submitUrl = String.format(Locale.US, "${Utility.getBaseUrl()}api/comments/%d/delete", c.id)
            AuthRequest(refererUrl, submitUrl, object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                override fun onResponse(call: Call, response: Response) {
                    if (response.body()?.string()?.contains("true") == true) {
                        comments.removeAt(position)
                        context.runOnUiThread { notifyItemRemoved(position) }
                    }
                }
            }).setMethod("POST", AuthRequest.EMPTY_BODY).start()
        }
        if (c.avatarUrl == null || Global.getDownloadPolicy() != Global.DataUsageType.FULL) {
            ImageDownloadUtility.loadImage(R.drawable.ic_person, holder.userImage)
        } else {
            ImageDownloadUtility.loadImage(context, c.avatarUrl, holder.userImage)
        }
    }

    override fun getItemCount(): Int = comments.size

    fun addComment(c: Comment) {
        comments.add(0, c)
        context.runOnUiThread { notifyItemInserted(0) }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val userImage: ImageButton = v.findViewById(R.id.propic)
        val close: ImageButton = v.findViewById(R.id.close)
        val user: TextView = v.findViewById(R.id.username)
        val body: TextView = v.findViewById(R.id.body)
        val date: TextView = v.findViewById(R.id.date)
        val layout: ConstraintLayout = v.findViewById(R.id.master_layout)
    }
}
