package com.example.scanmarker

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
    private lateinit var processButton: Button
    private lateinit var cropButton: Button
    private lateinit var saveButton: Button
    private lateinit var retakeButton: Button
    private lateinit var backButton: Button

    private var photoPath: String? = null
    private var originalBitmap: Bitmap? = null
    private var processedBitmap: Bitmap? = null
    private var alignedMat: Mat? = null

    private var studentInfo = StudentInfo()

    private val cornerDetector = CornerDetector()
    private val paperAligner = PaperAligner()

    data class StudentInfo(
        var studentId: String = "",
        var studentName: String = "",
        var classInfo: String = "",
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
        processButton = findViewById(R.id.processButton)
        cropButton = findViewById(R.id.cropButton)
        saveButton = findViewById(R.id.saveButton)
        retakeButton = findViewById(R.id.retakeButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun showStudentInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_student_info, null)
        
        val studentIdEdit = dialogView.findViewById<android.widget.EditText>(R.id.editStudentId)
        val studentNameEdit = dialogView.findViewById<android.widget.EditText>(R.id.editStudentName)
        val classEdit = dialogView.findViewById<android.widget.EditText>(R.id.editClass)

        val dialog = AlertDialog.Builder(this)
            .setTitle("学生信息")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                studentInfo.studentId = studentIdEdit.text.toString()
                studentInfo.studentName = studentNameEdit.text.toString()
                studentInfo.classInfo = classEdit.text.toString()
                
                if (studentInfo.studentId.isNotEmpty() || studentInfo.studentName.isNotEmpty()) {
                    Toast.makeText(this, "学生信息已保存", Toast.LENGTH_SHORT).show()
                }
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

        processButton.setOnClickListener {
            processImage()
        }

        cropButton.setOnClickListener {
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

    private fun loadImage(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                originalBitmap = BitmapFactory.decodeFile(path)
                imageView.setImageBitmap(originalBitmap)
            } else {
                Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "加载图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processImage() {
        if (originalBitmap == null) {
            Toast.makeText(this, "请先加载图片", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val mat = Mat()
            Utils.bitmapToMat(originalBitmap, mat)

            val corners: MatOfPoint2f = cornerDetector.detect(mat)
            alignedMat = paperAligner.align(mat, corners)

            processedBitmap = Bitmap.createBitmap(alignedMat!!.cols(), alignedMat!!.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(alignedMat, processedBitmap)

            imageView.setImageBitmap(processedBitmap)
            cropButton.isEnabled = true
            Toast.makeText(this, "处理成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "处理失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cropQuestions() {
        if (alignedMat == null) {
            Toast.makeText(this, "请先处理图片", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            Toast.makeText(this, "正在切题...", Toast.LENGTH_SHORT).show()

            val folderName = buildStudentFolderName()
            val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), folderName)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val cropBoxes = CropConfigActivity.getCropBoxes(this)
            val croppedFiles = if (cropBoxes.isNotEmpty()) {
                CropManager().cropWithCustomBoxes(alignedMat!!, outputDir, cropBoxes, studentInfo)
            } else {
                CropManager().cropAllQuestions(alignedMat!!, outputDir, studentInfo)
            }

            saveButton.isEnabled = true

            Toast.makeText(
                this,
                "切题完成！共 ${croppedFiles.size} 道题目\n学生: ${studentInfo.studentName ?: studentInfo.studentId}\n保存位置: ${outputDir.absolutePath}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(this, "切题失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildStudentFolderName(): String {
        val namePart = if (studentInfo.studentName.isNotEmpty()) studentInfo.studentName else "unknown"
        val idPart = if (studentInfo.studentId.isNotEmpty()) "_${studentInfo.studentId}" else ""
        val classPart = if (studentInfo.classInfo.isNotEmpty()) "_${studentInfo.classInfo}" else ""
        return "${namePart}${idPart}${classPart}_${studentInfo.batchId}"
    }

    private fun saveStudentInfo(outputDir: File) {
        val infoFile = File(outputDir, "student_info.txt")
        val content = """
            批次ID: ${studentInfo.batchId}
            学号: ${studentInfo.studentId}
            姓名: ${studentInfo.studentName}
            班级: ${studentInfo.classInfo}
            创建时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}
        """.trimIndent()

        FileOutputStream(infoFile).use { it.write(content.toByteArray()) }
    }

    private fun saveResult() {
        Toast.makeText(this, "切题已完成，题目已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        alignedMat?.release()
    }
}
