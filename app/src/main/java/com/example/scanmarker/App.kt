package com.example.scanmarker

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("opencv_java4")
    }
}
