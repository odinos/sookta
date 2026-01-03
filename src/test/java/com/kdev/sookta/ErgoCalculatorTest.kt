package com.kdev.sookta

import com.kdev.sookta.model.RebaInputData
import com.kdev.sookta.model.RiskLevel
import com.kdev.sookta.utils.ErgoCalculatorHelper
import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.test.assertEquals

class ErgoCalculatorTest {

    // --- Test Case 1: Light Work (จาก PDF) ---
    // ลักษณะงาน: ก้มลำตัว 30-45 (Score 3), ยืน 2 ขา (Score 1), ยกของเบา (Load 0)
    // REBA: น่าจะได้คะแนนต่ำ-ปานกลาง
    @Test
    fun testRebaCase1() {
        val input = RebaInputData(
            trunkScore = 3,     // 20-60 องศา
            neckScore = 1,      // 0-20 องศา
            legScore = 1,       // ยืนปกติ
            upperArmScore = 2,  // ยกแขนเล็กน้อย
            lowerArmScore = 1,  // ปกติ
            wristScore = 1,     // ปกติ
            loadScore = 0,      // < 5kg
            couplingScore = 0,  // Good
            activityScore = 0   // Static
        )

        val result = ErgoCalculatorHelper.calculateRebaRisk(input)
        println("Case 1 Score: ${result.score} (${result.riskLevel})")

        // Expected: Low to Medium Risk (Score ประมาณ 3-4)
        assertTrue(result.score <= 5)
    }

    // --- Test Case 2: Heavy Twist (จาก PDF) ---
    // ลักษณะงาน: ก้มมาก + บิดตัว (Trunk >60 + Twist), ยกกระสอบกาแฟ (>10kg)
    // REBA: ต้องได้ High Risk หรือ Very High
    @Test
    fun testRebaCase2() {
        val input = RebaInputData(
            trunkScore = 4 + 1, // >60 + Twist (Score 5)
            neckScore = 2,
            legScore = 2,       // ไม่มั่นคง
            upperArmScore = 3,
            lowerArmScore = 2,
            wristScore = 2,
            loadScore = 2,      // > 10kg
            couplingScore = 1,  // Fair
            activityScore = 1   // Repetitive
        )

        val result = ErgoCalculatorHelper.calculateRebaRisk(input)
        println("Case 2 Score: ${result.score} (${result.riskLevel})")

        // Expected: High Risk (Score >= 8) -> คะแนนจริงคือ 11
        assertTrue(result.score >= 8)

        // แก้ไข: ให้ผ่านถ้าเป็น HIGH หรือ VERY_HIGH
        assertTrue(
            "Expected High or Very High risk but got ${result.riskLevel}",
            result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.VERY_HIGH
        )
    }

    // --- Test Case 3: Upper Limb Strain (จาก PDF) ---
    // ลักษณะงาน: หยิบของระดับไหล่, ต้นแขนยกสูง, ข้อมือบิด
    @Test
    fun testRebaCase3() {
        val input = RebaInputData(
            trunkScore = 2,
            neckScore = 2,
            legScore = 1,
            upperArmScore = 4,  // ยกสูง > 90
            lowerArmScore = 1,
            wristScore = 2 + 1, // Twist
            loadScore = 1,      // 5-10kg
            couplingScore = 1,
            activityScore = 1
        )

        val result = ErgoCalculatorHelper.calculateRebaRisk(input)
        println("Case 3 Score: ${result.score} (${result.riskLevel})")

        // Expected: Medium to High
        assertTrue(result.score >= 4)
    }
}