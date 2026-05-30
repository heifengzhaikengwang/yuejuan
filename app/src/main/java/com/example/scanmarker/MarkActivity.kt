package com.example.scanmarker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scanmarker.scan.CornerDetector
import com.example.scanmarker.scan.PaperAligner
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import java.io.File
import java.io.FileOutputStream

class MarkActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var processButton: Button
    private lateinit var cropButton: Button
    private lateinit var saveButton: Button
    private lateinit var retakeButton: Button
    private lateinit var backButton: Button

    private var photoPath: String? = null
    private var originalBitmap: Bitmap? = null
    private var processedBitmap: Bitmap? = null
    private var alignedMat: Mat? = null

    private val cornerDetector = CornerDetector()
    private val paperAligner = PaperAligner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mark)

        initViews()
        loadPhoto()
        setupButtons()
    }

    private fun initViews() {
        imageView = findViewById(R.id.imageView)
        processButton = findViewById(R.id.processButton)
        cropButton = findViewById(R.id.cropButton)
        saveButton = findViewById(R.id.saveButton)
        retakeButton = findViewById(R.id.retakeButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun loadPhoto() {
        photoPath = intent.getStringExtra("photoPath")

        if (photoPath != null) {
            loadImage(photoPath!!)
        } else {
            Toast.makeText(this, "未找到图片", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupButtons() {
        processButton.setOnClickListener {
            processImage()
        }

        cropButton.setOnClickListener {
            cropQuestions()
        }

        saveButton.setOnClickListener {
            saveResult()
        }

        retakeButton.setOnClickListener {
            finish()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadImage(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                originalBitmap = BitmapFactory.decodeFile(path)
                imageView.setImageBitmap(originalBitmap)
            } else {
                Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "加载图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processImage() {
        if (originalBitmap == null) {
            Toast.makeText(this, "请先加载图片", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val mat = Mat()
            Utils.bitmapToMat(originalBitmap, mat)

            val corners: MatOfPoint2f = cornerDetector.detect(mat)
            alignedMat = paperAligner.align(mat, corners)

            processedBitmap = Bitmap.createBitmap(alignedMat!!.cols(), alignedMat!!.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(alignedMat, processedBitmap)

            imageView.setImageBitmap(processedBitmap)
            cropButton.isEnabled = true
            Toast.makeText(this, "处理成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "处理失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cropQuestions() {
        if (processedBitmap == null) {
            Toast.makeText(this, "请先处理图片", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "切题功能开发中", Toast.LENGTH_SHORT).show()
        saveButton.isEnabled = true
    }

    private fun saveResult() {
        if (processedBitmap == null) {
            Toast.makeText(this, "没有可保存的图片", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val timeStamp = System.currentTimeMillis()
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File(storageDir, "processed_$timeStamp.jpg")

            val fos = FileOutputStream(file)
            processedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()

            Toast.makeText(this, "保存成功: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        alignedMat?.release()
    }
}
