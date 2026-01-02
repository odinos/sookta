package com.kdev.sookta.utils

import com.kdev.sookta.model.ErgoInputData
import com.kdev.sookta.model.ErgoResult
import com.kdev.sookta.model.JobType
import com.kdev.sookta.model.RiskLevel
import kotlin.math.abs

/**
 * Helper Class สำหรับคำนวณความเสี่ยงตามหลักการยศาสตร์
 * อ้างอิง: ISO 11228-1 (Lifting) และ ISO 11228-2 (Pushing/Pulling)
 */
object ErgoCalculatorHelper {

    // ค่าคงที่อ้างอิง (Reference Mass)
    private const val REF_MASS_MALE = 25.0
    private const val REF_MASS_FEMALE = 20.0

    /**
     * ฟังก์ชันหลักสำหรับเรียกใช้งานจากภายนอก
     * จะตรวจสอบประเภทงานและเรียกสูตรคำนวณที่เหมาะสม
     */
    fun calculateRisk(data: ErgoInputData): ErgoResult {
        return when (data.jobType) {
            JobType.LIFTING -> calculateLiftingISO11228_1(data)
            JobType.PUSH_PULL -> calculatePushPullISO11228_2(data)
        }
    }

    /**
     * คำนวณงานยก (Lifting) ตามมาตรฐาน ISO 11228-1
     * สูตร: RWL = ReferenceMass * Vm * Hm * Fm (แบบย่อตาม PDF)
     * Risk = Lifting Index (LI) = Load / RWL
     */
    private fun calculateLiftingISO11228_1(data: ErgoInputData): ErgoResult {
        // 1. กำหนดน้ำหนักอ้างอิงตามเพศ (Reference Mass)
        val refMass = if (data.gender.lowercase() == "female") REF_MASS_FEMALE else REF_MASS_MALE

        // 2. คำนวณตัวคูณระยะห่าง (Horizontal Multiplier - Hm)
        // สูตรมาตรฐาน: Hm = 25 / H (ถ้า H < 25 ให้ใช้ 1.0)
        val h = data.horizontalDist.coerceAtLeast(25.0)
        val hm = 25.0 / h

        // 3. คำนวณตัวคูณความสูง (Vertical Multiplier - Vm)
        // สูตรมาตรฐาน: Vm = 1 - 0.003 * |V - 75|
        // V คือความสูงจุดยก (cm), 75 คือระดับความสูงมาตรฐาน (Knuckle height)
        val vm = 1.0 - (0.003 * abs(data.verticalHeight - 75.0))
        val finalVm = vm.coerceIn(0.0, 1.0) // ค่าต้องอยู่ระหว่าง 0 ถึง 1

        // 4. คำนวณตัวคูณความถี่ (Frequency Multiplier - Fm)
        // (ในโค้ดจริงอาจต้องใช้ตาราง Lookup Table ที่ซับซ้อนกว่านี้ตาม Duration)
        // นี่คือ Logic อย่างง่ายตามแนวทาง PDF: ยิ่งถี่มาก ตัวคูณยิ่งน้อย
        val fm = when {
            data.liftFrequency <= 0.2 -> 1.0   // 1 ครั้ง/5 นาที
            data.liftFrequency <= 1.0 -> 0.94
            data.liftFrequency <= 4.0 -> 0.84
            data.liftFrequency <= 6.0 -> 0.75
            else -> 0.0 // ถี่เกินไป ไม่แนะนำให้ทำ
        }

        // 5. คำนวณขีดจำกัดน้ำหนักที่แนะนำ (Recommended Weight Limit - RWL)
        val rwl = refMass * finalVm * hm * fm

        // 6. คำนวณดัชนีการยก (Lifting Index - LI)
        // LI = น้ำหนักจริง / RWL
        val li = if (rwl > 0) data.loadWeight / rwl else 99.0 // ป้องกันหารด้วย 0

        // 7. ประเมินความเสี่ยงตามค่า LI (จาก PDF: <1 ต่ำ, 1-3 ปานกลาง, >3 สูง)
        val risk = when {
            li <= 1.0 -> RiskLevel.LOW
            li <= 3.0 -> RiskLevel.MEDIUM
            else -> RiskLevel.HIGH
        }

        val suggestion = when (risk) {
            RiskLevel.LOW -> "สภาพการทำงานเหมาะสม"
            RiskLevel.MEDIUM -> "ควรปรับปรุงท่าทางหรือลดน้ำหนักวัตถุ"
            RiskLevel.HIGH -> "อันตราย! ต้องหยุดงานและปรับปรุงทันที"
        }

        return ErgoResult(risk, li, rwl, suggestion)
    }

    /**
     * คำนวณงานผลัก/ดึง (Pushing/Pulling) ตามมาตรฐาน ISO 11228-2
     * อ้างอิง Limit จาก PDF:
     * ชาย: Initial 25N, Sustained 15N
     * หญิง: Initial 20N, Sustained 12N
     */
    private fun calculatePushPullISO11228_2(data: ErgoInputData): ErgoResult {
        // 1. กำหนดค่า Force Limit ตามเพศ
        val (limitInitial, limitSustain) = if (data.gender.lowercase() == "female") {
            Pair(20.0, 12.0)
        } else {
            Pair(25.0, 15.0)
        }

        // 2. ตรวจสอบความเสี่ยง (เช็คทั้งแรงเริ่มและแรงขณะเข็น)
        var riskScore = 0.0 // ใช้เป็น Ratio เทียบกับ Limit
        var isHighRisk = false
        var isMediumRisk = false

        // คำนวณ Ratio (แรงที่ใช้ / แรงที่กำหนด)
        val ratioInitial = data.initialForce / limitInitial
        val ratioSustain = data.sustainForce / limitSustain

        // เลือกค่าที่แย่ที่สุดมาเป็น Score
        riskScore = maxOf(ratioInitial, ratioSustain)

        // 3. ประเมินความเสี่ยงตาม PDF Logic
        // > Limit (Ratio > 1) -> สูง
        // ~ Limit (Ratio 0.8 - 1.0) -> ปานกลาง (สมมติช่วง Threshold)
        // < Limit (Ratio < 0.8) -> ต่ำ
        val risk = when {
            riskScore > 1.0 -> RiskLevel.HIGH
            riskScore >= 0.8 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val suggestion = when (risk) {
            RiskLevel.LOW -> "แรงที่ใช้เหมาะสม"
            RiskLevel.MEDIUM -> "เริ่มใช้แรงเยอะ ควรตรวจสอบล้อรถเข็นหรือพื้นผิว"
            RiskLevel.HIGH -> "ใช้แรงมากเกินไป เสี่ยงต่อการบาดเจ็บ"
        }

        // Return limitValue เป็นค่าเฉลี่ยของ Limit ที่ใช้แสดงผล
        return ErgoResult(risk, riskScore, limitInitial, suggestion)
    }
}