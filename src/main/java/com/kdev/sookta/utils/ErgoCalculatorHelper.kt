package com.kdev.sookta.utils

import com.kdev.sookta.model.BodyPart
import com.kdev.sookta.model.ErgoInputData
import com.kdev.sookta.model.ErgoResult
import com.kdev.sookta.model.RebaInputData
import com.kdev.sookta.model.RiskLevel
import kotlin.math.abs

object ErgoCalculatorHelper {

    private val USER_SCORE_COLORS = listOf(
        0xFF8BC34A, // Level 1: เขียวอ่อน (ปลอดภัยมาก)
        0xFF9CCC65, // Level 2
        0xFFAED581, // Level 3: เขียวอมเหลือง
        0xFFDCE775, // Level 4
        0xFFFFF176, // Level 5: เหลือง (เริ่มเตือน)
        0xFFFFD54F, // Level 6
        0xFFFFB74D, // Level 7: ส้ม
        0xFFFF8A65, // Level 8
        0xFFFF5252  // Level 9: แดงสด (อันตรายสูงสุด)
    )
    private const val REF_MASS_MALE = 25.0
    private const val REF_MASS_FEMALE = 20.0

    // --- ส่วนที่ 1: ISO 11228-1 (Lifting) ---
    fun calculateLiftingRisk(data: ErgoInputData): ErgoResult {
        val isFemale = data.gender.lowercase() == "female"
        val refMass = if (isFemale) REF_MASS_FEMALE else REF_MASS_MALE

        // Hm = 25 / H (ถ้า H < 25 ให้ใช้ 1.0)
        val h = data.horizontalDist.coerceAtLeast(25.0)
        val hm = 25.0 / h

        // Vm = 1 - 0.003 * |V - 75|
        val vm = 1.0 - (0.003 * abs(data.verticalHeight - 75.0))
        val finalVm = vm.coerceIn(0.0, 1.0)

        // Fm logic
        val fm = when {
            data.liftFrequency <= 0.2 -> 1.0
            data.liftFrequency <= 1.0 -> 0.94
            data.liftFrequency <= 4.0 -> 0.84
            data.liftFrequency <= 6.0 -> 0.75
            else -> 0.0
        }

        val rwl = refMass * finalVm * hm * fm
        val li = if (rwl > 0) data.loadWeight / rwl else 99.0

        // Map LI -> User Score 1-9
        // LI < 1.0 -> 1-3 (Low)
        // LI 1.0-3.0 -> 4-6 (Medium)
        // LI > 3.0 -> 7-9 (High)
        val userScore = when {
            li <= 0.5 -> 1
            li <= 0.75 -> 2
            li <= 1.0 -> 3
            li <= 1.5 -> 4
            li <= 2.0 -> 5
            li <= 3.0 -> 6
            li <= 4.0 -> 7
            li <= 5.0 -> 8
            else -> 9
        }
        val risk = when {
            li <= 1.0 -> RiskLevel.LOW
            li <= 3.0 -> RiskLevel.MEDIUM
            else -> RiskLevel.HIGH
        }

        // [UX Update] สร้างคำแนะนำและข้อมูลเสริม
        val economicLoss = calculateEconomicLoss(risk, data.dailyIncome)
        val suggestionStr = if(risk==RiskLevel.LOW) "เหมาะสม" else "ควรปรับปรุง"
        val suggestionList = mutableListOf<String>()
        if(risk >= RiskLevel.MEDIUM) suggestionList.add("ลดน้ำหนักที่ยก")
        val bodyRisks = mapOf(BodyPart.TRUNK to risk)

        return ErgoResult(
            riskLevel = risk,
            techScore = li, // คะแนนดิบ
            userScore = userScore, // คะแนน 1-9
            userScoreColor = getColorForScore(userScore),
            limitValue = rwl,
            suggestion = suggestionStr,
            economicLoss = economicLoss,
            suggestionList = suggestionList,
            bodyPartRisks = bodyRisks
        )
    }

