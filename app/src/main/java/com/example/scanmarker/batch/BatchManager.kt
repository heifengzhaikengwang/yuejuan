package com.example.scanmarker.batch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BatchManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "batch_data"
        private const val KEY_CURRENT_BATCH = "current_batch"
        private const val KEY_BATCH_HISTORY = "batch_history"
        private const val BATCH_DIR = "batch_photos"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun createBatch(): BatchData {
        val batch = BatchData(
            batchId = generateBatchId(),
            createdTime = System.currentTimeMillis()
        )
        saveBatch(batch)
        return batch
    }

    fun getCurrentBatch(): BatchData? {
        val jsonStr = prefs.getString(KEY_CURRENT_BATCH, null) ?: return null
        return try {
            json.decodeFromString<BatchData>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    fun saveBatch(batch: BatchData) {
        val jsonStr = json.encodeToString(batch)
        prefs.edit().putString(KEY_CURRENT_BATCH, jsonStr).apply()
    }

    fun addPhotoToBatch(imagePath: String): BatchItem {
        val batch = getCurrentBatch() ?: createBatch()
        
        val thumbnailPath = generateThumbnail(imagePath)
        
        val newItem = BatchItem(
            imagePath = imagePath,
            thumbnailPath = thumbnailPath,
            addedTime = System.currentTimeMillis()
        )
        
        val updatedBatch = batch.copy(
            items = batch.items + newItem
        )
        saveBatch(updatedBatch)
        
        return newItem
    }

    fun addMultiplePhotos(imagePaths: List<String>): List<BatchItem> {
        return imagePaths.map { path -> addPhotoToBatch(path) }
    }

    fun removeItem(itemId: String): Boolean {
        val batch = getCurrentBatch() ?: return false
        
        val updatedBatch = batch.copy(
            items = batch.items.filter { it.id != itemId }
        )
        saveBatch(updatedBatch)
        return true
    }

    fun updateItemStudentInfo(itemId: String, studentId: String, studentName: String, classInfo: String): Boolean {
        val batch = getCurrentBatch() ?: return false
        
        val updatedItems = batch.items.map { item ->
            if (item.id == itemId) {
                item.copy(
                    studentId = studentId,
                    studentName = studentName,
                    classInfo = classInfo
                )
            } else item
        }
        
        val updatedBatch = batch.copy(items = updatedItems)
        saveBatch(updatedBatch)
        return true
    }

    fun batchUpdateStudentInfo(items: List<Pair<String, BatchItem.StudentInfo>>): Boolean {
        val batch = getCurrentBatch() ?: return false
        
        val itemMap = items.toMap()
        val updatedItems = batch.items.map { item ->
            itemMap[item.id]?.let { info ->
                item.copy(
                    studentId = info.studentId,
                    studentName = info.studentName,
                    classInfo = info.classInfo
                )
            } ?: item
        }
        
        val updatedBatch = batch.copy(items = updatedItems)
        saveBatch(updatedBatch)
        return true
    }

    fun setTemplateIndex(index: Int): Boolean {
        val batch = getCurrentBatch() ?: return false
        if (index < 0 || index >= batch.items.size) return false
        
        val updatedBatch = batch.copy(templateIndex = index)
        saveBatch(updatedBatch)
        return true
    }

    fun setCropBoxes(boxes: List<CropBox>): Boolean {
        val batch = getCurrentBatch() ?: return false
        val updatedBatch = batch.copy(cropBoxes = boxes)
        saveBatch(updatedBatch)
        return true
    }

    fun markItemProcessed(itemId: String, processedPath: String): Boolean {
        val batch = getCurrentBatch() ?: return false
        
        val updatedItems = batch.items.map { item ->
            if (item.id == itemId) {
                item.copy(isProcessed = true, processedPath = processedPath)
            } else item
        }
        
        val processedCount = updatedItems.count { it.isProcessed }
        val updatedBatch = batch.copy(
            items = updatedItems,
            isProcessed = processedCount == updatedItems.size,
            processedCount = processedCount
        )
        saveBatch(updatedBatch)
        return true
    }

    fun clearBatch() {
        val batch = getCurrentBatch() ?: return
        
        batch.items.forEach { item ->
            File(item.imagePath).delete()
            item.thumbnailPath?.let { File(it).delete() }
            item.processedPath?.let { File(it).delete() }
        }
        
        prefs.edit().remove(KEY_CURRENT_BATCH).apply()
    }

    private fun generateBatchId(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "batch_${dateFormat.format(Date())}"
    }

    private fun generateThumbnail(imagePath: String): String? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)

            val targetSize = 200
            options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
            options.inJustDecodeBounds = false

            val bitmap = BitmapFactory.decodeFile(imagePath, options) ?: return null
            
            val thumbnail: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ThumbnailUtils.extractThumbnail(bitmap, targetSize, targetSize, 
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT)
            } else {
                Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
            }

            val batchDir = File(context.getExternalFilesDir(null), BATCH_DIR)
            if (!batchDir.exists()) batchDir.mkdirs()

            val thumbnailFile = File(batchDir, "thumb_${System.currentTimeMillis()}.jpg")
            FileOutputStream(thumbnailFile).use { fos ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            }

            if (bitmap != thumbnail) bitmap.recycle()
            thumbnail.recycle()

            thumbnailFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    data class StudentInfo(
        val studentId: String = "",
        val studentName: String = "",
        val classInfo: String = ""
    )
}
