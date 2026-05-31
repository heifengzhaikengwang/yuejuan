package com.example.scanmarker.scan

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt

class PaperAligner {

    companion object {
        const val A4_RATIO = 210.0 / 297.0
        const val REFERENCE_WIDTH = 2100.0
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
        
        return result
    }

    fun adjustToA4Ratio(mat: Mat): Mat {
        val currentWidth = mat.cols().toDouble()
        val currentHeight = mat.rows().toDouble()
        val currentRatio = currentWidth / currentHeight
        
        if (kotlin.math.abs(currentRatio - A4_RATIO) < 0.01) {
            return mat
        }
        
        val targetWidth: Double
        val targetHeight: Double
        
        if (currentRatio > A4_RATIO) {
            targetHeight = currentHeight
            targetWidth = targetHeight * A4_RATIO
        } else {
            targetWidth = currentWidth
            targetHeight = targetWidth / A4_RATIO
        }
        
        val cropped = Mat()
        val x = (currentWidth - targetWidth) / 2
        val y = (currentHeight - targetHeight) / 2
        val rect = Rect(x.toInt(), y.toInt(), targetWidth.toInt(), targetHeight.toInt())
        mat(rect).copyTo(cropped)
        
        return cropped
    }

    fun alignWithMatrix(mat: Mat, transformMatrix: Mat): Mat {
        val result = Mat()
        Imgproc.warpPerspective(mat, result, transformMatrix, mat.size())
        return result
    }

    fun resizeToReference(mat: Mat): Mat {
        val currentHeight = mat.rows().toDouble()
        val targetHeight = REFERENCE_WIDTH / A4_RATIO
        
        val resized = Mat()
        Imgproc.resize(mat, resized, Size(REFERENCE_WIDTH, targetHeight), 0.0, 0.0, Imgproc.INTER_AREA)
        
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