    // --- ส่วนที่ 2: ISO 11228-2 (Pushing/Pulling) ---
    fun calculatePushPullRisk(data: ErgoInputData): ErgoResult {
        val (limitInitial, limitSustain) = if (data.gender.lowercase() == "female") Pair(20.0, 12.0) else Pair(25.0, 15.0)
        val ratioInitial = data.initialForce / limitInitial
        val ratioSustain = data.sustainForce / limitSustain
        val riskScore = maxOf(ratioInitial, ratioSustain) // Ratio คือ Tech Score

        // Map Ratio -> User Score 1-9
        // Ratio < 0.8 -> 1-3
        // Ratio 0.8-1.0 -> 4-6
        // Ratio > 1.0 -> 7-9
        val userScore = when {
            riskScore <= 0.4 -> 1
            riskScore <= 0.6 -> 2
            riskScore < 0.8 -> 3
            riskScore <= 0.9 -> 4
            riskScore <= 1.0 -> 5
            riskScore <= 1.2 -> 6
            riskScore <= 1.5 -> 7
            riskScore <= 2.0 -> 8
            else -> 9
        }

        val risk = when {
            riskScore > 1.0 -> RiskLevel.HIGH
            riskScore >= 0.8 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val suggestionStr = when (risk) {
            RiskLevel.LOW -> "แรงที่ใช้เหมาะสม"
            RiskLevel.MEDIUM -> "แรงใกล้ขีดจำกัด ควรระมัดระวัง"
            RiskLevel.HIGH -> "ใช้แรงเกินมาตรฐาน เสี่ยงบาดเจ็บ"
            else -> ""
        }

        // [UX Update]
        val economicLoss = calculateEconomicLoss(risk, data.dailyIncome)
        val suggestionList = mutableListOf<String>()
        if (risk >= RiskLevel.MEDIUM) {
            suggestionList.add("ตรวจสอบล้อรถเข็นว่าฝืดหรือไม่")
            suggestionList.add("ใช้แรงจากขาในการออกแรง ไม่ใช่หลัง")
        }
        val bodyRisks = mapOf(BodyPart.ARMS to risk, BodyPart.TRUNK to risk)

        return ErgoResult(
            riskLevel = risk,
            techScore = riskScore,
            userScore = userScore,
            userScoreColor = getColorForScore(userScore),
            limitValue = limitInitial,
            suggestion = "",
            economicLoss = economicLoss,
            suggestionList = emptyList(), // ใส่ logic เดิม
            bodyPartRisks = bodyRisks
        )
    }

    // --- ส่วนที่ 3: REBA (ใช้ RebaInputData ที่เพิ่มใหม่) ---
    fun calculateRebaRisk(input: RebaInputData): ErgoResult {
        // 1. Score A
        val scoreTableA = getRebaTableAScore(input.trunkScore, input.neckScore, input.legScore)
        val scoreA = scoreTableA + input.loadScore
        val scoreTableB = getRebaTableBScore(input.upperArmScore, input.lowerArmScore, input.wristScore)
        val scoreB = scoreTableB + input.couplingScore
        val scoreC = getRebaTableCScore(scoreA, scoreB)
        val finalScore = scoreC + input.activityScore // Tech Score (1-15)

        // Map REBA Score -> User Score 1-9
        // 1 -> 1
        // 2-3 -> 2-3
        // 4-7 -> 4-6
        // 8-10 -> 7-8
        // 11+ -> 9
        val userScore = when (finalScore) {
            1 -> 1
            2 -> 2
            3 -> 3
            4 -> 4
            5 -> 5
            6, 7 -> 6
            8 -> 7
            9, 10 -> 8
            else -> 9 // 11+
        }

        val risk = when {
            finalScore <= 1 -> RiskLevel.LOW
            finalScore <= 3 -> RiskLevel.LOW
            finalScore <= 7 -> RiskLevel.MEDIUM
            finalScore <= 10 -> RiskLevel.HIGH
            else -> RiskLevel.VERY_HIGH
        }

        val suggestionStr = when (risk) {
            RiskLevel.LOW -> "ความเสี่ยงต่ำ: ไม่จำเป็นต้องแก้ไข"
            RiskLevel.MEDIUM -> "ความเสี่ยงปานกลาง: ควรตรวจสอบและแก้ไขเร็วๆ นี้"
            RiskLevel.HIGH -> "ความเสี่ยงสูง: จำเป็นต้องแก้ไขโดยเร็ว"
            RiskLevel.VERY_HIGH -> "ความเสี่ยงสูงมาก: ต้องแก้ไขทันที!"
        }

        val economicLoss = calculateEconomicLoss(risk, input.dailyIncome)

        // 5.2 สร้าง Checklist คำแนะนำแบบเจาะจง
        val suggestionList = mutableListOf<String>()
        if (input.loadScore >= 1) suggestionList.add("ลดน้ำหนักสิ่งของ หรือใช้เครื่องทุ่นแรง")
        if (input.trunkScore >= 3) suggestionList.add("หลีกเลี่ยงการก้มหลังมาก หรือใช้เข็มขัดพยุงหลัง")
        if (input.neckScore >= 2) suggestionList.add("ปรับงานให้อยู่ระดับสายตา ลดการก้มคอ")
        if (input.upperArmScore >= 3) suggestionList.add("ลดการยกแขนสูงเหนือไหล่เป็นเวลานาน")
        if (input.wristScore >= 2) suggestionList.add("ปรับด้ามจับเครื่องมือให้ข้อมืออยู่ในแนวตรง")
        if (suggestionList.isEmpty() && risk != RiskLevel.LOW) suggestionList.add("ควรพักเบรกเพื่อยืดเหยียดกล้ามเนื้อ")

        // 5.3 สร้าง Body Map Data
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
            suggestion = "ระดับคะแนน: $userScore/9",
            economicLoss = economicLoss,
            suggestionList = suggestionList,
            bodyPartRisks = bodyRisks
        )
    }

    // --- Helper Functions ---

    private fun getColorForScore(score: Int): Long {
        // ป้องกัน Index Out of Bound (score 1-9 -> index 0-8)
        val index = (score - 1).coerceIn(0, 8)
        return USER_SCORE_COLORS[index]
    }

    // คำนวณเงินที่สูญเสีย (บาท/ปี)
    private fun calculateEconomicLoss(risk: RiskLevel, dailyIncome: Double): Int {
        return when (risk) {
            RiskLevel.VERY_HIGH, RiskLevel.HIGH -> (dailyIncome * 24).toInt()
            RiskLevel.MEDIUM -> (dailyIncome * 4).toInt()
            else -> 0
        }
    }

    // แปลง Score ย่อยเป็นระดับความเสี่ยงรายจุด
    private fun getPartRisk(score: Int, highThreshold: Int): RiskLevel {
        return when {
            score >= highThreshold -> RiskLevel.HIGH
            score >= highThreshold - 1 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    // --- Table Helper Logic (Original from User) ---
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
}