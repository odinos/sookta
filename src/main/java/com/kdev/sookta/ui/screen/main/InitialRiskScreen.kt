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
import com.kdev.sookta.model.RebaInputData
import com.kdev.sookta.utils.ErgoCalculatorHelper

@Composable
fun InitialRiskScreen(navController: NavController, activityNameArg: String, scoreArg: Int) {
    // 1. รับข้อมูลจาก SavedStateHandle (ข้อมูล Object ที่ส่งมา)
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val initialResult = savedStateHandle?.get<ErgoResult>("riskResult")
    // รับได้ทั้ง ErgoInputData (Lifting/Push) และ RebaInputData (REBA)
    val inputDataRaw = savedStateHandle?.get<Any>("inputData")

    // Fallback กรณีข้อมูลไม่มา
    if (initialResult == null || inputDataRaw == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ไม่พบข้อมูลการประเมิน กรุณาลองใหม่อีกครั้ง", color = Color.Red)
        }
        return
    }

    val displayActivityName = activityNameArg.toIntOrNull()?.let { stringResource(it) } ?: activityNameArg

    // 2. State สำหรับจำลองการแก้ไข (Simulation)
    // ตรวจสอบชนิดข้อมูลและ Cast ให้ถูกต้อง
    var solutionInputIso by remember { mutableStateOf(if (inputDataRaw is ErgoInputData) inputDataRaw else null) }
    var solutionInputReba by remember { mutableStateOf(if (inputDataRaw is RebaInputData) inputDataRaw else null) }

    val jobType = solutionInputIso?.jobType ?: JobType.REBA

    // คำนวณความเสี่ยงใหม่ Real-time
    val finalResult = remember(solutionInputIso, solutionInputReba) {
        if (solutionInputIso != null) {
            // ISO 11228 Case: ต้องเช็คว่าเป็นงานยก หรือ งานเข็น แล้วเรียกฟังก์ชันให้ถูก
            if (solutionInputIso!!.jobType == JobType.LIFTING) {
                ErgoCalculatorHelper.calculateLiftingRisk(solutionInputIso!!)
            } else {
                ErgoCalculatorHelper.calculatePushPullRisk(solutionInputIso!!)
            }
        } else if (solutionInputReba != null) {
            // REBA Case
            ErgoCalculatorHelper.calculateRebaRisk(solutionInputReba!!)
        } else {
            initialResult // Fallback
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.initial_risk_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Text("กิจกรรม: $displayActivityName", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(24.dp))

        // --- ส่วนแสดงผลความเสี่ยงปัจจุบัน (Before) ---
        Text("ผลการประเมินปัจจุบัน (Before)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        RiskScoreCircle(result = initialResult, jobType = jobType)

        // คำแนะนำเบื้องต้น
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), // สีแดงอ่อนแจ้งเตือน
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
        ) {
            Text(
                text = "ข้อเสนอแนะ: ${initialResult.suggestion}",
                color = Color(0xFFC62828),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            )
        }

        Spacer(Modifier.height(24.dp))

        // --- ส่วนจำลองการปรับปรุง (Solution Simulation) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), // สีฟ้าอ่อน
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("จำลองการปรับปรุง (Solution)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1565C0))
                Text("ลองปรับลดค่าต่างๆ เพื่อดูผลลัพธ์ใหม่", fontSize = 13.sp, color = Color.Gray)

                Spacer(Modifier.height(16.dp))

                if (jobType == JobType.LIFTING && solutionInputIso != null) {
                    // --- Sliders: Lifting ---
                    SolutionSlider("ลดน้ำหนักวัตถุ (kg)", solutionInputIso!!.loadWeight.toFloat(), 0f..solutionInputIso!!.loadWeight.toFloat()) {
                        solutionInputIso = solutionInputIso!!.copy(loadWeight = it.toDouble())
                    }
                    SolutionSlider("เพิ่มความสูงจุดยก (V) cm", solutionInputIso!!.verticalHeight.toFloat(), solutionInputIso!!.verticalHeight.toFloat()..150f) { // ยิ่งสูง(ใกล้เอว)ยิ่งดีในบาง case หรือปรับให้เข้าใกล้ 75
                        // Logic: ปรับให้เข้าใกล้ 75 (Standard) หรือ user ปรับเอง
                        solutionInputIso = solutionInputIso!!.copy(verticalHeight = it.toDouble())
                    }
                    SolutionSlider("ลดความถี่ (ครั้ง/นาที)", solutionInputIso!!.liftFrequency.toFloat(), 0.1f..solutionInputIso!!.liftFrequency.toFloat()) {
                        solutionInputIso = solutionInputIso!!.copy(liftFrequency = it.toDouble())
                    }

                } else if (jobType == JobType.PUSH_PULL && solutionInputIso != null) {
                    // --- Sliders: Push/Pull ---
                    SolutionSlider("ลดแรงเริ่มต้น (Initial N)", solutionInputIso!!.initialForce.toFloat(), 0f..solutionInputIso!!.initialForce.toFloat()) {
                        solutionInputIso = solutionInputIso!!.copy(initialForce = it.toDouble())
                    }
                    SolutionSlider("ลดแรงเข็นต่อเนื่อง (Sustain N)", solutionInputIso!!.sustainForce.toFloat(), 0f..solutionInputIso!!.sustainForce.toFloat()) {
                        solutionInputIso = solutionInputIso!!.copy(sustainForce = it.toDouble())
                    }

                } else if (jobType == JobType.REBA && solutionInputReba != null) {
                    // --- Sliders: REBA (ปรับ Score) ---
                    // REBA Score เป็น Int แต่ Slider เป็น Float
                    Text("ปรับปรุงท่าทาง (ลดคะแนนความเสี่ยง)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    Spacer(Modifier.height(8.dp))

                    SolutionSliderInt("คะแนนลำตัว (Trunk)", solutionInputReba!!.trunkScore, 1..solutionInputReba!!.trunkScore) {
                        solutionInputReba = solutionInputReba!!.copy(trunkScore = it)
                    }
                    SolutionSliderInt("คะแนนแขน (Upper Arm)", solutionInputReba!!.upperArmScore, 1..solutionInputReba!!.upperArmScore) {
                        solutionInputReba = solutionInputReba!!.copy(upperArmScore = it)
                    }
                    SolutionSliderInt("คะแนนน้ำหนัก (Load)", solutionInputReba!!.loadScore, 0..solutionInputReba!!.loadScore) {
                        solutionInputReba = solutionInputReba!!.copy(loadScore = it)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFBBDEFB))
                Spacer(Modifier.height(8.dp))

                // Result Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ความเสี่ยงหลังปรับ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(finalResult.riskLevel.label, color = Color(finalResult.riskLevel.colorHex), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(
                        text = "Score: ${String.format("%.2f", finalResult.score)}",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0)
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = {
                // ส่งผลลัพธ์ไปหน้า Final
                // ใช้ key ที่ตกลงกันไว้ (oldScore, newScore ใน route หรือส่ง Object)
                // ในที่นี้ส่ง Object ผ่าน savedStateHandle เพื่อความครบถ้วน
                navController.currentBackStackEntry?.savedStateHandle?.set("initialResult", initialResult)
                navController.currentBackStackEntry?.savedStateHandle?.set("finalResult", finalResult)

                // Route format: final_result/{oldScore}/{newScore} ตาม MainActivity
                val oldScoreStr = initialResult.score.toInt().toString()
                val newScoreStr = finalResult.score.toInt().toString()
                navController.navigate("final_result/$oldScoreStr/$newScoreStr")
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81))
        ) {
            Text("บันทึกและสรุปผลการประเมิน", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(50.dp))
    }
}

// --- Helper UI Components ---

@Composable
fun SolutionSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp)
            Text(String.format("%.1f", value), fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF1565C0), activeTrackColor = Color(0xFF1565C0), inactiveTrackColor = Color(0xFFBBDEFB))
        )
    }
}

@Composable
fun SolutionSliderInt(label: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp)
            Text("$value", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 13.sp)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = if (range.last - range.first > 0) (range.last - range.first) - 1 else 0,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF1565C0), activeTrackColor = Color(0xFF1565C0), inactiveTrackColor = Color(0xFFBBDEFB))
        )
    }
}

@Composable
fun RiskScoreCircle(result: ErgoResult, jobType: JobType) {
    val color = Color(result.riskLevel.colorHex)
    val scoreLabel = when(jobType) {
        JobType.LIFTING -> "Lifting Index"
        JobType.PUSH_PULL -> "Risk Ratio"
        JobType.REBA -> "REBA Score"
    }

    Box(
        modifier = Modifier
            .size(160.dp)
            .background(Color.White, shape = RoundedCornerShape(100))
            .border(8.dp, color, RoundedCornerShape(100)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = String.format("%.1f", result.score), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = scoreLabel, fontSize = 12.sp, color = Color.Gray)
            Text(
                text = result.riskLevel.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}