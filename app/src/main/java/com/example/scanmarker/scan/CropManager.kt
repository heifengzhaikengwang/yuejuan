package com.example.scanmarker.scan

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.scanmarker.CropBox
import com.example.scanmarker.MarkActivity
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream

class CropManager {

    private val paperAligner = PaperAligner()

    fun cropAllQuestions(
        mat: Mat,
        outputDir: File,
        studentInfo: MarkActivity.StudentInfo = MarkActivity.StudentInfo(),
        templateRatio: Double = 0.0,
        questionRows: Int = 5,
        questionCols: Int = 2
    ): List<File> {
        val croppedImages = mutableListOf<File>()

        val adjustedMat = if (templateRatio > 0.0) {
            paperAligner.adjustToTemplateRatio(mat, templateRatio)
        } else {
            mat
        }

        val resizedMat = paperAligner.resizeToReference(adjustedMat)

        val marginTop = (150 * PaperAligner.REFERENCE_WIDTH / resizedMat.cols()).toInt()
        val marginBottom = (100 * PaperAligner.REFERENCE_WIDTH / resizedMat.cols()).toInt()
        val marginLeft = (80 * PaperAligner.REFERENCE_WIDTH / resizedMat.cols()).toInt()
        val marginRight = (80 * PaperAligner.REFERENCE_WIDTH / resizedMat.cols()).toInt()

        val availableHeight = resizedMat.height() - marginTop - marginBottom
        val availableWidth = resizedMat.width() - marginLeft - marginRight

        val questionHeight = availableHeight / questionRows
        val questionWidth = availableWidth / questionCols

        val gapVertical = (20 * PaperAligner.REFERENCE_WIDTH / resizedMat.cols()).toInt()
        val gapHorizontal = (30 * PaperAligner.REFERENCE_WIDTH / resizedMat.cols()).toInt()

        val studentPrefix = buildStudentPrefix(studentInfo)

        for (row in 0 until questionRows) {
            for (col in 0 until questionCols) {
                val questionNum = row * questionCols + col + 1

                val x = marginLeft + col * questionWidth + gapHorizontal / 2
                val y = marginTop + row * questionHeight + gapVertical / 2
                val width = questionWidth - gapHorizontal
                val height = questionHeight - gapVertical

                if (x >= 0 && y >= 0 && x + width <= resizedMat.width() && y + height <= resizedMat.height()) {
                    val rect = Rect(x, y, width, height)
                    val croppedMat = Mat(resizedMat, rect)

                    val fileName = "${studentPrefix}question_${questionNum}.jpg"
                    val file = File(outputDir, fileName)
                    saveMatAsImage(croppedMat, file)
                    croppedImages.add(file)

                    croppedMat.release()
                }
            }
        }

        if (resizedMat !== mat) {
            resizedMat.release()
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
        studentInfo: MarkActivity.StudentInfo = MarkActivity.StudentInfo(),
        templateRatio: Double = 0.0
    ): List<File> {
        val croppedImages = mutableListOf<File>()

        val adjustedMat = if (templateRatio > 0.0) {
            paperAligner.adjustToTemplateRatio(mat, templateRatio)
        } else {
            mat
        }

        val resizedMat = paperAligner.resizeToReference(adjustedMat)

        val scaleX = PaperAligner.REFERENCE_WIDTH / resizedMat.cols()
        val scaleY = PaperAligner.REFERENCE_WIDTH / resizedMat.rows() / (resizedMat.cols().toDouble() / resizedMat.rows().toDouble())

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
                scaledRect.width().toInt().coerceAtMost(resizedMat.width()),
                scaledRect.height().toInt().coerceAtMost(resizedMat.height())
            )

            if (intRect.x + intRect.width <= resizedMat.width() && intRect.y + intRect.height <= resizedMat.height()) {
                val croppedMat = Mat(resizedMat, intRect)

                val fileName = "${studentPrefix}${box.name}_${index + 1}.jpg"
                val file = File(outputDir, fileName)
                saveMatAsImage(croppedMat, file)
                croppedImages.add(file)

                croppedMat.release()
            }
        }

        if (resizedMat !== mat) {
            resizedMat.release()
        }
        if (adjustedMat !== mat) {
            adjustedMat.release()
        }

        return croppedImages
    }

    fun cropSingleQuestion(
        mat: Mat,
        row: Int,
        col: Int,
        templateRatio: Double = 0.0,
        questionRows: Int = 5,
        questionCols: Int = 2
    ): Mat {
        val adjustedMat = if (templateRatio > 0.0) {
            paperAligner.adjustToTemplateRatio(mat, templateRatio)
        } else {
            mat
        }

        val resizedMat = paperAligner.resizeToReference(adjustedMat)

        val marginTop = (150 * PaperAligner.REFERENCE_WIDTH / resizedMat.cols()).toInt()
        val marginBottom = (100 * PaperAligner.REFERENCE_WIDTH / resizedMat.cols()).toInt()
        val marginLeft = (80 * PaperAligner.REFERENCE_WIDTH / resizedMat.cols()).toInt()
        val marginRight = (80 * PaperAligner.REFERENCE_WIDTH / resizedMat.cols()).toInt()

        val availableHeight = resizedMat.height() - marginTop - marginBottom
        val availableWidth = resizedMat.width() - marginLeft - marginRight

        val questionHeight = availableHeight / questionRows
        val questionWidth = availableWidth / questionCols

        val gapVertical = (20 * PaperAligner.REFERENCE_WIDTH / resizedMat.cols()).toInt()
        val gapHorizontal = (30 * PaperAligner.REFERENCE_WIDTH / resizedMat.cols()).toInt()

        val x = marginLeft + col * questionWidth + gapHorizontal / 2
        val y = marginTop + row * questionHeight + gapVertical / 2
        val width = questionWidth - gapHorizontal
        val height = questionHeight - gapVertical

        val rect = Rect(x, y, width, height)
        val croppedMat = Mat(resizedMat, rect)

        if (resizedMat !== mat) {
            resizedMat.release()
        }
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
