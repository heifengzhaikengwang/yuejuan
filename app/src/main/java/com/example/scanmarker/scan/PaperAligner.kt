package com.example.scanmarker.scan

import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class PaperAligner {

    fun align(mat: Mat, corners: MatOfPoint2f): Mat {
        val result = Mat()

        val pts = corners.toArray()
        val srcPts = MatOfPoint2f(*pts)

        val width = 2100
        val height = 2970

        val dstPts = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(width.toDouble(), 0.0),
            Point(width.toDouble(), height.toDouble()),
            Point(0.0, height.toDouble())
        )

        val perspectiveMatrix = Imgproc.getPerspectiveTransform(srcPts, dstPts)
        Imgproc.warpPerspective(mat, result, perspectiveMatrix, Size(width.toDouble(), height.toDouble()))

        return result
    }
}
