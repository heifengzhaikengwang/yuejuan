package com.example.scanmarker

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.example.scanmarker.scan.CornerDetector
import com.example.scanmarker.scan.CropManager
import com.example.scanmarker.scan.PaperAligner
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import java.io.File
import java.io.FileOutputStream

class MarkActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var configCropBtn: Button
    private lateinit var cropPaperButton: Button
    private lateinit var cropQuestionsButton: Button
    private lateinit var saveButton: Button
    private lateinit var retakeButton: Button
    private lateinit var backButton: Button

    private var photoPath: String? = null
    private var originalBitmap: Bitmap? = null
    private var croppedPaperBitmap: Bitmap? = null
    private var croppedPaperMat: Mat? = null

    private var studentInfo = StudentInfo()

    private val cornerDetector = CornerDetector()
    private val paperAligner = PaperAligner()

    data class StudentInfo(
        var studentId: String = "1",
        var studentName: String = "学生1",
        var classInfo: String = "1班",
        var batchId: String = System.currentTimeMillis().toString()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mark)

        initViews()
        loadPhoto()
        showStudentInfoDialog()
        setupListeners()
    }

    private fun initViews() {
        imageView = findViewById(R.id.imageView)
        configCropBtn = findViewById(R.id.configCropBtn)
        cropPaperButton = findViewById(R.id.processButton)
        cropQuestionsButton = findViewById(R.id.cropButton)
        saveButton = findViewById(R.id.saveButton)
        retakeButton = findViewById(R.id.retakeButton)
        backButton = findViewById(R.id.backButton)
        
        cropPaperButton.text = "裁掉答题卡多余部分"
        cropQuestionsButton.text = "裁切题目"
        cropQuestionsButton.isEnabled = false
    }

    private fun showStudentInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_student_info, null)
        
        val studentIdEdit = dialogView.findViewById<android.widget.EditText>(R.id.editStudentId)
        val studentNameEdit = dialogView.findViewById<android.widget.EditText>(R.id.editStudentName)
        val classEdit = dialogView.findViewById<android.widget.EditText>(R.id.editClass)

        studentIdEdit.setText(studentInfo.studentId)
        studentNameEdit.setText(studentInfo.studentName)
        classEdit.setText(studentInfo.classInfo)

        val dialog = AlertDialog.Builder(this)
            .setTitle("学生信息")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val newStudentId = studentIdEdit.text.toString()
                val newStudentName = studentNameEdit.text.toString()
                val newClassInfo = classEdit.text.toString()
                
                studentInfo.studentId = if (newStudentId.isNotEmpty()) newStudentId else studentInfo.studentId
                studentInfo.studentName = if (newStudentName.isNotEmpty()) newStudentName else studentInfo.studentName
                studentInfo.classInfo = if (newClassInfo.isNotEmpty()) newClassInfo else studentInfo.classInfo
                
                Toast.makeText(this, "学生信息已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("跳过") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun loadPhoto() {
        photoPath = intent.getStringExtra("photoPath")

        if (photoPath != null) {
            try {
                val file = File(photoPath!!)
                if (file.exists()) {
                    originalBitmap = BitmapFactory.decodeFile(photoPath!!)
                    imageView.setImageBitmap(originalBitmap)
                } else {
                    Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "未找到图片", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupListeners() {
        configCropBtn.setOnClickListener {
            openCropConfig()
        }

        cropPaperButton.setOnClickListener {
            cropPaper()
        }

        cropQuestionsButton.setOnClickListener {
            cropQuestions()
        }

        saveButton.setOnClickListener {
            saveResult()
        }

        retakeButton.setOnClickListener {
            finish()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun openCropConfig() {
        val intent = Intent(this, CropConfigActivity::class.java)
        intent.putExtra(CropConfigActivity.EXTRA_IMAGE_PATH, photoPath)
        startActivity(intent)
    }

    private fun cropPaper() {
        if (originalBitmap == null) {
            Toast.makeText(this, "请先加载图片", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val mat = Mat()
            Utils.bitmapToMat(originalBitmap, mat)

            val corners: MatOfPoint2f = cornerDetector.detect(mat)
            croppedPaperMat = paperAligner.align(mat, corners)

            croppedPaperBitmap = Bitmap.createBitmap(
                croppedPaperMat!!.cols(), 
                croppedPaperMat!!.rows(), 
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(croppedPaperMat, croppedPaperBitmap)

            imageView.setImageBitmap(croppedPaperBitmap)
            cropQuestionsButton.isEnabled = true
            
            mat.release()
            
            Toast.makeText(this, "已裁掉答题卡多余部分，现在可以裁切题目了", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "裁掉答题卡多余部分失败: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun cropQuestions() {
        if (croppedPaperMat == null) {
            Toast.makeText(this, "请先裁掉答题卡多余部分", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            Toast.makeText(this, "正在裁切题目...", Toast.LENGTH_SHORT).show()

            val folderName = buildStudentFolderName()
            val outputDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ScanMarker/$folderName"
            )
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val cropBoxes = CropConfigActivity.getCropBoxes(this)
            val croppedFiles = if (cropBoxes.isNotEmpty()) {
                CropManager().cropWithCustomBoxes(croppedPaperMat!!, outputDir, cropBoxes, studentInfo)
            } else {
                CropManager().cropAllQuestions(croppedPaperMat!!, outputDir, studentInfo)
            }

            saveButton.isEnabled = true

            Toast.makeText(
                this,
                "题目裁切完成！共 ${croppedFiles.size} 道题目\n学生: ${studentInfo.studentName}\n保存位置: Pictures/ScanMarker/$folderName",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(this, "题目裁切失败: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun buildStudentFolderName(): String {
        val namePart = if (studentInfo.studentName.isNotEmpty()) studentInfo.studentName else "学生1"
        val idPart = if (studentInfo.studentId.isNotEmpty()) "_${studentInfo.studentId}" else "_1"
        val classPart = if (studentInfo.classInfo.isNotEmpty()) "_${studentInfo.classInfo}" else "_1班"
        return "${namePart}${idPart}${classPart}_${studentInfo.batchId}"
    }

    private fun saveResult() {
        Toast.makeText(this, "题目已保存，完成！", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        croppedPaperMat?.release()
    }
}
