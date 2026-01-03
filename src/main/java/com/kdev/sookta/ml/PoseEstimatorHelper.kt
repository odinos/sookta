package com.kdev.sookta.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import com.kdev.sookta.model.RebaInputData
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Helper class สำหรับใช้ AI (MoveNet Thunder) ประเมินท่าทาง
 * ใช้ Library: com.google.ai.edge.litert
 */
class PoseEstimatorHelper(val context: Context) {

    private var interpreter: Interpreter? = null
    private var imageProcessor: ImageProcessor? = null

    // MoveNet Thunder ต้องการ input ขนาด 256x256
    private val inputSize = 256

    init {
        setupInterpreter()
    }

    private fun setupInterpreter() {
        try {
            // โหลดโมเดลจาก assets
            val model = FileUtil.loadMappedFile(context, "movenet_thunder.tflite")

            // ตั้งค่า Interpreter (สามารถเพิ่ม GPU Delegate ตรงนี้ได้ถ้าต้องการ)
            val options = Interpreter.Options()
            options.setNumThreads(4) // ใช้ CPU 4 threads

            interpreter = Interpreter(model, options)

            // เตรียม ImageProcessor เพื่อย่อรูปให้ได้ขนาด 256x256
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * ฟังก์ชันหลัก: รับ Bitmap -> คืนค่า RebaInputData ที่คำนวณคะแนนแล้ว
     */
    fun estimatePoseAndGetReba(bitmap: Bitmap, dailyIncome: Double): RebaInputData {
        if (interpreter == null) return RebaInputData(dailyIncome = dailyIncome)

        return try {
            // 1. เตรียม Input Image
            var tensorImage = TensorImage(DataType.UINT8)
            tensorImage.load(bitmap)
            tensorImage = imageProcessor!!.process(tensorImage)

            // 2. เตรียม Output Buffer
            // MoveNet Thunder Output shape: [1, 1, 17, 3] (Batch, Person, Keypoints, [y, x, score])
            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1, 17, 3), DataType.FLOAT32)

            // 3. รัน Inference
            interpreter?.run(tensorImage.buffer, outputBuffer.buffer.rewind())

            // 4. ดึงค่า Keypoints
            val keypoints = outputBuffer.floatArray
            val points = extractKeypoints(keypoints, bitmap.width, bitmap.height)

            // 5. คำนวณองศาและแปลงเป็นคะแนน REBA
            calculateRebaScores(points, dailyIncome)

        } catch (e: Exception) {
            e.printStackTrace()
            RebaInputData(dailyIncome = dailyIncome) // Fallback กรณี Error
        }
    }

    /**
     * แปลง Raw FloatArray เป็น Map ของจุดต่างๆ (x, y)
     */
    private fun extractKeypoints(raw: FloatArray, imgWidth: Int, imgHeight: Int): Map<String, PointF> {
        // ลำดับจุดตามมาตรฐาน MoveNet
        val bodyParts = listOf(
            "nose", "left_eye", "right_eye", "left_ear", "right_ear",
            "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
            "left_wrist", "right_wrist", "left_hip", "right_hip",
            "left_knee", "right_knee", "left_ankle", "right_ankle"
        )

        val map = mutableMapOf<String, PointF>()

        // ข้อมูลจะเรียงต่อกันชุดละ 3 ค่า (y, x, score)
        // MoveNet Thunder [1, 1, 17, 3] -> Flat array size = 51
        for (i in bodyParts.indices) {
            val y = raw[i * 3]
            val x = raw[i * 3 + 1]
            val score = raw[i * 3 + 2]

            // กรองความมั่นใจ (Confidence Score) > 0.2 ถึงจะนับว่าเจอจุด
            if (score > 0.2f) {
                // x, y เป็นค่า 0.0-1.0 ต้องคูณขนาดภาพจริง
                map[bodyParts[i]] = PointF(x * imgWidth, y * imgHeight)
            }
        }
        return map
    }

