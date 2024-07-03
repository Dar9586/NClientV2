package com.dar.nclientv3.adapters

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.dar.nclientv3.R
import com.dar.nclientv3.components.status.Status
import com.dar.nclientv3.components.status.StatusManager
import com.dar.nclientv3.settings.Global
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import yuku.ambilwarna.AmbilWarnaDialog
import java.util.*

class StatusManagerAdapter(private val activity: Activity) : RecyclerView.Adapter<StatusManagerAdapter.ViewHolder>() {
    private var statusList: MutableList<Status> = StatusManager.toList()
    private var newColor: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val resId = R.layout.entry_status
        val view = LayoutInflater.from(activity).inflate(resId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder.bindingAdapterPosition == statusList.size) {
            holder.name.setText(R.string.add)
            holder.color.visibility = View.INVISIBLE
            holder.color.setBackgroundColor(Color.TRANSPARENT)
            holder.cancel.setImageResource(R.drawable.ic_add)
            Global.setTint(holder.cancel.drawable)
            holder.cancel.setOnClickListener(null)
            holder.master.setOnClickListener { updateStatus(null) }
            return
        }
        val status = statusList[holder.bindingAdapterPosition]
        holder.name.text = status.name
        holder.color.visibility = View.VISIBLE
        holder.color.setBackgroundColor(status.opaqueColor())

        holder.cancel.setImageResource(R.drawable.ic_close)
        holder.master.setOnClickListener { updateStatus(status) }
        holder.cancel.setOnClickListener {
            StatusManager.remove(status)
            notifyItemRemoved(statusList.indexOf(status))
            statusList.remove(status)
        }
    }

    override fun getItemCount(): Int {
        return statusList.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == statusList.size) 1 else 0
    }

    private fun updateStatus(status: Status?) {
        val builder = MaterialAlertDialogBuilder(activity)
        val layout = View.inflate(activity, R.layout.dialog_add_status, null) as LinearLayout
        val name = layout.findViewById<EditText>(R.id.name)
        val btnColor = layout.findViewById<Button>(R.id.color)
        val color = status?.opaqueColor() ?: (Random().nextInt() or 0xff000000.toInt())
        newColor = color
        btnColor.setBackgroundColor(color)
        name.setText(status?.name ?: "")
        btnColor.setOnClickListener {
            AmbilWarnaDialog(activity, color, false, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog?) {}

                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    if (color == Color.WHITE || color == Color.BLACK) {
                        Toast.makeText(activity, R.string.invalid_color_selected, Toast.LENGTH_SHORT).show()
                        return
                    }
                    newColor = color
                    btnColor.setBackgroundColor(color)
                }
            }).show()
        }
        builder.setView(layout)
        builder.setTitle(status?.let { R.string.update_status } ?: R.string.create_new_status)
        builder.setPositiveButton(R.string.ok) { _, _ ->
            val newName = name.text.toString()
            if (newName.length < 2) {
                Toast.makeText(activity, R.string.name_too_short, Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (StatusManager.getByName(newName) != null && newName != status?.name) {
                Toast.makeText(activity, R.string.duplicated_name, Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            val newStatus = StatusManager.updateStatus(status, name.text.toString(), newColor)
            if (status == null) {
                statusList.add(newStatus)
                statusList.sortWith { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) }
                val index = statusList.indexOf(newStatus)
                notifyItemInserted(index)
            } else {
                val oldIndex = statusList.indexOf(status)
                statusList[oldIndex] = newStatus
                statusList.sortWith { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) }
                val newIndex = statusList.indexOf(newStatus)
                notifyItemMoved(oldIndex, newIndex)
                notifyItemChanged(newIndex)
            }
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val master: LinearLayout = itemView.findViewById(R.id.master_layout)
        val color: Button = itemView.findViewById(R.id.color)
        val cancel: ImageButton = itemView.findViewById(R.id.cancelButton)
        val name: TextView = itemView.findViewById(R.id.name)
    }
}
