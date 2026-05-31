package com.example.scanmarker.scan

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt
import kotlin.math.min
import kotlin.math.max

class PaperAligner {

    companion object {
        const val A4_RATIO = 210.0 / 297.0
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
        
        val croppedRatio = avgWidth / avgHeight
        
        if (kotlin.math.abs(croppedRatio - A4_RATIO) > 0.05) {
            val targetWidth: Double
            val targetHeight: Double
            
            if (croppedRatio > A4_RATIO) {
                targetHeight = avgHeight
                targetWidth = targetHeight * A4_RATIO
            } else {
                targetWidth = avgWidth
                targetHeight = targetWidth / A4_RATIO
            }
            
            val resized = Mat()
            Imgproc.resize(result, resized, Size(targetWidth, targetHeight), 0.0, 0.0, Imgproc.INTER_LINEAR)
            result.release()
            return resized
        }
        
        return result
    }

    fun alignWithMatrix(mat: Mat, transformMatrix: Mat): Mat {
        val result = Mat()
        Imgproc.warpPerspective(mat, result, transformMatrix, mat.size())
        return result
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
