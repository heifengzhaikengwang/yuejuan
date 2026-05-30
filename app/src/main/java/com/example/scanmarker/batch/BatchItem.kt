package com.example.scanmarker.batch

import android.graphics.RectF
import com.example.scanmarker.MarkActivity
import kotlinx.serialization.Serializable

@Serializable
data class BatchItem(
    val id: String = System.currentTimeMillis().toString(),
    val imagePath: String,
    val studentId: String = "",
    val studentName: String = "",
    val classInfo: String = "",
    val isProcessed: Boolean = false,
    val processedPath: String? = null,
    val thumbnailPath: String? = null,
    val addedTime: Long = System.currentTimeMillis()
)
