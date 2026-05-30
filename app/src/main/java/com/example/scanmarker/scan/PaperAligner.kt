package com.example.scanmarker.scan

import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class PaperAligner {

    val ALIGNED_WIDTH = 2100
    val ALIGNED_HEIGHT = 2970

    fun align(mat: Mat, corners: MatOfPoint2f): Mat {
        val result = Mat()
        val transformMatrix = getPerspectiveMatrix(corners)
        Imgproc.warpPerspective(mat, result, transformMatrix, Size(ALIGNED_WIDTH.toDouble(), ALIGNED_HEIGHT.toDouble()))
        return result
    }

    fun alignWithMatrix(mat: Mat, transformMatrix: Mat): Mat {
        val result = Mat()
        Imgproc.warpPerspective(mat, result, transformMatrix, Size(ALIGNED_WIDTH.toDouble(), ALIGNED_HEIGHT.toDouble()))
        return result
    }

    fun getPerspectiveMatrix(corners: MatOfPoint2f): Mat {
        val pts = corners.toArray()
        val srcPts = MatOfPoint2f(*pts)

        val dstPts = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(ALIGNED_WIDTH.toDouble(), 0.0),
            Point(ALIGNED_WIDTH.toDouble(), ALIGNED_HEIGHT.toDouble()),
            Point(0.0, ALIGNED_HEIGHT.toDouble())
        )

        return Imgproc.getPerspectiveTransform(srcPts, dstPts)
    }

    fun getDestinationPoints(): MatOfPoint2f {
        return MatOfPoint2f(
            Point(0.0, 0.0),
            Point(ALIGNED_WIDTH.toDouble(), 0.0),
            Point(ALIGNED_WIDTH.toDouble(), ALIGNED_HEIGHT.toDouble()),
            Point(0.0, ALIGNED_HEIGHT.toDouble())
        )
    }
}
