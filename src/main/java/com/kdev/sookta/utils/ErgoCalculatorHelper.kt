package com.kdev.sookta.utils

import com.kdev.sookta.ml.PoseLandmark
import com.kdev.sookta.model.BodyPart
import com.kdev.sookta.ml.Person
import android.graphics.PointF
import com.kdev.sookta.R
import com.kdev.sookta.model.ErgoInputData
import com.kdev.sookta.model.ErgoResult
import com.kdev.sookta.model.RebaInputData
import com.kdev.sookta.model.RiskLevel
import kotlin.math.abs
import kotlin.math.atan2

object ErgoCalculatorHelper {

    private val USER_SCORE_COLORS = listOf(
        0xFF8BC34A, 0xFF9CCC65, 0xFFAED581, 0xFFDCE775, 0xFFFFF176,
        0xFFFFD54F, 0xFFFFB74D, 0xFFFF8A65, 0xFFFF5252
    )
    private const val REF_MASS_MALE = 25.0
    private const val REF_MASS_FEMALE = 20.0

    // --- Constants สำหรับการคำนวณความสูญเสียทางเศรษฐกิจ ---
    private const val THAI_MIN_WAGE = 350.0 // ค่าแรงขั้นต่ำโดยเฉลี่ย

    // ค่ารักษาพยาบาลโดยประมาณ (ค่ายา/นวด/หาหมอ/เดินทาง)
    private const val MED_COST_MEDIUM = 300.0
    private const val MED_COST_HIGH = 1500.0
    private const val MED_COST_VHIGH = 5000.0

    // จำนวนวันที่คาดว่าจะเสียโอกาสในการทำงาน (หยุดงาน)
    private const val LOST_DAYS_MEDIUM = 2.0
    private const val LOST_DAYS_HIGH = 7.0
    private const val LOST_DAYS_VHIGH = 30.0

    // ==========================================
    // ส่วนที่ เพิ่มเติม: AI AUTO-FILL LOGIC
    // ==========================================

    fun calculateRebaInputFromPose(person: Person, currentData: RebaInputData): RebaInputData {
        // Helper to get point
        fun p(bp: PoseLandmark): PointF? = person.keyPoints.find { it.bodyPart == bp && it.score > 0.3f }?.coordinate

        val ear = p(PoseLandmark.RIGHT_EAR) ?: p(PoseLandmark.LEFT_EAR)
        val shoulder = p(PoseLandmark.RIGHT_SHOULDER) ?: p(PoseLandmark.LEFT_SHOULDER)
        val hip = p(PoseLandmark.RIGHT_HIP) ?: p(PoseLandmark.LEFT_HIP)
        val knee = p(PoseLandmark.RIGHT_KNEE) ?: p(PoseLandmark.LEFT_KNEE)
        val elbow = p(PoseLandmark.RIGHT_ELBOW) ?: p(PoseLandmark.LEFT_ELBOW)
        val wrist = p(PoseLandmark.RIGHT_WRIST) ?: p(PoseLandmark.LEFT_WRIST)
        val ankle = p(PoseLandmark.RIGHT_ANKLE) ?: p(PoseLandmark.LEFT_ANKLE)

        var newTrunk = currentData.trunkScore
        var newNeck = currentData.neckScore
        var newUpperArm = currentData.upperArmScore
        var newLowerArm = currentData.lowerArmScore
        var newLeg = currentData.legScore

        // Trunk (หลัง)
        if (shoulder != null && hip != null) {
            val angle = getVerticalAngle(hip, shoulder)
            newTrunk = when {
                angle <= 5 -> 1       // ยืนตรง
                angle <= 20 -> 2      // ก้มเล็กน้อย
                angle <= 60 -> 3      // ก้มปานกลาง
                else -> 4             // ก้มมาก
            }
        }

        // Neck (คอ)
        if (ear != null && shoulder != null) {
            val angle = getVerticalAngle(shoulder, ear)
            newNeck = if (angle <= 20) 1 else 2
        }

        // Upper Arm (ต้นแขน)
        if (shoulder != null && elbow != null) {
            val angle = getVerticalAngle(shoulder, elbow)
            newUpperArm = when {
                angle <= 20 -> 1
                angle <= 45 -> 2
                angle <= 90 -> 3
                else -> 4
            }
        }

        // Lower Arm (แขนท่อนล่าง)
        if (shoulder != null && elbow != null && wrist != null) {
            val angle = getThreePointAngle(shoulder, elbow, wrist)
            // มุมปกติควรอยู่ช่วง 60-100
            newLowerArm = if (angle in 60.0..100.0) 1 else 2
        }

        // Legs (ขา) - ตรวจสอบการงอเข่า
        if (hip != null && knee != null && ankle != null) {
            val kneeAngle = getThreePointAngle(hip, knee, ankle)
            // ถ้ายืนตรง มุมเข่าประมาณ 180, ถ้าย่อเข่า มุมจะลดลง
            newLeg = if (kneeAngle < 150) 2 else 1
        }

        return currentData.copy(
            trunkScore = newTrunk,
            neckScore = newNeck,
            upperArmScore = newUpperArm,
            lowerArmScore = newLowerArm,
            legScore = newLeg
        )
    }


