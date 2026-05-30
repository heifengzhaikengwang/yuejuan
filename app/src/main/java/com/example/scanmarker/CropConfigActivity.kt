package com.example.scanmarker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CropConfigActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
        private const val PREFS_NAME = "crop_config"
        private const val KEY_CROP_BOXES = "crop_boxes"

        fun saveCropBoxes(context: Context, boxes: List<CropBox>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = Json.encodeToString(boxes)
            prefs.edit().putString(KEY_CROP_BOXES, json).apply()
        }

        fun getCropBoxes(context: Context): List<CropBox> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_CROP_BOXES, null) ?: return emptyList()
            return try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private lateinit var previewImage: ImageView
    private lateinit var cropBoxView: CropBoxView
    private lateinit var addBoxBtn: Button
    private lateinit var removeBoxBtn: Button
    private lateinit var clearAllBtn: Button
    private lateinit var cancelBtn: Button
    private lateinit var saveBtn: Button

    private var imagePath: String? = null
    private var originalBitmap: Bitmap? = null
    private var imageScale: Float = 1f
    private var imageOffsetX: Float = 0f
    private var imageOffsetY: Float = 0f
    private var currentBoxes: List<CropBox> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_config)

        initViews()
        loadImage()
        loadSavedConfig()
        setupListeners()
    }

    private fun initViews() {
        previewImage = findViewById(R.id.previewImage)
        cropBoxView = findViewById(R.id.cropBoxView)
        addBoxBtn = findViewById(R.id.addBoxBtn)
        removeBoxBtn = findViewById(R.id.removeBoxBtn)
        clearAllBtn = findViewById(R.id.clearAllBtn)
        cancelBtn = findViewById(R.id.cancelBtn)
        saveBtn = findViewById(R.id.saveBtn)
    }

    private fun loadImage() {
        imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath != null) {
            try {
                originalBitmap = BitmapFactory.decodeFile(imagePath)
                if (originalBitmap != null) {
                    previewImage.setImageBitmap(originalBitmap)
                    previewImage.viewTreeObserver.addOnGlobalLayoutListener {
                        calculateImageScale()
                    }
                } else {
                    Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "未找到图片", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun calculateImageScale() {
        val bm = originalBitmap ?: return
        val viewWidth = previewImage.width.toFloat()
        val viewHeight = previewImage.height.toFloat()

        if (viewWidth <= 0 || viewHeight <= 0) return

        val scaleX = viewWidth / bm.width
        val scaleY = viewHeight / bm.height
        imageScale = minOf(scaleX, scaleY)

        imageOffsetX = (viewWidth - bm.width * imageScale) / 2
        imageOffsetY = (viewHeight - bm.height * imageScale) / 2

        cropBoxView.setImageTransform(imageScale, imageOffsetX, imageOffsetY)
    }

    private fun loadSavedConfig() {
        val savedBoxes = getCropBoxes(this)
        currentBoxes = savedBoxes
        if (savedBoxes.isNotEmpty()) {
            cropBoxView.setCropBoxes(savedBoxes)
        }
    }

    private fun setupListeners() {
        addBoxBtn.setOnClickListener {
            cropBoxView.addCropBox()
        }

        removeBoxBtn.setOnClickListener {
            cropBoxView.removeSelectedBox()
        }

        clearAllBtn.setOnClickListener {
            cropBoxView.clearAllBoxes()
        }

        cancelBtn.setOnClickListener {
            finish()
        }

        saveBtn.setOnClickListener {
            saveConfig()
        }

        cropBoxView.onBoxesChanged = { boxes ->
            currentBoxes = boxes
        }
    }

    private fun saveConfig() {
        val boxes = if (currentBoxes.isNotEmpty()) {
            currentBoxes
        } else {
            cropBoxView.cropBoxes
        }

        if (boxes.isEmpty()) {
            Toast.makeText(this, "至少需要一个裁切框", Toast.LENGTH_SHORT).show()
            return
        }

        saveCropBoxes(this, boxes)
        Toast.makeText(this, "已保存 ${boxes.size} 个裁切框", Toast.LENGTH_SHORT).show()
        finish()
    }
}
