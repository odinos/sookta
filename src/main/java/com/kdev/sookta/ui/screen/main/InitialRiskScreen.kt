package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.R
import com.kdev.sookta.model.ErgoInputData
import com.kdev.sookta.model.ErgoResult
import com.kdev.sookta.model.JobType
import com.kdev.sookta.model.RiskLevel
import com.kdev.sookta.utils.ErgoCalculatorHelper
import kotlin.math.roundToInt

@Composable
fun InitialRiskScreen(navController: NavController, activityNameArg: String, initialScoreArg: String?) {
    // 1. รับข้อมูล Object จาก SavedStateHandle (ข้อมูลจริงที่คำนวณมาแล้ว)
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val initialResult = savedStateHandle?.get<ErgoResult>("riskResult")
    val initialInput = savedStateHandle?.get<ErgoInputData>("inputData")

    // Fallback กรณีไม่มีข้อมูล (ป้องกัน Crash)
    if (initialResult == null || initialInput == null) {
        Text("Error: Missing Data", modifier = Modifier.padding(16.dp))
        return
    }

    val displayActivityName = activityNameArg.toIntOrNull()?.let { stringResource(it) } ?: activityNameArg

    // 2. State สำหรับการจำลองการแก้ไข (Solution Simulation)
    // เริ่มต้นให้ค่าเท่ากับ Input เดิม
    var solutionInput by remember { mutableStateOf(initialInput) }

    // คำนวณความเสี่ยงใหม่ทันทีที่มีการเปลี่ยนค่า (Projected Risk)
    val finalResult = remember(solutionInput) {
        ErgoCalculatorHelper.calculateRisk(solutionInput)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.initial_risk_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.label_activity_format, displayActivityName), color = Color.Gray, fontSize = 14.sp)

        Spacer(Modifier.height(20.dp))

        // --- ส่วนแสดงผลความเสี่ยงปัจจุบัน (Before) ---
        Text("ผลการประเมินปัจจุบัน (Before)", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        RiskScoreCircle(
            result = initialResult,
            isLifting = initialInput.jobType == JobType.LIFTING
        )

        // คำแนะนำเบื้องต้นจาก Helper
        Text(
            text = "สาเหตุ: ${initialResult.suggestion}",
            color = Color.Red.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(Modifier.height(20.dp))

        // --- ส่วนปรับปรุง Solution (Simulation) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), // สีฟ้าอ่อน
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("จำลองการปรับปรุง (Solution)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1565C0))
                Text("ลองปรับลดค่าต่างๆ เพื่อดูผลลัพธ์ใหม่", fontSize = 13.sp, color = Color.Gray)

                Spacer(Modifier.height(16.dp))

                if (initialInput.jobType == JobType.LIFTING) {
                    // --- Sliders สำหรับงานยก ---

                    // 1. ปรับน้ำหนัก
                    SolutionSlider(
                        label = "ลดน้ำหนักวัตถุ (kg)",
                        value = solutionInput.loadWeight.toFloat(),
                        range = 0f..initialInput.loadWeight.toFloat(),
                        onValueChange = { solutionInput = solutionInput.copy(loadWeight = it.toDouble()) }
                    )

                    // 2. ปรับระยะห่าง (H)
                    SolutionSlider(
                        label = "ระยะห่างตัว (H) cm",
                        value = solutionInput.horizontalDist.toFloat(),
                        range = 25f..maxOf(25f, initialInput.horizontalDist.toFloat()), // ปรับลดลงได้ไม่ต่ำกว่า 25
                        onValueChange = { solutionInput = solutionInput.copy(horizontalDist = it.toDouble()) }
                    )

                    // 3. ปรับความถี่
                    SolutionSlider(
                        label = "ความถี่ (ครั้ง/นาที)",
                        value = solutionInput.liftFrequency.toFloat(),
                        range = 0.1f..initialInput.liftFrequency.toFloat(),
                        onValueChange = { solutionInput = solutionInput.copy(liftFrequency = it.toDouble()) }
                    )

                } else {
                    // --- Sliders สำหรับงานเข็น (Push/Pull) ---

                    // 1. ลดแรงเริ่มต้น
                    SolutionSlider(
                        label = "ลดแรงกระชากเริ่มเข็น (Initial N)",
                        value = solutionInput.initialForce.toFloat(),
                        range = 0f..initialInput.initialForce.toFloat(),
                        onValueChange = { solutionInput = solutionInput.copy(initialForce = it.toDouble()) }
                    )

                    // 2. ลดแรงเข็นต่อเนื่อง
                    SolutionSlider(
                        label = "ลดแรงเข็นต่อเนื่อง (Sustain N)",
                        value = solutionInput.sustainForce.toFloat(),
                        range = 0f..initialInput.sustainForce.toFloat(),
                        onValueChange = { solutionInput = solutionInput.copy(sustainForce = it.toDouble()) }
                    )
                }

                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                // แสดงผลลัพธ์ Real-time หลังปรับ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ความเสี่ยงหลังปรับ", fontWeight = FontWeight.Bold)
                        Text(finalResult.riskLevel.label, color = Color(finalResult.riskLevel.colorHex), fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (initialInput.jobType == JobType.LIFTING) "LI: ${String.format("%.2f", finalResult.score)}" else "Ratio: ${String.format("%.2f", finalResult.score)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = {
                // ส่งผลลัพธ์ทั้งคู่ไปหน้า Final
                navController.currentBackStackEntry?.savedStateHandle?.set("initialResult", initialResult)
                navController.currentBackStackEntry?.savedStateHandle?.set("finalResult", finalResult)

                // Navigate
                navController.navigate("final_result_screen") // ต้องเปลี่ยน route ให้ตรงกับที่คุณตั้งไว้
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81))
        ) {
            Text("บันทึกและสรุปผลการประเมิน")
        }
    }
}

// UI Slider ย่อยสำหรับปรับค่า
@Composable
fun SolutionSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp)
            Text(String.format("%.1f", value), fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF1565C0),
                activeTrackColor = Color(0xFF1565C0),
                inactiveTrackColor = Color(0xFFBBDEFB)
            )
        )
    }
}

// UI วงกลมแสดงคะแนน (ปรับปรุงให้รองรับ RiskLevel)
@Composable
fun RiskScoreCircle(result: ErgoResult, isLifting: Boolean) {
    val color = Color(result.riskLevel.colorHex)
    val scoreLabel = if (isLifting) "LI" else "Ratio"

    Box(
        modifier = Modifier
            .size(160.dp)
            .background(Color.White, shape = RoundedCornerShape(100))
            .border(10.dp, color, RoundedCornerShape(100)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = String.format("%.2f", result.score), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = "$scoreLabel Score", fontSize = 14.sp, color = Color.Gray)
            Text(text = result.riskLevel.label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = color, modifier = Modifier.padding(top = 4.dp))
        }
    }
}