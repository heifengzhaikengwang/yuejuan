package com.example.scanmarker.batch

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.scanmarker.R
import com.example.scanmarker.CropConfigActivity
import com.example.scanmarker.MarkActivity
import com.example.scanmarker.scan.CornerDetector
import com.example.scanmarker.scan.CropManager
import com.example.scanmarker.scan.PaperAligner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import java.io.File
import java.io.FileOutputStream

class BatchProcessorActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var step1Text: TextView
    private lateinit var step2Text: TextView
    private lateinit var step3Text: TextView
    private lateinit var previewImage: ImageView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var startButton: Button
    private lateinit var configCropButton: Button

    private lateinit var batchManager: BatchManager
    private var batch: BatchData? = null

    private val cornerDetector = CornerDetector()
    private val paperAligner = PaperAligner()
    private val cropManager = CropManager()

    private var currentStep = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_processor)

        batchManager = BatchManager(this)
        batch = batchManager.getCurrentBatch()

        initViews()
        setupListeners()
        loadBatchData()
    }

    private fun initViews() {
        titleText = findViewById(R.id.titleText)
        step1Text = findViewById(R.id.step1Text)
        step2Text = findViewById(R.id.step2Text)
        step3Text = findViewById(R.id.step3Text)
        previewImage = findViewById(R.id.previewImage)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        startButton = findViewById(R.id.startButton)
        configCropButton = findViewById(R.id.configCropButton)
    }

    private fun setupListeners() {
        configCropButton.setOnClickListener {
            val intent = Intent(this, CropConfigActivity::class.java)
            val templateItem = batch?.items?.getOrNull(batch?.templateIndex ?: -1)
            templateItem?.let {
                intent.putExtra(CropConfigActivity.EXTRA_IMAGE_PATH, it.imagePath)
            }
            startActivity(intent)
        }

        startButton.setOnClickListener {
            when (currentStep) {
                1 -> startAlignment()
                2 -> startCropping()
                3 -> saveAllResults()
            }
        }
    }

    private fun loadBatchData() {
        batch = batchManager.getCurrentBatch()
        
        batch?.let { b ->
            titleText.text = "批量处理 (${b.items.size} 张)"
            progressBar.max = b.items.size
            progressText.text = "0 / ${b.items.size}"

            if (b.templateIndex >= 0 && b.templateIndex < b.items.size) {
                val templateItem = b.items[b.templateIndex]
                val bitmap = BitmapFactory.decodeFile(templateItem.imagePath)
                previewImage.setImageBitmap(bitmap)
            }

            when {
                b.alignMatrix != null -> {
                    currentStep = 2
                    updateStepIndicator()
                    startButton.text = "开始裁切"
                }
                b.isProcessed -> {
                    currentStep = 3
                    updateStepIndicator()
                    startButton.text = "保存结果"
                    statusText.text = "处理完成！"
                }
                else -> {
                    currentStep = 1
                    updateStepIndicator()
                    startButton.text = "开始对齐"
                }
            }
        } ?: run {
            Toast.makeText(this, "没有批次数据", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun updateStepIndicator() {
        step1Text.setTextColor(if (currentStep >= 1) getColor(R.color.green) else getColor(R.color.grey))
        step2Text.setTextColor(if (currentStep >= 2) getColor(R.color.green) else getColor(R.color.grey))
        step3Text.setTextColor(if (currentStep >= 3) getColor(R.color.green) else getColor(R.color.grey))
    }

    private fun startAlignment() {
        lifecycleScope.launch {
            try {
                batch?.let { b ->
                    val templateItem = b.items.getOrNull(b.templateIndex) ?: return@launch
                    
                    statusText.text = "正在对齐..."
                    previewImage.setImageBitmap(BitmapFactory.decodeFile(templateItem.imagePath))

                    withContext(Dispatchers.Default) {
                        val mat = Mat()
                        val bitmap = BitmapFactory.decodeFile(templateItem.imagePath)
                        Utils.bitmapToMat(bitmap, mat)

                        val corners: MatOfPoint2f = cornerDetector.detect(mat)
                        val alignedMat = paperAligner.align(mat, corners)

                        val alignedBitmap = Bitmap.createBitmap(
                            alignedMat.cols(), alignedMat.rows(), Bitmap.Config.ARGB_8888
                        )
                        Utils.matToBitmap(alignedMat, alignedBitmap)

                        withContext(Dispatchers.Main) {
                            previewImage.setImageBitmap(alignedBitmap)
                            alignedMat.release()
                            mat.release()
                        }
                    }

                    currentStep = 2
                    updateStepIndicator()
                    startButton.text = "开始裁切"
                    statusText.text = "对齐完成！"

                    Toast.makeText(this@BatchProcessorActivity, "对齐成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "对齐失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startCropping() {
        lifecycleScope.launch {
            try {
                batch?.let { b ->
                    statusText.text = "正在裁切..."
                    
                    val cropBoxes = CropConfigActivity.getCropBoxes(this@BatchProcessorActivity)
                    var processed = 0

                    withContext(Dispatchers.Default) {
                        b.items.forEachIndexed { index, item ->
                            if (!item.isProcessed) {
                                try {
                                    val mat = Mat()
                                    val bitmap = BitmapFactory.decodeFile(item.imagePath)
                                    Utils.bitmapToMat(bitmap, mat)

                                    val corners: MatOfPoint2f = cornerDetector.detect(mat)
                                    val alignedMat = paperAligner.align(mat, corners)

                                    val studentInfo = MarkActivity.StudentInfo(
                                        studentId = item.studentId,
                                        studentName = item.studentName,
                                        classInfo = item.classInfo
                                    )

                                    val outputDir = File(
                                        getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                        "batch_${b.batchId}"
                                    )
                                    if (!outputDir.exists()) outputDir.mkdirs()

                                    val croppedFiles = if (cropBoxes.isNotEmpty()) {
                                        cropManager.cropWithCustomBoxes(alignedMat, outputDir, cropBoxes, studentInfo)
                                    } else {
                                        cropManager.cropAllQuestions(alignedMat, outputDir, studentInfo)
                                    }

                                    val processedPath = outputDir.absolutePath
                                    batchManager.markItemProcessed(item.id, processedPath)

                                    alignedMat.release()
                                    mat.release()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            processed++
                            withContext(Dispatchers.Main) {
                                progressBar.progress = processed
                                progressText.text = "$processed / ${b.items.size}"
                            }
                        }
                    }

                    currentStep = 3
                    updateStepIndicator()
                    startButton.text = "保存结果"
                    statusText.text = "裁切完成！"

                    Toast.makeText(this@BatchProcessorActivity, "所有图片已处理完成", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "裁切失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveAllResults() {
        AlertDialog.Builder(this)
            .setTitle("保存完成")
            .setMessage("所有图片已处理完成！\n\n结果保存在: Pictures/batch_${batch?.batchId}")
            .setPositiveButton("确定") { _, _ ->
                batchManager.clearBatch()
                Toast.makeText(this, "批次已清空", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("查看结果") { _, _ ->
                Toast.makeText(this, "请到Pictures文件夹查看结果", Toast.LENGTH_LONG).show()
            }
            .show()
    }
}