    // ==========================================
    // ส่วนที่ 1: ISO 11228-1 (Lifting) - ปรับปรุงเพิ่ม Distance
    // ==========================================
    fun calculateLiftingRisk(data: ErgoInputData): ErgoResult {
        val isFemale = data.gender.lowercase() == "female"
        val refMass = if (isFemale) REF_MASS_FEMALE else REF_MASS_MALE

        val h = data.horizontalDist.coerceAtLeast(25.0)
        val hm = 25.0 / h
        val vm = (1.0 - (0.003 * abs(data.verticalHeight - 75.0))).coerceIn(0.0, 1.0)

        val fm = when {
            data.liftFrequency <= 0.2 -> 1.0
            data.liftFrequency <= 1.0 -> 0.94
            data.liftFrequency <= 4.0 -> 0.84
            data.liftFrequency <= 6.0 -> 0.75
            else -> 0.0
        }

        // **เพิ่ม: Distance Multiplier (DM)**
        // ลดทอนน้ำหนักที่ยกได้ลง หากต้องเดินแบกไกล
        val dm = when {
            data.transportDistance <= 2.0 -> 1.0   // ระยะใกล้ ไม่กระทบ
            data.transportDistance <= 10.0 -> 0.85 // เริ่มลดทอน
            data.transportDistance <= 20.0 -> 0.75
            else -> 0.6                            // ไกลมาก ลดทอนเยอะ
        }

        // เพิ่ม DM เข้าไปในสูตร RWL
        val rwl = refMass * vm * hm * fm * dm

        val li = if (rwl > 0) data.loadWeight / rwl else 99.0

        val userScore = mapLiToUserScore(li)
        val risk = mapLiToRiskLevel(li)

        val suggestionRes = if(risk == RiskLevel.LOW) R.string.sugg_safe else R.string.sugg_improve

        val suggestionList = mutableListOf<String>()
        if(risk >= RiskLevel.MEDIUM) suggestionList.add("act_reduce_weight")

        // เพิ่มคำแนะนำเรื่องระยะทาง
        if (data.transportDistance > 10.0) suggestionList.add("act_use_cart_distance")

        val bodyRisks = mapOf(com.kdev.sookta.model.BodyPart.TRUNK to risk)

        return ErgoResult(
            riskLevel = risk,
            techScore = li,
            userScore = userScore,
            userScoreColor = getColorForScore(userScore),
            limitValue = rwl,
            suggestion = suggestionRes.toString(),
            economicLoss = calculateHybridEconomicLoss(risk, data.dailyIncome),
            suggestionList = suggestionList,
            bodyPartRisks = bodyRisks
        )
    }


    // ==========================================
    // ส่วนที่ 2: Push/Pull
    // ==========================================
    fun calculatePushPullRisk(data: ErgoInputData): ErgoResult {
        val (limitInitial, limitSustain) = if (data.gender.lowercase() == "female") Pair(20.0, 12.0) else Pair(25.0, 15.0)
        val ratioInitial = data.initialForce / limitInitial
        val ratioSustain = data.sustainForce / limitSustain
        val riskScore = maxOf(ratioInitial, ratioSustain)

        val userScore = mapRatioToUserScore(riskScore)
        val risk = mapRatioToRiskLevel(riskScore)

        val suggestionRes = when (risk) {
            RiskLevel.LOW -> R.string.sugg_force_ok
            RiskLevel.MEDIUM -> R.string.sugg_force_warn
            RiskLevel.HIGH -> R.string.sugg_force_danger
            else -> R.string.sugg_force_ok
        }

        val suggestionList = mutableListOf<String>()
        if (risk >= RiskLevel.MEDIUM) {
            suggestionList.add("act_check_wheels")
            suggestionList.add("act_use_legs")
        }
        val bodyRisks = mapOf(BodyPart.ARMS to risk, BodyPart.TRUNK to risk)

        return ErgoResult(
            riskLevel = risk,
            techScore = riskScore,
            userScore = userScore,
            userScoreColor = getColorForScore(userScore),
            limitValue = limitInitial,
            suggestion = suggestionRes.toString(),
            economicLoss = calculateHybridEconomicLoss(risk, data.dailyIncome),
            suggestionList = suggestionList,
            bodyPartRisks = bodyRisks
        )
    }

