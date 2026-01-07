package com.kdev.sookta.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
enum class RiskLevel(val label: String, val colorHex: Long) : Parcelable {
    LOW("ความเสี่ยงต่ำ", 0xFF4CAF50),       // สำหรับ Logic ภายใน
    MEDIUM("ความเสี่ยงปานกลาง", 0xFFFFC107), // สำหรับ Logic ภายใน
    HIGH("ความเสี่ยงสูง", 0xFFF44336),       // สำหรับ Logic ภายใน
    VERY_HIGH("ความเสี่ยงสูงมาก", 0xFFB71C1C) // สำหรับ Logic ภายใน
}

// **เพิ่ม: Enum ระบุตำแหน่งร่างกายสำหรับ Body Map**
enum class BodyPart {
    NECK, TRUNK, LEGS, ARMS, WRISTS
}

enum class JobType : Serializable {
    LIFTING,    // งานยก (ISO 11228-1)
    PUSH_PULL,  // งานผลัก/ดึง (ISO 11228-2)
    REBA        // งานประเมินท่าทาง (REBA)
}

data class ErgoInputData(
    val jobType: JobType,
    val gender: String = "male",
    // **เพิ่ม: รายได้ต่อวัน (บาท) กำหนดค่าเริ่มต้นเป็น 300 (ค่าแรงขั้นต่ำ)**
    val dailyIncome: Double = 300.0,

    // Lifting
    val loadWeight: Double = 0.0,
    val horizontalDist: Double = 25.0,
    val verticalHeight: Double = 75.0,
    val liftFrequency: Double = 0.2,
    val durationHours: Double = 1.0,
    // **Feature Request: ระยะทางขนย้าย (เมตร)**
    val transportDistance: Double = 0.0,
    // Push/Pull
    val initialForce: Double = 0.0,
    val sustainForce: Double = 0.0
) : Serializable

@Parcelize
data class RebaInputData(
    val dailyIncome: Double = 300.0,
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
    val riskLevel: RiskLevel,     // ระดับความเสี่ยงมาตรฐาน (Technical)
    val techScore: Double,        // คะแนนดิบทางเทคนิค (เช่น REBA Score 1-15)

    // **ส่วนที่ปรับปรุง: ระบบคะแนน User 1-9**
    val userScore: Int,           // คะแนนแบบเข้าใจง่าย 1-9
    val userScoreColor: Long,     // สีไล่ระดับตามคะแนน 1-9

    val limitValue: Double,
    val suggestion: String,
    val economicLoss: Int = 0,
    val suggestionList: List<String> = emptyList(),
    val bodyPartRisks: Map<BodyPart, RiskLevel> = emptyMap()
) : Parcelable