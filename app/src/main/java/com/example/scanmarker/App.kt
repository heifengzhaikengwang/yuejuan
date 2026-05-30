package com.example.scanmarker

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class App : Application() {
    private val TAG = "ScanMarkerApp"

    override fun onCreate() {
        super.onCreate()
        initOpenCV()
    }

    private fun initOpenCV() {
        try {
            if (OpenCVLoader.initDebug()) {
                Log.d(TAG, "OpenCV 初始化成功")
            } else {
                Log.e(TAG, "OpenCV 初始化失败")
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenCV 初始化异常: ${e.message}", e)
            // OpenCV 初始化失败不崩溃
        }
    }
}
