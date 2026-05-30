package com.example.scanmarker

import android.graphics.RectF
import kotlinx.serialization.Serializable

@Serializable
data class CropBox(
    val id: String = System.currentTimeMillis().toString(),
    val name: String = "裁切框",
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    fun toRectF(): RectF {
        return RectF(
            left.coerceAtMost(right),
            top.coerceAtMost(bottom),
            right.coerceAtLeast(left),
            bottom.coerceAtLeast(top)
        )
    }

    fun width(): Float = Math.abs(right - left)
    fun height(): Float = Math.abs(bottom - top)

    fun contains(x: Float, y: Float, touchSlop: Float = 20f): Boolean {
        val rect = toRectF()
        return x >= rect.left - touchSlop && 
               x <= rect.right + touchSlop && 
               y >= rect.top - touchSlop && 
               y <= rect.bottom + touchSlop
    }

    fun getCornerAt(x: Float, y: Float, touchSlop: Float = 30f): Corner? {
        val rect = toRectF()
        return when {
            distance(x, y, rect.left, rect.top) < touchSlop -> Corner.TOP_LEFT
            distance(x, y, rect.right, rect.top) < touchSlop -> Corner.TOP_RIGHT
            distance(x, y, rect.right, rect.bottom) < touchSlop -> Corner.BOTTOM_RIGHT
            distance(x, y, rect.left, rect.bottom) < touchSlop -> Corner.BOTTOM_LEFT
            else -> null
        }
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return Math.sqrt(Math.pow((x1 - x2).toDouble(), 2.0) + Math.pow((y1 - y2).toDouble(), 2.0)).toFloat()
    }
}

enum class Corner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT
}
