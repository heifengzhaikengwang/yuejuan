package com.example.scanmarker

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 本地库已自动通过jniLibs加载
    }
}
