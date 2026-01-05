package com.kdev.sookta.ml

import android.graphics.PointF

// Enum ระบุชื่อจุดต่างๆ บนร่างกาย (ตามมาตรฐาน MoveNet 17 จุด)
enum class PoseLandmark(val position: Int) {
    NOSE(0),
    LEFT_EYE(1),
    RIGHT_EYE(2),
    LEFT_EAR(3),
    RIGHT_EAR(4),
    LEFT_SHOULDER(5),
    RIGHT_SHOULDER(6),
    LEFT_ELBOW(7),
    RIGHT_ELBOW(8),
    LEFT_WRIST(9),
    RIGHT_WRIST(10),
    LEFT_HIP(11),
    RIGHT_HIP(12),
    LEFT_KNEE(13),
    RIGHT_KNEE(14),
    LEFT_ANKLE(15),
    RIGHT_ANKLE(16);

    companion object {
        private val map = PoseLandmark.entries.associateBy(PoseLandmark::position)
        fun fromInt(position: Int): PoseLandmark = map[position] ?: NOSE
    }
}

// Data Class เก็บข้อมูลจุดพิกัด (X, Y) และค่าความมั่นใจ (Score)
data class KeyPoint(
    val bodyPart: PoseLandmark,
    val coordinate: PointF,
    val score: Float
)

// Data Class เก็บข้อมูลคน (ประกอบด้วยหลายจุด KeyPoint)
data class Person(
    val id: Int = -1, // ID สำหรับ Tracking (ถ้ามี)
    val keyPoints: List<KeyPoint>,
    val score: Float // คะแนนรวมความมั่นใจของทั้งตัว
)