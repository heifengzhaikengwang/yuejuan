package com.example.scanmarker.scan

import org.opencv.core.*

class CropManager {
    fun crop(mat: Mat, rect: Rect): Mat {
        return Mat(mat, rect)
    }
}
