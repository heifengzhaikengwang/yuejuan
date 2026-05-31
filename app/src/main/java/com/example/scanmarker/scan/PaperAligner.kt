package com.example.scanmarker.scan

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt
import kotlin.math.abs

class PaperAligner {

    companion object {
        const val A4_WIDTH_MM = 210.0
        const val A4_HEIGHT_MM = 297.0
        const val DPI = 300.0
        val A4_WIDTH_PIXELS: Double get() = A4_WIDTH_MM * DPI / 25.4
        val A4_HEIGHT_PIXELS: Double get() = A4_HEIGHT_MM * DPI / 25.4
    }

    fun align(mat: Mat, corners: MatOfPoint2f): Mat {
        val pts = corners.toArray()
        
        val widthTop = distance(pts[0], pts[1])
        val widthBottom = distance(pts[3], pts[2])
        val heightLeft = distance(pts[0], pts[3])
        val heightRight = distance(pts[1], pts[2])
        
        val avgWidth = (widthTop + widthBottom) / 2.0
        val avgHeight = (heightLeft + heightRight) / 2.0
        
        val result = Mat()
        val transformMatrix = getPerspectiveMatrix(corners)
        Imgproc.warpPerspective(mat, result, transformMatrix, Size(avgWidth, avgHeight))
        
        val resized = resizeToA4Size(result)
        if (resized !== result) {
            result.release()
        }
        
        return resized
    }

    fun alignWithMatrix(mat: Mat, transformMatrix: Mat): Mat {
        val result = Mat()
        Imgproc.warpPerspective(mat, result, transformMatrix, mat.size())
        
        val resized = resizeToA4Size(result)
        if (resized !== result) {
            result.release()
        }
        
        return resized
    }

    private fun resizeToA4Size(mat: Mat): Mat {
        val currentWidth = mat.cols().toDouble()
        val currentHeight = mat.rows().toDouble()
        
        val targetWidth = A4_WIDTH_PIXELS
        val targetHeight = A4_HEIGHT_PIXELS
        
        val resized = Mat()
        Imgproc.resize(mat, resized, Size(targetWidth, targetHeight), 0.0, 0.0, Imgproc.INTER_AREA)
        
        return resized
    }

    fun getPerspectiveMatrix(corners: MatOfPoint2f): Mat {
        val pts = corners.toArray()
        
        val widthTop = distance(pts[0], pts[1])
        val widthBottom = distance(pts[3], pts[2])
        val heightLeft = distance(pts[0], pts[3])
        val heightRight = distance(pts[1], pts[2])
        
        val avgWidth = (widthTop + widthBottom) / 2.0
        val avgHeight = (heightLeft + heightRight) / 2.0
        
        val dstPts = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(avgWidth, 0.0),
            Point(avgWidth, avgHeight),
            Point(0.0, avgHeight)
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
