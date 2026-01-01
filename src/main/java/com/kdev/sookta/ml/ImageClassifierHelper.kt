package com.kdev.sookta.ml

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log

import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
// import org.tensorflow.lite.support.common.ops.NormalizeOp // (เปิดใช้ถ้าจำเป็น)
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException

class ImageClassifierHelper(
    private val context: Context,
    private val modelName: String = "your_model.tflite",       // ชื่อไฟล์โมเดล
    private val labelName: String = "labels.txt",             // ชื่อไฟล์ Label
    private val inputSize: Int = 224                          // ขนาดรูปที่โมเดลต้องการ (ปกติ 224)
) {

    private var interpreter: Interpreter? = null
    private var imageProcessor: ImageProcessor? = null
    private var outputBuffer: TensorBuffer? = null
    private var labels: List<String> = emptyList()

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        try {
            // 1. โหลดโมเดล
            val modelFile = FileUtil.loadMappedFile(context, modelName)
            val options = Interpreter.Options()
            options.setNumThreads(4) // ใช้ 4 core ช่วยประมวลผล

            // สร้าง Interpreter (ตัวรันโมเดล)
            interpreter = Interpreter(modelFile, options)

            // 2. โหลด Labels
            labels = FileUtil.loadLabels(context, labelName)

            // 3. เตรียมตัวจัดการรูปภาพ (Pre-processing)
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                // .add(NormalizeOp(0f, 255f)) // *ถ้าโมเดลต้องการ Normalize (0-1) ให้เปิดบรรทัดนี้
                .build()

            // 4. เตรียมที่เก็บผลลัพธ์ (Output Buffer)
            // เช็ค shape ของ output ตัวแรก
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            val dataType = interpreter!!.getOutputTensor(0).dataType()

            // สร้าง Buffer ให้ขนาดตรงกับที่โมเดลจะพ่นออกมา
            outputBuffer = TensorBuffer.createFixedSize(outputShape, dataType)

            Log.d(TAG, "LiteRT Setup Complete")

        } catch (e: IOException) {
            Log.e(TAG, "Error initializing classifier: ${e.message}")
        }
    }

    fun classify(bitmap: Bitmap): String {
        if (interpreter == null) {
            setupClassifier()
            if (interpreter == null) return "Classifier failed."
        }

        // 1. เตรียมรูป (Convert Bitmap -> TensorImage)
        var tensorImage = TensorImage(interpreter!!.getInputTensor(0).dataType())
        tensorImage.load(bitmap)
        tensorImage = imageProcessor!!.process(tensorImage)

        // 2. รันโมเดล (Inference)
        // รับ Input เป็น Buffer ของรูป, ส่งผลลัพธ์ลง outputBuffer
        val startTime = SystemClock.uptimeMillis()

        interpreter!!.run(tensorImage.buffer, outputBuffer!!.buffer.rewind())

        val inferenceTime = SystemClock.uptimeMillis() - startTime
        Log.d(TAG, "Inference time: $inferenceTime ms")

        // 3. แปลงผลลัพธ์เป็นข้อความ (Post-processing)
        return formatResults()
    }

    private fun formatResults(): String {
        // ใช้ TensorLabel ช่วยจับคู่คะแนนกับชื่อ Label
        val labeledProbability = TensorLabel(labels, outputBuffer!!)
            .mapWithFloatValue

        // เรียงลำดับจากคะแนนมากไปน้อย และเอามาแค่ 3 อันดับแรก
        val topResults = labeledProbability.entries
            .sortedByDescending { it.value }
            .take(3)

        val sb = StringBuilder()
        for ((key, value) in topResults) {
            sb.append("$key: ${"%.2f".format(value * 100)}%\n")
        }
        return sb.toString()
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    companion object {
        private const val TAG = "LiteRT_Helper"
    }
}