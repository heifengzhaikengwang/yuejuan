package com.example.scanmarker

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class CropBoxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var viewCropBoxes = mutableListOf<CropBox>()
    private var originalCropBoxes = mutableListOf<CropBox>()
    private var selectedBox: CropBox? = null
    private var draggingCorner: Corner? = null
    private var isDraggingBox = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val cornerRadius = 15f
    private val handleSize = 30f

    private var imageScale: Float = 1f
    private var imageOffsetX: Float = 0f
    private var imageOffsetY: Float = 0f

    var onBoxesChanged: ((List<CropBox>) -> Unit)? = null

    val cropBoxes: List<CropBox>
        get() = viewCropBoxes.toList()

    init {
        paint.apply {
            color = Color.parseColor("#4CAF50")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        cornerPaint.apply {
            color = Color.parseColor("#FF9800")
            style = Paint.Style.FILL
        }

        textPaint.apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    fun setImageTransform(scale: Float, offsetX: Float, offsetY: Float) {
        imageScale = scale
        imageOffsetX = offsetX
        imageOffsetY = offsetY
        invalidate()
    }

    fun setCropBoxes(boxes: List<CropBox>) {
        originalCropBoxes.clear()
        originalCropBoxes.addAll(boxes)
        convertToViewCoordinates()
        invalidate()
    }

    fun addCropBox(name: String = "裁切框${viewCropBoxes.size + 1}") {
        val padding = 50f
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val viewBox = CropBox(
            name = name,
            left = imageOffsetX + padding,
            top = imageOffsetY + padding,
            right = imageOffsetX + viewWidth - padding,
            bottom = imageOffsetY + viewHeight - padding
        )
        viewCropBoxes.add(viewBox)
        selectedBox = viewBox
        invalidate()
        convertToOriginalCoordinates()
        notifyBoxesChanged()
    }

    fun removeSelectedBox() {
        selectedBox?.let { box ->
            viewCropBoxes.remove(box)
            selectedBox = null
            invalidate()
            convertToOriginalCoordinates()
            notifyBoxesChanged()
        }
    }

    fun clearAllBoxes() {
        viewCropBoxes.clear()
        selectedBox = null
        invalidate()
        convertToOriginalCoordinates()
        notifyBoxesChanged()
    }

    private fun convertToViewCoordinates() {
        viewCropBoxes.clear()
        viewCropBoxes.addAll(originalCropBoxes.map { box ->
            box.copy(
                left = box.left * imageScale + imageOffsetX,
                top = box.top * imageScale + imageOffsetY,
                right = box.right * imageScale + imageOffsetX,
                bottom = box.bottom * imageScale + imageOffsetY
            )
        })
    }

    private fun convertToOriginalCoordinates() {
        originalCropBoxes.clear()
        originalCropBoxes.addAll(viewCropBoxes.map { box ->
            box.copy(
                left = (box.left - imageOffsetX) / imageScale,
                top = (box.top - imageOffsetY) / imageScale,
                right = (box.right - imageOffsetX) / imageScale,
                bottom = (box.bottom - imageOffsetY) / imageScale
            )
        })
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        viewCropBoxes.forEachIndexed { index, box ->
            val rect = box.toRectF()
            
            if (box == selectedBox) {
                paint.color = Color.parseColor("#FF9800")
                paint.strokeWidth = 5f
            } else {
                paint.color = Color.parseColor("#4CAF50")
                paint.strokeWidth = 3f
            }

            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

            if (box == selectedBox) {
                drawCornerHandles(canvas, rect)
            }

            val textY = rect.top - 10f
            val textX = rect.left + 10f
            canvas.drawText(box.name, textX, textY, textPaint)
        }
    }

    private fun drawCornerHandles(canvas: Canvas, rect: RectF) {
        val handleRadius = 15f

        canvas.drawCircle(rect.left, rect.top, handleRadius, cornerPaint)
        canvas.drawCircle(rect.right, rect.top, handleRadius, cornerPaint)
        canvas.drawCircle(rect.right, rect.bottom, handleRadius, cornerPaint)
        canvas.drawCircle(rect.left, rect.bottom, handleRadius, cornerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleActionDown(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                handleActionMove(event.x, event.y)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                handleActionUp()
            }
        }
        return true
    }

    private fun handleActionDown(x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y

        for (box in viewCropBoxes.reversed()) {
            val corner = box.getCornerAt(x, y)
            if (corner != null) {
                selectedBox = box
                draggingCorner = corner
                invalidate()
                return
            }

            if (box.contains(x, y)) {
                selectedBox = box
                isDraggingBox = true
                invalidate()
                return
            }
        }

        selectedBox = null
        invalidate()
    }

    private fun handleActionMove(x: Float, y: Float) {
        val box = selectedBox ?: return
        val dx = x - lastTouchX
        val dy = y - lastTouchY

        if (draggingCorner != null) {
            val newBox = when (draggingCorner!!) {
                Corner.TOP_LEFT -> box.copy(left = box.left + dx, top = box.top + dy)
                Corner.TOP_RIGHT -> box.copy(right = box.right + dx, top = box.top + dy)
                Corner.BOTTOM_RIGHT -> box.copy(right = box.right + dx, bottom = box.bottom + dy)
                Corner.BOTTOM_LEFT -> box.copy(left = box.left + dx, bottom = box.bottom + dy)
            }
            updateBox(box, newBox)
        } else if (isDraggingBox) {
            val newBox = box.copy(
                left = box.left + dx,
                top = box.top + dy,
                right = box.right + dx,
                bottom = box.bottom + dy
            )
            updateBox(box, newBox)
        }

        lastTouchX = x
        lastTouchY = y
    }

    private fun handleActionUp() {
        draggingCorner = null
        isDraggingBox = false
        convertToOriginalCoordinates()
        notifyBoxesChanged()
    }

    private fun updateBox(oldBox: CropBox, newBox: CropBox) {
        val index = viewCropBoxes.indexOf(oldBox)
        if (index != -1) {
            viewCropBoxes[index] = newBox.copy(id = oldBox.id, name = oldBox.name)
            selectedBox = viewCropBoxes[index]
            invalidate()
        }
    }

    private fun notifyBoxesChanged() {
        onBoxesChanged?.invoke(originalCropBoxes.toList())
    }
}
