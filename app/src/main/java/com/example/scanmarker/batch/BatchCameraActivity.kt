package com.example.scanmarker.batch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import com.example.scanmarker.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BatchCameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: androidx.camera.view.PreviewView
    private lateinit var captureButton: ImageButton
    private lateinit var importButton: Button
    private lateinit var viewBatchButton: Button
    private lateinit var clearBatchButton: Button
    private lateinit var processBatchButton: Button
    private lateinit var photoCountText: TextView
    private lateinit var backButton: ImageButton

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var batchManager: BatchManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val paths = mutableListOf<String>()
            uris.forEach { uri ->
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val file = createImageFile()
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    paths.add(file.absolutePath)
                }
            }
            batchManager.addMultiplePhotos(paths)
            updatePhotoCount()
            Toast.makeText(this, "已导入 ${paths.size} 张图片", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_camera)

        batchManager = BatchManager(this)
        batchManager.createBatch()

        initViews()
        setupListeners()
        checkCameraPermission()

        cameraExecutor = Executors.newSingleThreadExecutor()
        updatePhotoCount()
    }

    private fun initViews() {
        viewFinder = findViewById(R.id.viewFinder)
        captureButton = findViewById(R.id.captureButton)
        importButton = findViewById(R.id.importButton)
        viewBatchButton = findViewById(R.id.viewBatchButton)
        clearBatchButton = findViewById(R.id.clearBatchButton)
        processBatchButton = findViewById(R.id.processBatchButton)
        photoCountText = findViewById(R.id.photoCountText)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupListeners() {
        captureButton.setOnClickListener {
            takePhoto()
        }

        importButton.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }

        viewBatchButton.setOnClickListener {
            startActivity(Intent(this, BatchManagerActivity::class.java))
        }

        clearBatchButton.setOnClickListener {
            batchManager.clearBatch()
            batchManager.createBatch()
            updatePhotoCount()
            Toast.makeText(this, "批次已清空", Toast.LENGTH_SHORT).show()
        }

        processBatchButton.setOnClickListener {
            val batch = batchManager.getCurrentBatch()
            if (batch == null || batch.items.isEmpty()) {
                Toast.makeText(this, "请先添加图片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, BatchProcessorActivity::class.java))
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 
                    PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "相机启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = createImageFile()

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    batchManager.addPhotoToBatch(photoFile.absolutePath)
                    updatePhotoCount()
                    Toast.makeText(
                        this@BatchCameraActivity,
                        "已保存",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@BatchCameraActivity,
                        "保存失败: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(System.currentTimeMillis())
        val storageDir = getExternalFilesDir("batch_photos")
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File(storageDir, "IMG_${timestamp}.jpg")
    }

    private fun updatePhotoCount() {
        val batch = batchManager.getCurrentBatch()
        val count = batch?.items?.size ?: 0
        photoCountText.text = "已拍摄: $count 张"

        processBatchButton.isEnabled = count > 0
        viewBatchButton.text = if (count > 0) "查看批次($count)" else "查看批次"
    }

    override fun onResume() {
        super.onResume()
        updatePhotoCount()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