    /**
     * คำนวณองศาและคะแนน REBA
     */
    private fun calculateRebaScores(points: Map<String, PointF>, dailyIncome: Double): RebaInputData {
        // ตรวจสอบว่าจะใช้ด้านซ้ายหรือขวา (ในที่นี้เช็คด้านขวาก่อน ถ้าไม่มีไปเช็คซ้าย)
        val isRightSide = points["right_shoulder"] != null && points["right_hip"] != null
        val side = if (isRightSide) "right" else "left"

        val pEar = points["${side}_ear"]
        val pShoulder = points["${side}_shoulder"]
        val pHip = points["${side}_hip"]
        val pKnee = points["${side}_knee"]
        val pElbow = points["${side}_elbow"]
        val pWrist = points["${side}_wrist"]

        // ถ้าหาจุดหลักไม่เจอ คืนค่า Default
        if (pShoulder == null || pHip == null) return RebaInputData(dailyIncome = dailyIncome)

        // --- 1. Trunk Score (ลำตัว) ---
        // วัดมุมก้มของลำตัวเทียบกับแนวตั้งฉาก
        val trunkAngle = calculateVerticalAngle(pHip, pShoulder)
        val trunkScore = when {
            trunkAngle <= 5 -> 1      // ยืนตรง
            trunkAngle <= 20 -> 2     // ก้มเล็กน้อย
            trunkAngle <= 60 -> 3     // ก้มปานกลาง
            else -> 4                 // ก้มมาก (>60)
        }

        // --- 2. Neck Score (คอ) ---
        // วัดมุมศีรษะเทียบกับแนวตั้ง (ใช้ไหล่->หู)
        val neckAngle = if (pEar != null) calculateVerticalAngle(pShoulder, pEar) else 0.0
        val neckScore = when {
            neckAngle <= 20 -> 1
            else -> 2
        }

        // --- 3. Upper Arm Score (ต้นแขน) ---
        // วัดมุมต้นแขนเทียบกับแนวลำตัว (ใช้ไหล่->ศอก)
        val upperArmAngle = if (pElbow != null) calculateVerticalAngle(pShoulder, pElbow) else 0.0
        val upperArmScore = when {
            upperArmAngle <= 20 -> 1
            upperArmAngle <= 45 -> 2
            upperArmAngle <= 90 -> 3
            else -> 4
        }

        // --- 4. Leg Score (ขา) ---
        // ถ้ามุมเข่าไม่อยู่ในแนวตรงกับสะโพกและข้อเท้า อาจแปลว่านั่งหรือยืนย่อเข่า
        // (ส่วนนี้ AI ประเมินยากถ้าไม่เห็นขา ให้ Default = 1 ไว้ก่อน หรือใช้ Logic มุมเข่า)
        val legScore = 1

        // หมายเหตุ: ส่วน Lower Arm, Wrist, Load, Coupling ประเมินจากรูปยาก ให้ใช้ค่า Default หรือรอ User ปรับแก้
        return RebaInputData(
            dailyIncome = dailyIncome,
            trunkScore = trunkScore,
            neckScore = neckScore,
            legScore = legScore,
            upperArmScore = upperArmScore,
            lowerArmScore = 1,
            wristScore = 1,
            loadScore = 0,
            couplingScore = 0,
            activityScore = 0
        )
    }

    /**
     * คำนวณมุมระหว่างเวกเตอร์ p1->p2 กับแนวตั้งฉาก (Vertical Axis)
     * เหมาะสำหรับหา Trunk Flexion (สะโพก->ไหล่) หรือ Neck Flexion
     */
    private fun calculateVerticalAngle(p1: PointF, p2: PointF): Double {
        val dy = p2.y - p1.y
        val dx = p2.x - p1.x
        // atan2(dx, -dy) เทียบกับแกน Y กลับหัวของ Android
        val radians = atan2(dx.toDouble(), -dy.toDouble())
        return abs(Math.toDegrees(radians))
    }

    // ปิด Interpreter เมื่อไม่ใช้ (เรียกใน OnDestroy ของ Activity/Fragment ถ้าทำได้)
    fun close() {
        interpreter?.close()
    }
}