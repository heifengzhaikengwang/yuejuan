package com.example.scanmarker.scan

import android.graphics.Bitmap
import com.example.scanmarker.MarkActivity
import org.opencv.android.Utils
import org.opencv.core.*
import java.io.File
import java.io.FileOutputStream

class CropManager {

    fun cropAllQuestions(
        mat: Mat,
        outputDir: File,
        studentInfo: MarkActivity.StudentInfo = MarkActivity.StudentInfo(),
        questionRows: Int = 5,
        questionCols: Int = 2
    ): List<File> {
        val croppedImages = mutableListOf<File>()

        val marginTop = 150
        val marginBottom = 100
        val marginLeft = 80
        val marginRight = 80

        val availableHeight = mat.height() - marginTop - marginBottom
        val availableWidth = mat.width() - marginLeft - marginRight

        val questionHeight = availableHeight / questionRows
        val questionWidth = availableWidth / questionCols

        val gapVertical = 20
        val gapHorizontal = 30

        val studentPrefix = buildStudentPrefix(studentInfo)

        for (row in 0 until questionRows) {
            for (col in 0 until questionCols) {
                val questionNum = row * questionCols + col + 1

                val x = marginLeft + col * questionWidth + gapHorizontal / 2
                val y = marginTop + row * questionHeight + gapVertical / 2
                val width = questionWidth - gapHorizontal
                val height = questionHeight - gapVertical

                if (x >= 0 && y >= 0 && x + width <= mat.width() && y + height <= mat.height()) {
                    val rect = Rect(x, y, width, height)
                    val croppedMat = Mat(mat, rect)

                    val fileName = "${studentPrefix}question_${questionNum}.jpg"
                    val file = File(outputDir, fileName)
                    saveMatAsImage(croppedMat, file)
                    croppedImages.add(file)

                    croppedMat.release()
                }
            }
        }

        return croppedImages
    }

    private fun buildStudentPrefix(studentInfo: MarkActivity.StudentInfo): String {
        val parts = mutableListOf<String>()
        if (studentInfo.studentId.isNotEmpty()) parts.add(studentInfo.studentId)
        if (studentInfo.studentName.isNotEmpty()) parts.add(studentInfo.studentName)
        return if (parts.isNotEmpty()) "${parts.joinToString("_")}_" else ""
    }

    fun cropSingleQuestion(mat: Mat, row: Int, col: Int, questionRows: Int = 5, questionCols: Int = 2): Mat {
        val marginTop = 150
        val marginBottom = 100
        val marginLeft = 80
        val marginRight = 80

        val availableHeight = mat.height() - marginTop - marginBottom
        val availableWidth = mat.width() - marginLeft - marginRight

        val questionHeight = availableHeight / questionRows
        val questionWidth = availableWidth / questionCols

        val gapVertical = 20
        val gapHorizontal = 30

        val x = marginLeft + col * questionWidth + gapHorizontal / 2
        val y = marginTop + row * questionHeight + gapVertical / 2
        val width = questionWidth - gapHorizontal
        val height = questionHeight - gapVertical

        val rect = Rect(x, y, width, height)
        return Mat(mat, rect)
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
