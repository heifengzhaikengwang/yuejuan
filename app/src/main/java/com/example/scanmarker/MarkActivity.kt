package com.example.scanmarker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MarkActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private var photoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mark)

        imageView = findViewById(R.id.imageView)
        val retakeButton = findViewById<Button>(R.id.retakeButton)
        val confirmButton = findViewById<Button>(R.id.confirmButton)

        photoPath = intent.getStringExtra("photoPath")

        if (photoPath != null) {
            loadImage(photoPath!!)
        } else {
            Toast.makeText(this, "未找到图片", Toast.LENGTH_SHORT).show()
            finish()
        }

        retakeButton.setOnClickListener {
            finish()
        }

        confirmButton.setOnClickListener {
            Toast.makeText(this, "图片已保存", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadImage(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                imageView.setImageBitmap(bitmap)
            } else {
                Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "加载图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