    // ==========================================
    // ส่วนที่ 3: REBA
    // ==========================================
    fun calculateRebaRisk(input: RebaInputData): ErgoResult {
        val scoreTableA = getRebaTableAScore(input.trunkScore, input.neckScore, input.legScore)
        val scoreA = scoreTableA + input.loadScore
        val scoreTableB = getRebaTableBScore(input.upperArmScore, input.lowerArmScore, input.wristScore)
        val scoreB = scoreTableB + input.couplingScore
        val scoreC = getRebaTableCScore(scoreA, scoreB)
        val finalScore = scoreC + input.activityScore

        val userScore = mapRebaToUserScore(finalScore)
        val risk = mapRebaToRiskLevel(finalScore)

        val suggestionRes = when (risk) {
            RiskLevel.LOW -> R.string.sugg_reba_low
            RiskLevel.MEDIUM -> R.string.sugg_reba_med
            RiskLevel.HIGH -> R.string.sugg_reba_high
            RiskLevel.VERY_HIGH -> R.string.sugg_reba_vhigh
        }

        val suggestionList = mutableListOf<String>()
        if (input.loadScore >= 1) suggestionList.add("act_reduce_load_tool")
        if (input.trunkScore >= 3) suggestionList.add("act_avoid_bend")
        if (input.neckScore >= 2) suggestionList.add("act_adj_eye_level")
        if (input.upperArmScore >= 3) suggestionList.add("act_reduce_arm_raise")
        if (input.wristScore >= 2) suggestionList.add("act_adj_wrist")
        if (suggestionList.isEmpty() && risk != RiskLevel.LOW) suggestionList.add("act_rest_stretch")

        val bodyRisks = mapOf(
            BodyPart.TRUNK to getPartRisk(input.trunkScore, 4),
            BodyPart.NECK to getPartRisk(input.neckScore, 2),
            BodyPart.LEGS to getPartRisk(input.legScore, 2),
            BodyPart.ARMS to getPartRisk(maxOf(input.upperArmScore, input.lowerArmScore), 3),
            BodyPart.WRISTS to getPartRisk(input.wristScore, 2)
        )

        return ErgoResult(
            riskLevel = risk,
            techScore = finalScore.toDouble(),
            userScore = userScore,
            userScoreColor = getColorForScore(userScore),
            limitValue = 15.0,
            suggestion = suggestionRes.toString(), // เก็บ ResID string
            economicLoss = calculateHybridEconomicLoss(risk, input.dailyIncome),
            suggestionList = suggestionList,
            bodyPartRisks = bodyRisks
        )
    }

    // --- Helper Mappers ---
    private fun mapLiToUserScore(li: Double): Int = when {
        li <= 0.5 -> 1; li <= 0.75 -> 2; li <= 1.0 -> 3; li <= 1.5 -> 4
        li <= 2.0 -> 5; li <= 3.0 -> 6; li <= 4.0 -> 7; li <= 5.0 -> 8; else -> 9
    }
    private fun mapLiToRiskLevel(li: Double): RiskLevel = if (li <= 1.0) RiskLevel.LOW else if (li <= 3.0) RiskLevel.MEDIUM else RiskLevel.HIGH

    private fun mapRatioToUserScore(r: Double): Int = when {
        r <= 0.4 -> 1; r <= 0.6 -> 2; r < 0.8 -> 3; r <= 0.9 -> 4
        r <= 1.0 -> 5; r <= 1.2 -> 6; r <= 1.5 -> 7; r <= 2.0 -> 8; else -> 9
    }
    private fun mapRatioToRiskLevel(r: Double): RiskLevel = if (r > 1.0) RiskLevel.HIGH else if (r >= 0.8) RiskLevel.MEDIUM else RiskLevel.LOW

