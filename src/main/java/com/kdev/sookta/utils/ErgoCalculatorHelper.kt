package com.kdev.sookta.utils

import com.kdev.sookta.R
import com.kdev.sookta.model.BodyPart
import com.kdev.sookta.model.ErgoInputData
import com.kdev.sookta.model.ErgoResult
import com.kdev.sookta.model.RebaInputData
import com.kdev.sookta.model.RiskLevel
import kotlin.math.abs

object ErgoCalculatorHelper {

    private val USER_SCORE_COLORS = listOf(
        0xFF8BC34A, 0xFF9CCC65, 0xFFAED581, 0xFFDCE775, 0xFFFFF176,
        0xFFFFD54F, 0xFFFFB74D, 0xFFFF8A65, 0xFFFF5252
    )
    private const val REF_MASS_MALE = 25.0
    private const val REF_MASS_FEMALE = 20.0

    // --- ส่วนที่ 1: ISO 11228-1 (Lifting) ---
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

        val rwl = refMass * vm * hm * fm
        val li = if (rwl > 0) data.loadWeight / rwl else 99.0

        val userScore = mapLiToUserScore(li)
        val risk = mapLiToRiskLevel(li)

        // Return Resource ID
        val suggestionRes = if(risk == RiskLevel.LOW) R.string.sugg_safe else R.string.sugg_improve

        // Return List of Resource IDs (Strings representing keys) or plain strings if dynamic?
        // เพื่อความง่ายและรองรับ Serializable: เราจะเก็บเป็น List<String> ที่เป็น KEY ของ Resource
        // แล้วให้ UI ไป map เอาเอง หรือเก็บเป็น StringRes ID แต่ ErgoResult ต้องแก้ Model
        // **วิธีที่ง่ายที่สุด:** ส่ง Key String (เช่น "act_reduce_weight") ไป แล้วให้ UI ใช้ getIdentifier หรือ Map
        // แต่เพื่อความ Clean จะขอแก้ให้ ErgoResult เก็บ List<Int> (ResId) แทน String ไปเลยจะดีที่สุด
        // **แต่ถ้าแก้ Model ไม่ได้:** ให้ส่ง String ที่เป็น "PREFIX_RES_ID:xxx"

        // *สมมติว่า ErgoResult.suggestionList เป็น List<String> ตามเดิม*
        // ผมจะส่ง String ที่เป็น Resource Name ไปแทน แล้วให้ UI Helper แปลง
        val suggestionList = mutableListOf<String>()
        if(risk >= RiskLevel.MEDIUM) suggestionList.add("act_reduce_weight") // Key

        val bodyRisks = mapOf(BodyPart.TRUNK to risk)

        return ErgoResult(
            riskLevel = risk,
            techScore = li,
            userScore = userScore,
            userScoreColor = getColorForScore(userScore),
            limitValue = rwl,
            suggestion = suggestionRes.toString(), // เก็บ ResID เป็น String ชั่วคราว
            economicLoss = calculateEconomicLoss(risk, data.dailyIncome),
            suggestionList = suggestionList,
            bodyPartRisks = bodyRisks
        )
    }

    // --- ส่วนที่ 2: Push/Pull ---
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
            economicLoss = calculateEconomicLoss(risk, data.dailyIncome),
            suggestionList = suggestionList,
            bodyPartRisks = bodyRisks
        )
    }

    // --- ส่วนที่ 3: REBA ---
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
            economicLoss = calculateEconomicLoss(risk, input.dailyIncome),
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

    private fun calculateEconomicLoss(risk: RiskLevel, dailyIncome: Double): Int {
        return when (risk) {
            RiskLevel.VERY_HIGH, RiskLevel.HIGH -> (dailyIncome * 24).toInt()
            RiskLevel.MEDIUM -> (dailyIncome * 4).toInt()
            else -> 0
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
}