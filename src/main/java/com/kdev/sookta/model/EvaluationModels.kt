package com.kdev.sookta.model
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import java.io.Serializable

@Parcelize
enum class RiskLevel (val label: String, val colorHex: Long) : Parcelable {
    LOW("ความเสี่ยงต่ำ", 0xFF4CAF50),      // เขียว
    MEDIUM("ความเสี่ยงปานกลาง", 0xFFFFC107), // เหลือง
    HIGH("ความเสี่ยงสูง", 0xFFF44336)      // แดง
}


enum class JobType : Serializable {
    LIFTING, // งานยก (ISO 11228-1)
    PUSH_PULL // งานผลัก/ดึง (ISO 11228-2)
}


data class ErgoInputData(
    val jobType: JobType,
    val gender: String = "male", // "male" หรือ "female" (ดึงจาก Profile)

    // สำหรับ Lifting (ISO 11228-1)
    val loadWeight: Double = 0.0,    // น้ำหนักวัตถุ (kg)
    val horizontalDist: Double = 25.0, // ระยะห่างตัว (cm) H
    val verticalHeight: Double = 75.0, // ความสูงจุดยก (cm) V
    val liftFrequency: Double = 0.2,   // ความถี่ (ครั้ง/นาที) F
    val durationHours: Double = 1.0,   // ระยะเวลาทำงาน (ชม.)

    // สำหรับ Push/Pull (ISO 11228-2)
    val initialForce: Double = 0.0,  // แรงเริ่มต้น (N)
    val sustainForce: Double = 0.0   // แรงขณะเข็น (N)
) : Serializable

// Data Class เก็บผลลัพธ์
@Parcelize
data class ErgoResult(
    val riskLevel: RiskLevel,
    val score: Double, // ค่า LI หรือ Ratio ของ Force
    val limitValue: Double, // ค่า RWL หรือ Force Limit
    val suggestion: String // คำแนะนำเบื้องต้น
) : Parcelable