    private fun mapRebaToUserScore(s: Int): Int = when (s) {
        1 -> 1; 2 -> 2; 3 -> 3; 4 -> 4; 5 -> 5; 6, 7 -> 6; 8 -> 7; 9, 10 -> 8; else -> 9
    }
    private fun mapRebaToRiskLevel(s: Int): RiskLevel = if(s<=3) RiskLevel.LOW else if(s<=7) RiskLevel.MEDIUM else if(s<=10) RiskLevel.HIGH else RiskLevel.VERY_HIGH

    private fun getColorForScore(score: Int): Long {
        return USER_SCORE_COLORS[(score - 1).coerceIn(0, 8)]
    }

    /**
     * [New Feature] คำนวณความสูญเสียแบบผสมผสาน (Hybrid Loss Model)
     * = (ค่าเสียโอกาสจากการหยุดงาน) + (ค่ารักษาพยาบาลโดยประมาณ)
     */
    private fun calculateHybridEconomicLoss(risk: RiskLevel, dailyIncome: Double): Int {
        // ถ้าผู้ใช้ไม่กรอกรายได้ หรือกรอกมาเป็น 0 ให้ใช้ค่าแรงขั้นต่ำ (350 บาท) เป็นฐาน
        val incomeBase = if (dailyIncome > 0) dailyIncome else THAI_MIN_WAGE

        return when (risk) {
            RiskLevel.VERY_HIGH -> {
                val lostOpportunity = incomeBase * LOST_DAYS_VHIGH // หยุดงาน 30 วัน
                val medicalCost = MED_COST_VHIGH // ค่ารักษา 5000
                (lostOpportunity + medicalCost).toInt()
            }
            RiskLevel.HIGH -> {
                val lostOpportunity = incomeBase * LOST_DAYS_HIGH // หยุดงาน 7 วัน
                val medicalCost = MED_COST_HIGH // ค่ารักษา 1500
                (lostOpportunity + medicalCost).toInt()
            }
            RiskLevel.MEDIUM -> {
                val lostOpportunity = incomeBase * LOST_DAYS_MEDIUM // หยุดงาน 2 วัน
                val medicalCost = MED_COST_MEDIUM // ค่ารักษา 300
                (lostOpportunity + medicalCost).toInt()
            }
            else -> 0 // ความเสี่ยงต่ำ ไม่มีความสูญเสีย
        }
    }

    private fun getPartRisk(score: Int, highThreshold: Int): RiskLevel {
        return when {
            score >= highThreshold -> RiskLevel.HIGH
            score >= highThreshold - 1 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    // Reba Tables logic remains same...
    private fun getRebaTableAScore(trunk: Int, neck: Int, leg: Int): Int {
        var s = trunk + (if (neck >= 2) 1 else 0) + (if (leg >= 2) 1 else 0)
        if (trunk >= 4 && neck >= 3) s += 1
        return s.coerceAtMost(9)
    }

    private fun getRebaTableBScore(upper: Int, lower: Int, wrist: Int): Int {
        var s = upper
        if (lower >= 2) s += 1
        if (wrist >= 2) s += 1
        if (upper >= 4 && wrist >= 3) s += 1
        return s.coerceAtMost(9)
    }

    private fun getRebaTableCScore(scoreA: Int, scoreB: Int): Int {
        val maxScore = maxOf(scoreA, scoreB)
        val minScore = minOf(scoreA, scoreB)
        var c = maxScore
        if (minScore >= 6) c += 1
        return c.coerceAtMost(12)
    }

    // Geometry Helpers for AI
    private fun getVerticalAngle(p1: PointF, p2: PointF): Double {
        val dx = p2.x - p1.x
        val dy = p1.y - p2.y
        return Math.toDegrees(atan2(abs(dx), abs(dy)).toDouble())
    }

    private fun getThreePointAngle(p1: PointF, p2: PointF, p3: PointF): Double {
        val a1 = atan2((p1.y - p2.y).toDouble(), (p1.x - p2.x).toDouble())
        val a2 = atan2((p3.y - p2.y).toDouble(), (p3.x - p2.x).toDouble())
        var angle = Math.toDegrees(a1 - a2)
        if (angle < 0) angle += 360.0
        if (angle > 180) angle = 360.0 - angle
        return angle
    }
}