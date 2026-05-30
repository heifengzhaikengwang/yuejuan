package com.example.scanmarker.batch

import com.example.scanmarker.CropBox
import kotlinx.serialization.Serializable

@Serializable
data class BatchData(
    val batchId: String = System.currentTimeMillis().toString(),
    val items: List<BatchItem> = emptyList(),
    val templateIndex: Int = -1,
    val cropBoxes: List<CropBox> = emptyList(),
    val alignMatrix: FloatArray? = null,
    val createdTime: Long = System.currentTimeMillis(),
    val isProcessed: Boolean = false,
    val processedCount: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BatchData

        if (batchId != other.batchId) return false
        if (items != other.items) return false
        if (templateIndex != other.templateIndex) return false
        if (cropBoxes != other.cropBoxes) return false
        if (alignMatrix != null) {
            if (other.alignMatrix == null) return false
            if (!alignMatrix.contentEquals(other.alignMatrix)) return false
        } else if (other.alignMatrix != null) return false
        if (createdTime != other.createdTime) return false
        if (isProcessed != other.isProcessed) return false
        if (processedCount != other.processedCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = batchId.hashCode()
        result = 31 * result + items.hashCode()
        result = 31 * result + templateIndex
        result = 31 * result + cropBoxes.hashCode()
        result = 31 * result + (alignMatrix?.contentHashCode() ?: 0)
        result = 31 * result + createdTime.hashCode()
        result = 31 * result + isProcessed.hashCode()
        result = 31 * result + processedCount
        return result
    }
}
