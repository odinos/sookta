package com.kdev.sookta.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
enum class RiskLevel(val label: String, val colorHex: Long) : Parcelable {
    LOW("ความเสี่ยงต่ำ", 0xFF4CAF50),      // เขียว
    MEDIUM("ความเสี่ยงปานกลาง", 0xFFFFC107), // เหลือง
    HIGH("ความเสี่ยงสูง", 0xFFF44336),      // แดง
    VERY_HIGH("ความเสี่ยงสูงมาก", 0xFFB71C1C) // **เพิ่ม: แดงเข้ม สำหรับ REBA Score > 11**
}

enum class JobType : Serializable {
    LIFTING,    // งานยก (ISO 11228-1)
    PUSH_PULL,  // งานผลัก/ดึง (ISO 11228-2)
    REBA        // **เพิ่ม: งานประเมินท่าทาง (REBA)**
}

data class ErgoInputData(
    val jobType: JobType,
    val gender: String = "male",

    // สำหรับ Lifting (ISO 11228-1)
    val loadWeight: Double = 0.0,
    val horizontalDist: Double = 25.0,
    val verticalHeight: Double = 75.0,
    val liftFrequency: Double = 0.2,
    val durationHours: Double = 1.0,

    // สำหรับ Push/Pull (ISO 11228-2)
    val initialForce: Double = 0.0,
    val sustainForce: Double = 0.0
) : Serializable

// **เพิ่ม: Data Class สำหรับรับค่า REBA แยกออกมาเพื่อให้ชัดเจน**
@Parcelize
data class RebaInputData(
    val trunkScore: Int = 1,
    val neckScore: Int = 1,
    val legScore: Int = 1,
    val upperArmScore: Int = 1,
    val lowerArmScore: Int = 1,
    val wristScore: Int = 1,
    val loadScore: Int = 0,
    val couplingScore: Int = 0,
    val activityScore: Int = 0
) : Parcelable

@Parcelize
data class ErgoResult(
    val riskLevel: RiskLevel,
    val score: Double,
    val limitValue: Double,
    val suggestion: String
) : Parcelable