package com.example.scanmarker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private val REQUEST_CODE_CAMERA_PERMISSION = 1001
    private val TAG = "CameraActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_camera)

            cameraExecutor = Executors.newSingleThreadExecutor()

            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_CAMERA_PERMISSION
                )
            }

            val captureButton = findViewById<Button>(R.id.captureButton)
            captureButton.setOnClickListener {
                takePhoto()
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error", e)
            Toast.makeText(this, "初始化错误: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun allPermissionsGranted(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(
            baseContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val storagePermission = ContextCompat.checkSelfPermission(
            baseContext,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        return cameraPermission && storagePermission
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "必要权限被拒绝", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        try {
            val providerFuture = ProcessCameraProvider.getInstance(this)

            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()

                    val preview = Preview.Builder().build()
                    val viewFinder = findViewById<PreviewView>(R.id.viewFinder)
                    preview.setSurfaceProvider(viewFinder.surfaceProvider)

                    imageCapture = ImageCapture.Builder()
                        .setTargetResolution(android.util.Size(2160, 3840))
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Camera start error", e)
                    runOnUiThread {
                        Toast.makeText(this, "相机启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e(TAG, "startCamera error", e)
            Toast.makeText(this, "相机初始化失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun takePhoto() {
        if (!::imageCapture.isInitialized) {
            Toast.makeText(this, "相机未就绪", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFile = try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
        } catch (e: Exception) {
            Log.e(TAG, "创建图片文件失败", e)
            Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show()
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = "拍照成功: ${photoFile.absolutePath}"
                    Log.d(TAG, msg)
                    Toast.makeText(this@CameraActivity, "拍照成功", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@CameraActivity, MarkActivity::class.java)
                    intent.putExtra("photoPath", photoFile.absolutePath)
                    startActivity(intent)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "拍照失败: ${exception.message}", exception)
                    Toast.makeText(this@CameraActivity, "拍照失败: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}
