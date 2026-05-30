package com.example.scanmarker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CameraActivity : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture
    private val REQUEST_CODE_CAMERA_PERMISSION = 1001
    private val TAG = "CameraActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_camera)

            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CODE_CAMERA_PERMISSION
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error", e)
            Toast.makeText(this, "初始化错误: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

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
                Toast.makeText(this, "相机权限被拒绝", Toast.LENGTH_SHORT).show()
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
            }, ContextCompat.getMainExecutor(this)) // 使用主线程避免问题
        } catch (e: Exception) {
            Log.e(TAG, "startCamera error", e)
            Toast.makeText(this, "相机初始化失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
