package com.example.scanmarker.scan

import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class CornerDetector {

    fun detect(mat: Mat): MatOfPoint2f {
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)

        val bin = Mat()
        Imgproc.threshold(gray, bin, 80.0, 255.0, Imgproc.THRESH_BINARY_INV)

        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            bin, contours, Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        val rects = contours
            .map { Imgproc.boundingRect(it) }
            .filter { it.area() > 8000 }
            .sortedBy { it.x + it.y }
            .take(4)

        val pts = rects.map {
            Point(it.x + it.width / 2.0, it.y + it.height / 2.0)
        }

        return MatOfPoint2f(*pts.toTypedArray())
    }
}
