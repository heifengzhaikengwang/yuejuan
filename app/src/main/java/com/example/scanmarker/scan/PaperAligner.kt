package com.example.scanmarker.scan

import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt

class PaperAligner {

    companion object {
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

    fun adjustToTemplateRatio(mat: Mat, templateRatio: Double, tolerance: Double = 0.01): Mat {
        val currentWidth = mat.cols().toDouble()
        val currentHeight = mat.rows().toDouble()
        val currentRatio = currentWidth / currentHeight
        
        if (kotlin.math.abs(currentRatio - templateRatio) < tolerance) {
            return mat
        }
        
        val targetWidth: Double
        val targetHeight: Double
        
        if (currentRatio > templateRatio) {
            targetHeight = currentHeight
            targetWidth = targetHeight * templateRatio
        } else {
            targetWidth = currentWidth
            targetHeight = targetWidth / templateRatio
        }
        
        val cropped = Mat()
        val x = ((currentWidth - targetWidth) / 2).toInt()
        val y = ((currentHeight - targetHeight) / 2).toInt()
        val rect = Rect(x, y, targetWidth.toInt(), targetHeight.toInt())
        val roi = Mat(mat, rect)
        roi.copyTo(cropped)
        roi.release()
        
        return cropped
    }

    fun alignWithMatrix(mat: Mat, transformMatrix: Mat): Mat {
        val result = Mat()
        Imgproc.warpPerspective(mat, result, transformMatrix, mat.size())
        return result
    }

    fun resizeToReference(mat: Mat): Mat {
        val currentWidth = mat.cols().toDouble()
        val currentHeight = mat.rows().toDouble()
        val targetWidth = REFERENCE_WIDTH
        val targetHeight = targetWidth / (currentWidth / currentHeight)
        
        val resized = Mat()
        Imgproc.resize(mat, resized, Size(targetWidth, targetHeight), 0.0, 0.0, Imgproc.INTER_AREA)
        
        return resized
    }

    fun getTemplateRatio(corners: MatOfPoint2f): Double {
        val pts = corners.toArray()
        
        val widthTop = distance(pts[0], pts[1])
        val widthBottom = distance(pts[3], pts[2])
        val heightLeft = distance(pts[0], pts[3])
        val heightRight = distance(pts[1], pts[2])
        
        val avgWidth = (widthTop + widthBottom) / 2.0
        val avgHeight = (heightLeft + heightRight) / 2.0
        
        return avgWidth / avgHeight
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
