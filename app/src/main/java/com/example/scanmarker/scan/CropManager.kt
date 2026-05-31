package com.example.scanmarker.scan

import android.graphics.Bitmap
import com.example.scanmarker.CropBox
import com.example.scanmarker.MarkActivity
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream

class CropManager {

    private val paperAligner = PaperAligner()

    fun cropAllQuestions(
        mat: Mat,
        outputDir: File,
        studentInfo: MarkActivity.StudentInfo = MarkActivity.StudentInfo(),
        questionRows: Int = 5,
        questionCols: Int = 2
    ): List<File> {
        val croppedImages = mutableListOf<File>()

        val adjustedMat = prepareForCropping(mat)

        val marginTop = (150 * PaperAligner.REFERENCE_WIDTH / adjustedMat.cols()).toInt()
        val marginBottom = (100 * PaperAligner.REFERENCE_WIDTH / adjustedMat.cols()).toInt()
        val marginLeft = (80 * PaperAligner.REFERENCE_WIDTH / adjustedMat.cols()).toInt()
        val marginRight = (80 * PaperAligner.REFERENCE_WIDTH / adjustedMat.cols()).toInt()

        val availableHeight = adjustedMat.height() - marginTop - marginBottom
        val availableWidth = adjustedMat.width() - marginLeft - marginRight

        val questionHeight = availableHeight / questionRows
        val questionWidth = availableWidth / questionCols

        val gapVertical = (20 * PaperAligner.REFERENCE_WIDTH / adjustedMat.cols()).toInt()
        val gapHorizontal = (30 * PaperAligner.REFERENCE_WIDTH / adjustedMat.cols()).toInt()

        val studentPrefix = buildStudentPrefix(studentInfo)

        for (row in 0 until questionRows) {
            for (col in 0 until questionCols) {
                val questionNum = row * questionCols + col + 1

                val x = marginLeft + col * questionWidth + gapHorizontal / 2
                val y = marginTop + row * questionHeight + gapVertical / 2
                val width = questionWidth - gapHorizontal
                val height = questionHeight - gapVertical

                if (x >= 0 && y >= 0 && x + width <= adjustedMat.width() && y + height <= adjustedMat.height()) {
                    val rect = Rect(x, y, width, height)
                    val croppedMat = Mat(adjustedMat, rect)

                    val fileName = "${studentPrefix}question_${questionNum}.jpg"
                    val file = File(outputDir, fileName)
                    saveMatAsImage(croppedMat, file)
                    croppedImages.add(file)

                    croppedMat.release()
                }
            }
        }

        if (adjustedMat !== mat) {
            adjustedMat.release()
        }

        return croppedImages
    }

    fun cropWithCustomBoxes(
        mat: Mat,
        outputDir: File,
        cropBoxes: List<CropBox>,
        studentInfo: MarkActivity.StudentInfo = MarkActivity.StudentInfo()
    ): List<File> {
        val croppedImages = mutableListOf<File>()

        val adjustedMat = prepareForCropping(mat)

        val scaleX = PaperAligner.REFERENCE_WIDTH / mat.cols()
        val scaleY = (PaperAligner.REFERENCE_WIDTH / PaperAligner.A4_RATIO) / mat.rows()

        val studentPrefix = buildStudentPrefix(studentInfo)

        cropBoxes.forEachIndexed { index, box ->
            val scaledRect = RectF(
                box.left * scaleX,
                box.top * scaleY,
                box.right * scaleX,
                box.bottom * scaleY
            )

            val intRect = Rect(
                scaledRect.left.toInt().coerceAtLeast(0),
                scaledRect.top.toInt().coerceAtLeast(0),
                scaledRect.width().toInt().coerceAtMost(adjustedMat.width()),
                scaledRect.height().toInt().coerceAtMost(adjustedMat.height())
            )

            if (intRect.x + intRect.width <= adjustedMat.width() && intRect.y + intRect.height <= adjustedMat.height()) {
                val croppedMat = Mat(adjustedMat, intRect)

                val fileName = "${studentPrefix}${box.name}_${index + 1}.jpg"
                val file = File(outputDir, fileName)
                saveMatAsImage(croppedMat, file)
                croppedImages.add(file)

                croppedMat.release()
            }
        }

        if (adjustedMat !== mat) {
            adjustedMat.release()
        }

        return croppedImages
    }

    private fun prepareForCropping(mat: Mat): Mat {
        val adjustedMat = paperAligner.adjustToA4Ratio(mat)
        val resizedMat = paperAligner.resizeToReference(adjustedMat)

        if (adjustedMat !== mat) {
            adjustedMat.release()
        }

        return resizedMat
    }

    fun cropSingleQuestion(mat: Mat, row: Int, col: Int, questionRows: Int = 5, questionCols: Int = 2): Mat {
        val adjustedMat = prepareForCropping(mat)

        val marginTop = (150 * PaperAligner.REFERENCE_WIDTH / adjustedMat.cols()).toInt()
        val marginBottom = (100 * PaperAligner.REFERENCE_WIDTH / adjustedMat.cols()).toInt()
        val marginLeft = (80 * PaperAligner.REFERENCE_WIDTH / adjustedMat.cols()).toInt()
        val marginRight = (80 * PaperAligner.REFERENCE_WIDTH / adjustedMat.cols()).toInt()

        val availableHeight = adjustedMat.height() - marginTop - marginBottom
        val availableWidth = adjustedMat.width() - marginLeft - marginRight

        val questionHeight = availableHeight / questionRows
        val questionWidth = availableWidth / questionCols

        val gapVertical = (20 * PaperAligner.REFERENCE_WIDTH / adjustedMat.cols()).toInt()
        val gapHorizontal = (30 * PaperAligner.REFERENCE_WIDTH / adjustedMat.cols()).toInt()

        val x = marginLeft + col * questionWidth + gapHorizontal / 2
        val y = marginTop + row * questionHeight + gapVertical / 2
        val width = questionWidth - gapHorizontal
        val height = questionHeight - gapVertical

        val rect = Rect(x, y, width, height)
        val croppedMat = Mat(adjustedMat, rect)

        if (adjustedMat !== mat) {
            adjustedMat.release()
        }

        return croppedMat
    }

    private fun buildStudentPrefix(studentInfo: MarkActivity.StudentInfo): String {
        val parts = mutableListOf<String>()
        if (studentInfo.studentId.isNotEmpty()) parts.add(studentInfo.studentId)
        if (studentInfo.studentName.isNotEmpty()) parts.add(studentInfo.studentName)
        return if (parts.isNotEmpty()) "${parts.joinToString("_")}_" else ""
    }

    private fun saveMatAsImage(mat: Mat, file: File) {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)

        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        fos.flush()
        fos.close()
        bitmap.recycle()
    }
}
