package com.example.scanmarker.batch

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scanmarker.R
import com.example.scanmarker.CropConfigActivity

class BatchManagerActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var selectTemplateButton: Button
    private lateinit var editStudentButton: Button
    private lateinit var deleteSelectedButton: Button
    private lateinit var processButton: Button

    private lateinit var batchManager: BatchManager
    private lateinit var adapter: BatchPhotoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_manager)

        batchManager = BatchManager(this)

        initViews()
        setupRecyclerView()
        setupListeners()
        loadBatchData()
    }

    private fun initViews() {
        titleText = findViewById(R.id.titleText)
        photosRecyclerView = findViewById(R.id.photosRecyclerView)
        selectTemplateButton = findViewById(R.id.selectTemplateButton)
        editStudentButton = findViewById(R.id.editStudentButton)
        deleteSelectedButton = findViewById(R.id.deleteSelectedButton)
        processButton = findViewById(R.id.processButton)
    }

    private fun setupRecyclerView() {
        adapter = BatchPhotoAdapter(
            items = emptyList(),
            onItemClick = { item -> showItemOptions(item) },
            onCheckChanged = { _, _ -> updateButtonStates() }
        )

        photosRecyclerView.layoutManager = LinearLayoutManager(this)
        photosRecyclerView.adapter = adapter
    }

    private fun setupListeners() {
        selectTemplateButton.setOnClickListener {
            selectTemplate()
        }

        editStudentButton.setOnClickListener {
            showBatchStudentInfoDialog()
        }

        deleteSelectedButton.setOnClickListener {
            deleteSelected()
        }

        processButton.setOnClickListener {
            processBatch()
        }
    }

    private fun loadBatchData() {
        val batch = batchManager.getCurrentBatch()
        if (batch != null) {
            adapter.setItems(batch.items)
            adapter.setTemplateIndex(batch.templateIndex)
            titleText.text = "批次管理 (${batch.items.size} 张)"
        } else {
            titleText.text = "批次管理 (0 张)"
        }
        updateButtonStates()
    }

    private fun updateButtonStates() {
        val hasSelection = adapter.getSelectedItems().isNotEmpty()
        deleteSelectedButton.isEnabled = hasSelection
        deleteSelectedButton.text = if (hasSelection) "删除选中 (${adapter.getSelectedItems().size})" else "删除选中"
    }

    private fun showItemOptions(item: BatchItem) {
        val options = arrayOf("查看大图", "设为模板", "编辑学生信息", "删除")
        
        AlertDialog.Builder(this)
            .setTitle(item.studentName.ifEmpty { "图片选项" })
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showFullImage(item)
                    1 -> setAsTemplate(item)
                    2 -> showStudentInfoDialog(item)
                    3 -> deleteItem(item)
                }
            }
            .show()
    }

    private fun showFullImage(item: BatchItem) {
        Toast.makeText(this, "图片路径: ${item.imagePath}", Toast.LENGTH_LONG).show()
    }

    private fun setAsTemplate(item: BatchItem) {
        val batch = batchManager.getCurrentBatch() ?: return
        val index = batch.items.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            batchManager.setTemplateIndex(index)
            adapter.setTemplateIndex(index)
            Toast.makeText(this, "已设为模板", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectTemplate() {
        val selected = adapter.getSelectedItems()
        if (selected.size != 1) {
            Toast.makeText(this, "请选择一张图片作为模板", Toast.LENGTH_SHORT).show()
            return
        }

        val batch = batchManager.getCurrentBatch() ?: return
        val itemId = selected.first()
        val index = batch.items.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            batchManager.setTemplateIndex(index)
            adapter.setTemplateIndex(index)
            Toast.makeText(this, "已设为模板", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showStudentInfoDialog(item: BatchItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_student_info, null)
        
        val studentIdEdit = dialogView.findViewById<android.widget.EditText>(R.id.editStudentId)
        val studentNameEdit = dialogView.findViewById<android.widget.EditText>(R.id.editStudentName)
        val classEdit = dialogView.findViewById<android.widget.EditText>(R.id.editClass)

        studentIdEdit.setText(item.studentId)
        studentNameEdit.setText(item.studentName)
        classEdit.setText(item.classInfo)

        AlertDialog.Builder(this)
            .setTitle("编辑学生信息")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                batchManager.updateItemStudentInfo(
                    item.id,
                    studentIdEdit.text.toString(),
                    studentNameEdit.text.toString(),
                    classEdit.text.toString()
                )
                loadBatchData()
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showBatchStudentInfoDialog() {
        val selected = adapter.getSelectedItems()
        if (selected.isEmpty()) {
            Toast.makeText(this, "请先选择要编辑的图片", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_student_info, null)
        
        val studentIdEdit = dialogView.findViewById<android.widget.EditText>(R.id.editStudentId)
        val studentNameEdit = dialogView.findViewById<android.widget.EditText>(R.id.editStudentName)
        val classEdit = dialogView.findViewById<android.widget.EditText>(R.id.editClass)

        AlertDialog.Builder(this)
            .setTitle("批量设置学生信息 (${selected.size} 张)")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val batch = batchManager.getCurrentBatch() ?: return@setPositiveButton
                val updates = mutableListOf<Pair<String, Triple<String, String, String>>>()
                
                selected.forEach { itemId ->
                    val item = batch.items.find { it.id == itemId }
                    item?.let {
                        updates.add(itemId to Triple(
                            studentIdEdit.text.toString(),
                            studentNameEdit.text.toString(),
                            classEdit.text.toString()
                        ))
                    }
                }
                
                batchManager.batchUpdateStudentInfo(updates)
                loadBatchData()
                adapter.clearSelection()
                Toast.makeText(this, "已保存 ${updates.size} 条", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteItem(item: BatchItem) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除这张图片吗？")
            .setPositiveButton("删除") { _, _ ->
                batchManager.removeItem(item.id)
                loadBatchData()
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteSelected() {
        val selected = adapter.getSelectedItems().toList()
        if (selected.isEmpty()) {
            Toast.makeText(this, "请先选择要删除的图片", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除选中的 ${selected.size} 张图片吗？")
            .setPositiveButton("删除") { _, _ ->
                selected.forEach { itemId ->
                    batchManager.removeItem(itemId)
                }
                loadBatchData()
                adapter.clearSelection()
                Toast.makeText(this, "已删除 ${selected.size} 张", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun processBatch() {
        val batch = batchManager.getCurrentBatch()
        if (batch == null || batch.items.isEmpty()) {
            Toast.makeText(this, "请先添加图片", Toast.LENGTH_SHORT).show()
            return
        }

        if (batch.templateIndex < 0) {
            Toast.makeText(this, "请先选择一张图片作为模板", Toast.LENGTH_SHORT).show()
            return
        }

        startActivity(Intent(this, BatchProcessorActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        loadBatchData()
    }
}
