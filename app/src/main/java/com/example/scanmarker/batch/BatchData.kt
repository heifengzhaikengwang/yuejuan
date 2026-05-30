package com.example.scanmarker.batch

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.scanmarker.CropBox
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f

@Serializable
data class BatchData(
    val batchId: String = System.currentTimeMillis().toString(),
    val items: List<BatchItem> = emptyList(),
    val templateIndex: Int = -1,
    val cropBoxes: List<CropBox> = emptyList(),
    val createdTime: Long = System.currentTimeMillis(),
    val isProcessed: Boolean = false,
    val processedCount: Int = 0
) {
    @Transient
    var templateMat: Mat? = null
    
    @Transient
    var templateCorners: MatOfPoint2f? = null
    
    @Transient
    var templateTransformMatrix: Mat? = null
    
    fun loadTemplateData(templateImagePath: String) {
        if (templateMat == null) {
            val bitmap = BitmapFactory.decodeFile(templateImagePath)
            templateMat = Mat()
            Utils.bitmapToMat(bitmap, templateMat)
        }
    }
    
    fun release() {
        templateMat?.release()
        templateCorners?.release()
        templateTransformMatrix?.release()
        templateMat = null
        templateCorners = null
        templateTransformMatrix = null
    }
}
