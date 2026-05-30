package com.example.scanmarker

import android.app.Application
import org.opencv.android.OpenCVLoader

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 使用OpenCVLoader初始化本地库
        OpenCVLoader.initDebug()
    }
}
