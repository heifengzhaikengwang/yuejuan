package com.example.scanmarker

import android.app.Application
import org.opencv.android.OpenCVLoader

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!OpenCVLoader.initDebug()) {
            // 如果debug初始化失败，尝试其他方式
            // 但在我们已经包含了本地库的情况下，这个应该能正常工作
        }
    }
}
