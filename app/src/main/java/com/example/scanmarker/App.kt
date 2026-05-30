package com.example.scanmarker

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Maven依赖会自动处理OpenCV库的加载
    }
}
