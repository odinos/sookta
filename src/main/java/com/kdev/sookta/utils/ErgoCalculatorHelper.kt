package com.kdev.sookta.utils

import com.kdev.sookta.model.ErgoInputData
import com.kdev.sookta.model.ErgoResult
import com.kdev.sookta.model.RebaInputData // Import มาใช้
import com.kdev.sookta.model.RiskLevel
import kotlin.math.abs

object ErgoCalculatorHelper {

    // ค่าคงที่อ้างอิง (Reference Mass)
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

        val risk = when {
            li <= 1.0 -> RiskLevel.LOW
            li <= 3.0 -> RiskLevel.MEDIUM
            else -> RiskLevel.HIGH
        }

        val suggestion = when (risk) {
            RiskLevel.LOW -> "ความเสี่ยงต่ำ: สภาพการทำงานเหมาะสม"
            RiskLevel.MEDIUM -> "ความเสี่ยงปานกลาง: ควรปรับท่าทางหรือลดน้ำหนัก"
            RiskLevel.HIGH -> "ความเสี่ยงสูง: อันตราย! ต้องแก้ไขทันที"
            else -> ""
        }

        return ErgoResult(risk, li, rwl, suggestion)
    }

    // --- ส่วนที่ 2: ISO 11228-2 (Pushing/Pulling) ---
    fun calculatePushPullRisk(data: ErgoInputData): ErgoResult {
        val (limitInitial, limitSustain) = if (data.gender.lowercase() == "female") {
            Pair(20.0, 12.0)
        } else {
            Pair(25.0, 15.0)
        }

        val ratioInitial = data.initialForce / limitInitial
        val ratioSustain = data.sustainForce / limitSustain
        val riskScore = maxOf(ratioInitial, ratioSustain)

        val risk = when {
            riskScore > 1.0 -> RiskLevel.HIGH
            riskScore >= 0.8 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val suggestion = when (risk) {
            RiskLevel.LOW -> "แรงที่ใช้เหมาะสม"
            RiskLevel.MEDIUM -> "แรงใกล้ขีดจำกัด ควรระมัดระวัง"
            RiskLevel.HIGH -> "ใช้แรงเกินมาตรฐาน เสี่ยงบาดเจ็บ"
            else -> ""
        }

        return ErgoResult(risk, riskScore, limitInitial, suggestion)
    }

    // --- ส่วนที่ 3: REBA (ใช้ RebaInputData ที่เพิ่มใหม่) ---
    fun calculateRebaRisk(input: RebaInputData): ErgoResult {
        // 1. Score A
        val scoreTableA = getRebaTableAScore(input.trunkScore, input.neckScore, input.legScore)
        val scoreA = scoreTableA + input.loadScore

        // 2. Score B
        val scoreTableB = getRebaTableBScore(input.upperArmScore, input.lowerArmScore, input.wristScore)
        val scoreB = scoreTableB + input.couplingScore

        // 3. Score C
        val scoreC = getRebaTableCScore(scoreA, scoreB)

        // 4. Final Score
        val finalScore = scoreC + input.activityScore

        // 5. Risk Level Interpretation
        val risk = when (finalScore) {
            in 0..1 -> RiskLevel.LOW
            in 2..3 -> RiskLevel.LOW
            in 4..7 -> RiskLevel.MEDIUM
            in 8..10 -> RiskLevel.HIGH
            else -> RiskLevel.VERY_HIGH // Score 11+
        }

        val suggestion = when (risk) {
            RiskLevel.LOW -> "ความเสี่ยงต่ำ: ไม่จำเป็นต้องแก้ไข"
            RiskLevel.MEDIUM -> "ความเสี่ยงปานกลาง: ควรตรวจสอบและแก้ไขเร็วๆ นี้"
            RiskLevel.HIGH -> "ความเสี่ยงสูง: จำเป็นต้องแก้ไขโดยเร็ว"
            RiskLevel.VERY_HIGH -> "ความเสี่ยงสูงมาก: ต้องแก้ไขทันที!"
        }

        return ErgoResult(risk, finalScore.toDouble(), 15.0, suggestion)
    }

    // --- Helper Logic (เหมือนเดิม) ---
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