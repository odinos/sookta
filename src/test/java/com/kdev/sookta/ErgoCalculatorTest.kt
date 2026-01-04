package com.kdev.sookta

import com.kdev.sookta.model.RebaInputData
import com.kdev.sookta.model.RiskLevel
import com.kdev.sookta.utils.ErgoCalculatorHelper
import org.junit.Assert.* // ใช้ JUnit Assert
import org.junit.Test

class ErgoCalculatorTest {

    // --- Test Case 1: Light Work ---
    // ลักษณะงาน: ก้มลำตัว 30-45 (Score 3), ยืน 2 ขา (Score 1), ยกของเบา (Load 0)
    // REBA: น่าจะได้คะแนนต่ำ-ปานกลาง (User Score 1-9 ต่ำๆ)
    @Test
    fun testRebaCase1() {
        val input = RebaInputData(
            dailyIncome = 300.0, // เพิ่ม dailyIncome (ไม่กระทบผลคะแนน แต่ต้องมีใน Constructor)
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
        println("Case 1 - Tech Score: ${result.techScore}, User Score: ${result.userScore} (${result.riskLevel})")

        // Expected: Low Risk (Tech Score <= 3, User Score <= 3)
        // ตาม Logic ใน Helper: Final Score 3 -> User Score 3
        assertTrue("User Score ควรต่ำ (<= 3)", result.userScore <= 3)
        assertEquals("Risk Level ควรเป็น LOW", RiskLevel.LOW, result.riskLevel)
        assertEquals("Economic Loss ควรเป็น 0", 0, result.economicLoss)
    }

    // --- Test Case 2: Heavy Twist ---
    // ลักษณะงาน: ก้มมาก + บิดตัว (Trunk >60 + Twist), ยกกระสอบกาแฟ (>10kg)
    // REBA: ต้องได้ High Risk หรือ Very High
    @Test
    fun testRebaCase2() {
        val input = RebaInputData(
            dailyIncome = 500.0, // สมมติรายได้ 500
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
        println("Case 2 - Tech Score: ${result.techScore}, User Score: ${result.userScore} (${result.riskLevel})")

        // Expected: High Risk (Tech Score >= 8) -> คะแนนจริงคือ 11 (Very High)
        // User Score: 11+ -> 9
        assertTrue("Tech Score ควรสูง (>= 8)", result.techScore >= 8)
        assertEquals("User Score ควรเป็น 9 (สูงสุด)", 9, result.userScore)

        // ตรวจสอบความเสี่ยง
        assertTrue(
            "Expected High or Very High risk but got ${result.riskLevel}",
            result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.VERY_HIGH
        )

        // ตรวจสอบ Economic Loss (High/Very High = dailyIncome * 24)
        // 500 * 24 = 12000
        assertEquals("Economic Loss ไม่ถูกต้อง", 12000, result.economicLoss)
    }

    // --- Test Case 3: Upper Limb Strain ---
    // ลักษณะงาน: หยิบของระดับไหล่, ต้นแขนยกสูง, ข้อมือบิด
    @Test
    fun testRebaCase3() {
        val input = RebaInputData(
            dailyIncome = 300.0,
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
        println("Case 3 - Tech Score: ${result.techScore}, User Score: ${result.userScore} (${result.riskLevel})")

        // Expected: Medium to High
        // Tech Score น่าจะประมาณ 4-7 (Medium)
        assertTrue("Tech Score ควร >= 4", result.techScore >= 4)
        assertTrue("User Score ควร >= 4", result.userScore >= 4)

        // ถ้าเป็น Medium -> Economic Loss = 300 * 4 = 1200
        if (result.riskLevel == RiskLevel.MEDIUM) {
            assertEquals("Economic Loss (Medium) ไม่ถูกต้อง", 1200, result.economicLoss)
        }
    }
}