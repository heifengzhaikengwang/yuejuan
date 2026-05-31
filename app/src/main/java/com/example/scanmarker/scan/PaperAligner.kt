package com.example.scanmarker.scan

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt

class PaperAligner {

    fun align(mat: Mat, corners: MatOfPoint2f): Mat {
        val pts = corners.toArray()
        
        val widthTop = distance(pts[0], pts[1])
        val widthBottom = distance(pts[3], pts[2])
        val heightLeft = distance(pts[0], pts[3])
        val heightRight = distance(pts[1], pts[2])
        
        val avgWidth = (widthTop + widthBottom) / 2.0
        val avgHeight = (heightLeft + heightRight) / 2.0
        
        val outputWidth = avgWidth.toInt()
        val outputHeight = avgHeight.toInt()
        
        val result = Mat()
        val transformMatrix = getPerspectiveMatrix(corners, outputWidth.toDouble(), outputHeight.toDouble())
        Imgproc.warpPerspective(mat, result, transformMatrix, Size(outputWidth.toDouble(), outputHeight.toDouble()))
        
        return result
    }

    fun alignWithMatrix(mat: Mat, transformMatrix: Mat): Mat {
        val result = Mat()
        Imgproc.warpPerspective(mat, result, transformMatrix, mat.size())
        return result
    }

    fun getPerspectiveMatrix(corners: MatOfPoint2f, outputWidth: Double = 0.0, outputHeight: Double = 0.0): Mat {
        val pts = corners.toArray()
        
        val widthTop = distance(pts[0], pts[1])
        val widthBottom = distance(pts[3], pts[2])
        val heightLeft = distance(pts[0], pts[3])
        val heightRight = distance(pts[1], pts[2])
        
        val avgWidth = (widthTop + widthBottom) / 2.0
        val avgHeight = (heightLeft + heightRight) / 2.0
        
        val w = if (outputWidth > 0) outputWidth else avgWidth
        val h = if (outputHeight > 0) outputHeight else avgHeight
        
        val dstPts = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(w, 0.0),
            Point(w, h),
            Point(0.0, h)
        )

        return Imgproc.getPerspectiveTransform(corners, dstPts)
    }

    fun getDestinationPoints(width: Double, height: Double): MatOfPoint2f {
        return MatOfPoint2f(
            Point(0.0, 0.0),
            Point(width, 0.0),
            Point(width, height),
            Point(0.0, height)
        )
    }

    private fun distance(p1: Point, p2: Point): Double {
        return sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y))
    }
}
