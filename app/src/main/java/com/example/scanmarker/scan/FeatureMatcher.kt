package com.example.scanmarker.scan

import org.opencv.core.*
import org.opencv.features2d.*
import org.opencv.imgproc.Imgproc

class FeatureMatcher {

    private val orb = ORB.create(500)
    private val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)

    fun findMatchingCorners(
        templateMat: Mat,
        targetMat: Mat,
        templateCorners: MatOfPoint2f
    ): MatOfPoint2f {
        val templateCornersArr = templateCorners.toArray()
        
        try {
            val kp1 = MatOfKeyPoint()
            val kp2 = MatOfKeyPoint()
            val desc1 = Mat()
            val desc2 = Mat()

            orb.detectAndCompute(templateMat, Mat(), kp1, desc1)
            orb.detectAndCompute(targetMat, Mat(), kp2, desc2)

            if (kp1.size() < 10 || kp2.size() < 10) {
                return tryCornerDetection(targetMat)
            }

            val matches = MatOfDMatch()
            matcher.match(desc1, desc2, matches)

            val matchList = matches.toList()
            val goodMatches = matchList.sortedBy { it.distance }.take(minOf(50, matchList.size))

            if (goodMatches.size < 4) {
                return tryCornerDetection(targetMat)
            }

            val srcPoints = mutableListOf<Point>()
            val dstPoints = mutableListOf<Point>()

            val kp1List = kp1.toList()
            val kp2List = kp2.toList()

            goodMatches.forEach { dm ->
                srcPoints.add(kp1List[dm.queryIdx].pt)
                dstPoints.add(kp2List[dm.trainIdx].pt)
            }

            if (srcPoints.size < 4) {
                return tryCornerDetection(targetMat)
            }

            val srcMat = MatOfPoint2f(*srcPoints.toTypedArray())
            val dstMat = MatOfPoint2f(*dstPoints.toTypedArray())

            val homography = Imgproc.findHomography(
                srcMat,
                dstMat,
                Imgproc.RANSAC,
                5.0
            )

            val targetCorners = MatOfPoint2f()
            Core.perspectiveTransform(templateCorners, targetCorners, homography)

            return targetCorners

        } catch (e: Exception) {
            e.printStackTrace()
            return tryCornerDetection(targetMat)
        }
    }

    private fun tryCornerDetection(mat: Mat): MatOfPoint2f {
        return try {
            val cornerDetector = CornerDetector()
            cornerDetector.detect(mat)
        } catch (e: Exception) {
            e.printStackTrace()
            getFallbackCorners(mat)
        }
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
}
