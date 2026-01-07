package com.kdev.sookta.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import com.kdev.sookta.ml.KeyPoint
import com.kdev.sookta.ml.Person
import com.kdev.sookta.ml.PoseLandmark
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Helper class สำหรับใช้ AI (MoveNet Thunder) ประเมินท่าทาง
 */
class PoseEstimatorHelper(val context: Context) {

    private var interpreter: Interpreter? = null
    private var imageProcessor: ImageProcessor? = null

    // MoveNet Thunder ใช้ Input ขนาด 256x256
    private var inputShape = intArrayOf(1, 256, 256, 3)
    private val inputSize = 256

    init {
        setupInterpreter()
    }

    private fun setupInterpreter() {
        try {
            val model = FileUtil.loadMappedFile(context, "movenet_thunder.tflite")
            val options = Interpreter.Options()
            options.setNumThreads(4)
            interpreter = Interpreter(model, options)

            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * ฟังก์ชันหลัก: รับ Bitmap -> คืนค่า List<Person>
     */
    fun estimatePoses(bitmap: Bitmap): List<Person> {
        if (interpreter == null) setupInterpreter()
        val tflite = interpreter ?: return emptyList()

        val inputHeight = inputShape[1]
        val inputWidth = inputShape[2]
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val inputTensor = tflite.getInputTensor(0)
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap, inputTensor.dataType())

        val outputBuffer = Array(1) { Array(1) { Array(17) { FloatArray(3) } } }
        tflite.run(inputBuffer, outputBuffer)

        val detectedPersons = mutableListOf<Person>()
        val keyPointsData = outputBuffer[0][0]
        val mappedKeyPoints = mutableListOf<KeyPoint>()
        var totalScore = 0f

        for (i in keyPointsData.indices) {
            val y = keyPointsData[i][0]
            val x = keyPointsData[i][1]
            val score = keyPointsData[i][2]

            mappedKeyPoints.add(
                KeyPoint(
                    bodyPart = PoseLandmark.fromInt(i),
                    coordinate = PointF(x, y),
                    score = score
                )
            )
            totalScore += score
        }

        val person = Person(
            id = 0,
            keyPoints = mappedKeyPoints,
            score = totalScore / 17.0f
        )

        if (person.score > 0.2f) {
            detectedPersons.add(person)
        }

        return detectedPersons
    }

    /**
     * แก้ไขข้อ 2: ฟังก์ชันใหม่สำหรับประเมินระยะ H (Horizontal) และ V (Vertical) สำหรับงาน Lifting
     * คืนค่าเป็น Pair(Horizontal_CM, Vertical_CM) หรือ null ถ้าหาไม่เจอ
     */
    fun estimateLiftingDimensions(bitmap: Bitmap): Pair<Double, Double>? {
        val persons = estimatePoses(bitmap)
        val person = persons.firstOrNull() ?: return null

        fun p(bp: PoseLandmark): PointF? = person.keyPoints.find { it.bodyPart == bp && it.score > 0.3f }?.coordinate

        val shoulder = p(PoseLandmark.RIGHT_SHOULDER) ?: p(PoseLandmark.LEFT_SHOULDER)
        val hip = p(PoseLandmark.RIGHT_HIP) ?: p(PoseLandmark.LEFT_HIP)
        val wrist = p(PoseLandmark.RIGHT_WRIST) ?: p(PoseLandmark.LEFT_WRIST)
        val ankle = p(PoseLandmark.RIGHT_ANKLE) ?: p(PoseLandmark.LEFT_ANKLE)

        if (shoulder != null && hip != null && wrist != null && ankle != null) {
            // คำนวณ Scale Factor: อ้างอิงจากความยาวลำตัว (Torso Length) เฉลี่ยประมาณ 53 ซม.
            val torsoPixelDist = distance(shoulder, hip)
            val pixelsPerCm = torsoPixelDist / 53.0

            // H: ระยะห่างแนวนอนระหว่างข้อเท้า (จุดศูนย์ถ่วง) กับข้อมือ (จุดจับของ)
            val hPixels = abs(wrist.x - ankle.x) * bitmap.width // x เป็น 0-1 ต้องคูณ width
            val hCm = (hPixels / pixelsPerCm).coerceIn(25.0, 65.0) // Clamp ค่าตามมาตรฐาน ISO

            // V: ความสูงของมือจากพื้น (ข้อเท้า)
            // Y เพิ่มขึ้นด้านล่าง ดังนั้น (AnkleY - WristY) คือความสูงขึ้นจากพื้น
            val vPixels = (ankle.y - wrist.y) * bitmap.height
            val vCm = (vPixels / pixelsPerCm).coerceIn(0.0, 175.0)

            return Pair(hCm, vCm)
        }
        return null
    }

    private fun distance(p1: PointF, p2: PointF): Double {
        return sqrt((p1.x - p2.x).toDouble().pow(2) + (p1.y - p2.y).toDouble().pow(2))
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap, dataType: DataType): ByteBuffer {
        val inputSize = inputShape[1]
        val safeBitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        val byteBuffer = ByteBuffer.allocateDirect(4 * 1 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)
        safeBitmap.getPixels(intValues, 0, safeBitmap.width, 0, 0, safeBitmap.width, safeBitmap.height)

        for (pixelValue in intValues) {
            byteBuffer.putFloat(((pixelValue shr 16) and 0xFF).toFloat())
            byteBuffer.putFloat(((pixelValue shr 8) and 0xFF).toFloat())
            byteBuffer.putFloat((pixelValue and 0xFF).toFloat())
        }

        if (safeBitmap != bitmap) safeBitmap.recycle()
        return byteBuffer
    }
}