package com.example.scanmarker.scan

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.*

class CornerDetector {

    companion object {
        const val MIN_MARKER_AREA = 2000
        const val MAX_MARKER_AREA = 100000
        const val BINARY_THRESHOLD = 80.0
    }

    fun detect(mat: Mat): MatOfPoint2f {
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)

        val bin = Mat()
        Imgproc.threshold(gray, bin, BINARY_THRESHOLD, 255.0, Imgproc.THRESH_BINARY_INV)

        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            bin, contours, Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        val rects = contours
            .map { Imgproc.boundingRect(it) }
            .filter { it.area() > MIN_MARKER_AREA && it.area() < MAX_MARKER_AREA }

        if (rects.size < 4) {
            return getFallbackCorners(mat)
        }

        val candidates = findBestQuadrilateral(rects, mat.width(), mat.height())
        
        if (candidates.size == 4) {
            val sortedPoints = sortCornerPoints(candidates)
            return MatOfPoint2f(*sortedPoints.toTypedArray())
        }

        return getFallbackCorners(mat)
    }

    private fun findBestQuadrilateral(
        rects: List<Rect>, 
        imgWidth: Int, 
        imgHeight: Int
    ): List<Point> {
        val centers = rects.map { 
            Point(it.x + it.width / 2.0, it.y + it.height / 2.0) 
        }

        val validCombinations = mutableListOf<Quadrilateral>()

        for (i in 0 until centers.size - 3) {
            for (j in i + 1 until centers.size - 2) {
                for (k in j + 1 until centers.size - 1) {
                    for (l in k + 1 until centers.size) {
                        val quadPoints = listOf(centers[i], centers[j], centers[k], centers[l])
                        val quality = evaluateQuadrilateral(quadPoints, imgWidth, imgHeight)
                        if (quality > 0) {
                            validCombinations.add(Quadrilateral(quadPoints, quality))
                        }
                    }
                }
            }
        }

        if (validCombinations.isNotEmpty) {
            return validCombinations.maxByOrNull { it.quality }!!.points
        }

        return findEdgeBasedCorners(centers, imgWidth, imgHeight)
    }

    private fun evaluateQuadrilateral(points: List<Point>, imgWidth: Int, imgHeight: Int): Double {
        if (points.size != 4) return -1.0

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }

        val width = maxX - minX
        val height = maxY - minY

        if (width < imgWidth * 0.3 || height < imgHeight * 0.3) return -1.0
        if (width > imgWidth * 0.95 || height > imgHeight * 0.95) return -1.0

        val aspectRatio = width / height
        if (aspectRatio < 0.4 || aspectRatio > 2.5) return -1.0

        val leftPoints = points.filter { it.x < minX + width * 0.4 }
        val rightPoints = points.filter { it.x > maxX - width * 0.4 }
        val topPoints = points.filter { it.y < minY + height * 0.4 }
        val bottomPoints = points.filter { it.y > maxY - height * 0.4 }

        if (leftPoints.size < 1 || rightPoints.size < 1 || 
            topPoints.size < 1 || bottomPoints.size < 1) return -1.0

        var quality = 100.0
        
        quality += (width / imgWidth) * 50
        quality += (height / imgHeight) * 50
        
        if (leftPoints.size >= 2) quality += 20
        if (rightPoints.size >= 2) quality += 20
        if (topPoints.size >= 2) quality += 20
        if (bottomPoints.size >= 2) quality += 20

        return quality
    }

    private fun findEdgeBasedCorners(centers: List<Point>, imgWidth: Int, imgHeight: Int): List<Point> {
        val margin = 50.0

        val leftPoints = centers.filter { it.x < imgWidth * 0.25 + margin }
        val rightPoints = centers.filter { it.x > imgWidth * 0.75 - margin }
        val topPoints = centers.filter { it.y < imgHeight * 0.25 + margin }
        val bottomPoints = centers.filter { it.y > imgHeight * 0.75 - margin }

        val corners = mutableListOf<Point>()

        val topLeft = leftPoints.intersect(topPoints).firstOrNull()
            ?: topPoints.minByOrNull { it.x }
            ?: leftPoints.minByOrNull { it.y }
        
        val topRight = rightPoints.intersect(topPoints).firstOrNull()
            ?: topPoints.maxByOrNull { it.x }
            ?: rightPoints.minByOrNull { it.y }
        
        val bottomRight = rightPoints.intersect(bottomPoints).firstOrNull()
            ?: bottomPoints.maxByOrNull { it.x }
            ?: rightPoints.maxByOrNull { it.y }
        
        val bottomLeft = leftPoints.intersect(bottomPoints).firstOrNull()
            ?: bottomPoints.minByOrNull { it.x }
            ?: leftPoints.maxByOrNull { it.y }

        if (topLeft != null) corners.add(topLeft)
        if (topRight != null) corners.add(topRight)
        if (bottomRight != null) corners.add(bottomRight)
        if (bottomLeft != null) corners.add(bottomLeft)

        if (corners.size < 4 && centers.size >= 4) {
            return centers.take(4)
        }

        return corners
    }

    private fun sortCornerPoints(points: List<Point>): List<Point> {
        if (points.size != 4) return points

        val centerX = points.map { it.x }.average()
        val centerY = points.map { it.y }.average()

        val sorted = points.sortedBy { 
            atan2(it.y - centerY, it.x - centerX)
        }

        val idx = sorted.indexOfFirst { it.x < centerX && it.y < centerY }
        if (idx != -1) {
            val rotated = sorted.drop(idx) + sorted.take(idx)
            return listOf(rotated[0], rotated[3], rotated[2], rotated[1])
        }

        val minY = sorted.minBy { it.y }
        val topLeft = points.filter { it.y == minY.y }.minBy { it.x }
        val topRight = points.filter { it.y == minY.y }.maxBy { it.x }
        
        val maxY = sorted.maxBy { it.y }
        val bottomLeft = points.filter { it.y == maxY.y }.minBy { it.x }
        val bottomRight = points.filter { it.y == maxY.y }.maxBy { it.x }

        return listOf(topLeft, topRight, bottomRight, bottomLeft)
    }

    private fun getFallbackCorners(mat: Mat): MatOfPoint2f {
        val margin = 50.0
        val w = mat.width().toDouble()
        val h = mat.height().toDouble()

        return MatOfPoint2f(
            Point(margin, margin),
            Point(w - margin, margin),
            Point(w - margin, h - margin),
            Point(margin, h - margin)
        )
    }

    private data class Quadrilateral(val points: List<Point>, val quality: Double)
}
