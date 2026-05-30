package com.example.scanmarker.batch

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scanmarker.R

class BatchPhotoAdapter(
    private var items: List<BatchItem>,
    private val onItemClick: (BatchItem) -> Unit,
    private val onCheckChanged: (BatchItem, Boolean) -> Unit
) : RecyclerView.Adapter<BatchPhotoAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<String>()
    private var templateIndex: Int = -1

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
        val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
        val studentNameText: TextView = itemView.findViewById(R.id.studentNameText)
        val studentIdText: TextView = itemView.findViewById(R.id.studentIdText)
        val classText: TextView = itemView.findViewById(R.id.classText)
        val statusText: TextView = itemView.findViewById(R.id.statusText)
        val templateBadge: ImageView = itemView.findViewById(R.id.templateBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_batch_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.checkbox.isChecked = selectedItems.contains(item.id)
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedItems.add(item.id)
            } else {
                selectedItems.remove(item.id)
            }
            onCheckChanged(item, isChecked)
        }

        item.thumbnailPath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            holder.thumbnail.setImageBitmap(bitmap)
        } ?: run {
            holder.thumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.studentNameText.text = if (item.studentName.isNotEmpty()) item.studentName else "未填写姓名"
        holder.studentIdText.text = "学号: ${if (item.studentId.isNotEmpty()) item.studentId else "未填写"}"
        holder.classText.text = "班级: ${if (item.classInfo.isNotEmpty()) item.classInfo else "未填写"}"
        
        holder.statusText.text = if (item.isProcessed) "已处理" else "未处理"
        holder.statusText.setTextColor(
            if (item.isProcessed) 
                holder.itemView.context.getColor(android.R.color.holo_green_light)
            else 
                holder.itemView.context.getColor(android.R.color.darker_gray)
        )

        holder.templateBadge.visibility = if (position == templateIndex) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<BatchItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setTemplateIndex(index: Int) {
        val oldIndex = templateIndex
        templateIndex = index
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (index >= 0) notifyItemChanged(index)
    }

    fun getSelectedItems(): Set<String> = selectedItems.toSet()

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }
}
