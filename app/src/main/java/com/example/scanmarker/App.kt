package com.example.scanmarker

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initOpenCV()
    }

    private fun initOpenCV() {
        try {
            if (OpenCVLoader.initDebug()) {
                Log.d("ScanMarker", "OpenCV 初始化成功")
            } else {
                Log.e("ScanMarker", "OpenCV 初始化失败")
            }
        } catch (e: Exception) {
            Log.e("ScanMarker", "OpenCV 初始化异常: ${e.message}")
        }
    }
}
