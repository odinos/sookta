package com.kdev.sookta.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class ImageClassifierHelper(
    private val context: Context,
    // !! สำคัญ: เปลี่ยน "your_model.tflite" เป็นชื่อไฟล์โมเดลของคุณ
    private val modelName: String = "your_model.tflite",
    private val maxResults: Int = 3
) {

    private var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(0.5f) // ตั้งค่าความมั่นใจขั้นต่ำ
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(4)
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            // สร้างตัวประมวลผลจากไฟล์โมเดลใน assets
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelName,
                optionsBuilder.build()
            )
        } catch (e: IllegalStateException) {
            Log.e("TFLite", "Error setting up image classifier: ${e.message}")
        }
    }

    fun classify(bitmap: Bitmap): String {
        if (imageClassifier == null) {
            setupImageClassifier()
            if (imageClassifier == null) return "Classifier not initialized."
        }

        // เตรียมรูปภาพก่อนส่งเข้าโมเดล
        val imageProcessor = ImageProcessor.Builder()
            // !! สำคัญ: ปรับขนาด 224x224 ให้ตรงกับขนาดที่โมเดลของคุณต้องการ
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        // สั่งให้โมเดลทำงาน
        val results = imageClassifier?.classify(tensorImage)

        // จัดรูปแบบผลลัพธ์เพื่อนำไปแสดง
        return formatResults(results)
    }

    private fun formatResults(results: List<org.tensorflow.lite.task.vision.classifier.Classifications>?): String {
        if (results.isNullOrEmpty()) {
            return "No results found."
        }
        val sb = StringBuilder()
        results.forEach { classification ->
            classification.categories.forEach { category ->
                sb.append("${category.label}: ${"%.2f".format(category.score)}\n")
            }
        }
        return sb.toString()
    }

    // ฟังก์ชันสำหรับปิดการทำงานของ classifier เพื่อคืนหน่วยความจำ
    fun close() {
        imageClassifier?.close()
        imageClassifier = null
    }
}