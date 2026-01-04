package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.R
import com.kdev.sookta.model.BodyPart
import com.kdev.sookta.model.ErgoInputData
import com.kdev.sookta.model.ErgoResult
import com.kdev.sookta.model.JobType
import com.kdev.sookta.model.RebaInputData
import com.kdev.sookta.model.RiskLevel
import com.kdev.sookta.utils.ErgoCalculatorHelper

@Composable
fun InitialRiskScreen(navController: NavController, activityNameArg: String, scoreArg: Int) {
    // 1. รับข้อมูล
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val initialResult = savedStateHandle?.get<ErgoResult>("riskResult")
    val inputDataRaw = savedStateHandle?.get<Any>("inputData")

    if (initialResult == null || inputDataRaw == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ไม่พบข้อมูลการประเมิน กรุณาลองใหม่อีกครั้ง", color = Color.Red)
        }
        return
    }

    val displayActivityName = activityNameArg.toIntOrNull()?.let { stringResource(it) } ?: activityNameArg

    // 2. State Simulation
    var solutionInputIso by remember { mutableStateOf(if (inputDataRaw is ErgoInputData) inputDataRaw else null) }
    var solutionInputReba by remember { mutableStateOf(if (inputDataRaw is RebaInputData) inputDataRaw else null) }
    val jobType = solutionInputIso?.jobType ?: JobType.REBA

    val selectedSuggestions = remember { mutableStateListOf<String>() }

    val finalResult = remember(solutionInputIso, solutionInputReba) {
        if (solutionInputIso != null) {
            if (solutionInputIso!!.jobType == JobType.LIFTING) {
                ErgoCalculatorHelper.calculateLiftingRisk(solutionInputIso!!)
            } else {
                ErgoCalculatorHelper.calculatePushPullRisk(solutionInputIso!!)
            }
        } else if (solutionInputReba != null) {
            ErgoCalculatorHelper.calculateRebaRisk(solutionInputReba!!)
        } else {
            initialResult
        }
    }

    val contextSuggestions = remember(initialResult, activityNameArg) {
        val baseSuggestions = initialResult.suggestionList.ifEmpty { listOf(initialResult.suggestion) }

        // ถ้าเป็นงานเฉพาะทาง ให้เพิ่มคำแนะนำพิเศษ (Hardcode เสริมเข้าไป)
        val extraSuggestions = mutableListOf<String>()
        if (activityNameArg.contains("พ่นยา") || activityNameArg.contains("Pesticide")) {
            extraSuggestions.add("ปรับสายสะพายเครื่องพ่นยาให้กระชับ")
            extraSuggestions.add("สลับข้างสะพายถังเพื่อลดการกดทับไหล่เดียว")
        } else if (activityNameArg.contains("ตัดแต่ง") || activityNameArg.contains("Pruning")) {
            extraSuggestions.add("ใช้บันไดที่มั่นคงแทนการเอื้อมสุดแขน")
            extraSuggestions.add("ใช้กรรไกรตัดกิ่งด้ามยาว")
        } else if (activityNameArg.contains("ใส่ปุ๋ย") || activityNameArg.contains("Fertilizing")) {
            extraSuggestions.add("ใช้รถเข็นบรรทุกกระสอบปุ๋ยแทนการแบก")
        }

        // รวมคำแนะนำเดิม + คำแนะนำเสริม (ไม่ให้ซ้ำ)
        (baseSuggestions + extraSuggestions).distinct()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.initial_risk_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Text("กิจกรรม: $displayActivityName", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(24.dp))

        // --- ส่วนที่ 1: คะแนน และ Body Map ---
        Text("ผลการประเมินความเสี่ยง", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF333333))
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // ปรับระยะห่าง
            verticalAlignment = Alignment.Top
        ) {
            // ซ้าย: คะแนนความเสี่ยง
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                RiskScoreCircle(result = initialResult)
                Spacer(Modifier.height(12.dp))
                if (initialResult.economicLoss > 0) {
                    Text("อาจสูญเสียรายได้", fontSize = 12.sp, color = Color.Gray)
                    Text("${initialResult.economicLoss} บ./ปี", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }
            }

            // ขวา: Body Map + รายชื่ออวัยวะ
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("ตำแหน่งที่เสี่ยง", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                // รูป Body Map
                BodyMapVisualization(bodyRisks = initialResult.bodyPartRisks)

                Spacer(Modifier.height(12.dp))

                // [NEW] รายการระบุชื่ออวัยวะ (Text Legend)
                val riskyParts = initialResult.bodyPartRisks.filter { it.value != RiskLevel.LOW }
                if (riskyParts.isNotEmpty()) {
                    riskyParts.forEach { (part, level) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            // จุดสีเล็กๆ หน้าชื่อ
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(Color(level.colorHex), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            // ชื่ออวัยวะภาษาไทย และระดับความเสี่ยง
                            Text(
                                text = "${getBodyPartNameTH(part)} (${level.label})",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                } else {
                    Text("ไม่มีจุดเสี่ยงรุนแรง", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp), // อาจเพิ่ม padding ถ้าต้องการ
            thickness = 1.dp,
            color = Color.LightGray.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(24.dp))

        // --- ส่วนที่ 2: Checklist คำแนะนำ ---
        Text(
            "แนวทางปรับปรุง (เลือกสิ่งที่ทำได้)",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF1565C0),
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            "เลือกหัวข้อด้านล่างเพื่อยืนยันว่าจะนำไปปรับปรุง",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(12.dp))

        contextSuggestions.forEach { suggestion ->
            val isSelected = selectedSuggestions.contains(suggestion)
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        if (isSelected) selectedSuggestions.remove(suggestion) else selectedSuggestions.add(suggestion)
                    }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked -> if (checked) selectedSuggestions.add(suggestion) else selectedSuggestions.remove(suggestion) },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2E7D32))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = suggestion, fontSize = 14.sp, color = if (isSelected) Color(0xFF2E7D32) else Color.DarkGray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- ส่วนที่ 3: Simulation ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFFE65100))
                    Spacer(Modifier.width(8.dp))
                    Text("ลองปรับค่าคำนวณ (Simulation)", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                }
                Spacer(Modifier.height(16.dp))

                if (jobType == JobType.LIFTING && solutionInputIso != null) {
                    SolutionSlider("น้ำหนักวัตถุ (kg)", solutionInputIso!!.loadWeight.toFloat(), 0f..solutionInputIso!!.loadWeight.toFloat()) { solutionInputIso = solutionInputIso!!.copy(loadWeight = it.toDouble()) }
                    SolutionSlider("ความสูงจุดยก (V)", solutionInputIso!!.verticalHeight.toFloat(), 0f..180f) { solutionInputIso = solutionInputIso!!.copy(verticalHeight = it.toDouble()) }
                } else if (jobType == JobType.PUSH_PULL && solutionInputIso != null) {
                    SolutionSlider("แรงเข็น (N)", solutionInputIso!!.sustainForce.toFloat(), 0f..solutionInputIso!!.sustainForce.toFloat()) { solutionInputIso = solutionInputIso!!.copy(sustainForce = it.toDouble()) }
                } else if (jobType == JobType.REBA && solutionInputReba != null) {
                    Text("ลองลดคะแนนท่าทาง", fontSize = 12.sp, color = Color.Gray)
                    SolutionSliderInt("ลำตัว (Trunk)", solutionInputReba!!.trunkScore, 1..solutionInputReba!!.trunkScore) { solutionInputReba = solutionInputReba!!.copy(trunkScore = it) }
                    SolutionSliderInt("แขน (Upper Arm)", solutionInputReba!!.upperArmScore, 1..solutionInputReba!!.upperArmScore) { solutionInputReba = solutionInputReba!!.copy(upperArmScore = it) }
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("คะแนนจำลอง: ", fontSize = 14.sp)
                    Box(Modifier.background(Color(finalResult.userScoreColor), CircleShape).padding(horizontal = 8.dp)) {
                        Text("${finalResult.userScore}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = {
                navController.currentBackStackEntry?.savedStateHandle?.set("initialResult", initialResult)
                navController.currentBackStackEntry?.savedStateHandle?.set("finalResult", finalResult)
                navController.currentBackStackEntry?.savedStateHandle?.set("selectedSuggestions", ArrayList(selectedSuggestions))

                val oldScoreStr = initialResult.userScore.toString()
                val newScoreStr = finalResult.userScore.toString()
                navController.navigate("final_result/$oldScoreStr/$newScoreStr/$activityNameArg")
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text("สรุปผลการปรับปรุง", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(50.dp))
    }
}

// --- Helper: แปลงชื่ออวัยวะเป็นภาษาไทย ---
fun getBodyPartNameTH(part: BodyPart): String {
    return when (part) {
        BodyPart.NECK -> "คอ"
        BodyPart.TRUNK -> "หลัง/ลำตัว"  // ระบุชัดเจนว่าเป็นหลัง
        BodyPart.LEGS -> "ขา/เข่า"
        BodyPart.ARMS -> "ไหล่/แขน"
        BodyPart.WRISTS -> "ข้อมือ"
    }
}

// --- Helper: Body Map Visualization ---
@Composable
fun BodyMapVisualization(bodyRisks: Map<BodyPart, RiskLevel>) {
    Box(
        modifier = Modifier
            .width(100.dp) // ปรับขนาดให้พอดี
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = 5f, cap = StrokeCap.Round)
            val colorBody = Color.LightGray

            // วาดคน (Stickman)
            drawCircle(colorBody, radius = w * 0.12f, center = Offset(w / 2, h * 0.15f), style = stroke) // หัว
            drawLine(colorBody, start = Offset(w / 2, h * 0.2f), end = Offset(w / 2, h * 0.55f), strokeWidth = 5f, cap = StrokeCap.Round) // ลำตัว
            drawLine(colorBody, start = Offset(w / 2, h * 0.25f), end = Offset(w * 0.15f, h * 0.4f), strokeWidth = 5f, cap = StrokeCap.Round) // แขนซ้าย
            drawLine(colorBody, start = Offset(w / 2, h * 0.25f), end = Offset(w * 0.85f, h * 0.4f), strokeWidth = 5f, cap = StrokeCap.Round) // แขนขวา
            drawLine(colorBody, start = Offset(w / 2, h * 0.55f), end = Offset(w * 0.25f, h * 0.9f), strokeWidth = 5f, cap = StrokeCap.Round) // ขาซ้าย
            drawLine(colorBody, start = Offset(w / 2, h * 0.55f), end = Offset(w * 0.75f, h * 0.9f), strokeWidth = 5f, cap = StrokeCap.Round) // ขาขวา
        }

        // Overlay จุดสีตามความเสี่ยง
        bodyRisks[BodyPart.NECK]?.let { RiskDot(Modifier.align(Alignment.TopCenter).offset(y = 25.dp), it) }
        bodyRisks[BodyPart.TRUNK]?.let { RiskDot(Modifier.align(Alignment.Center).offset(y = (-20).dp), it) }
        bodyRisks[BodyPart.ARMS]?.let {
            RiskDot(Modifier.align(Alignment.TopStart).offset(x = 5.dp, y = 60.dp), it)
            RiskDot(Modifier.align(Alignment.TopEnd).offset(x = (-5).dp, y = 60.dp), it)
        }
        bodyRisks[BodyPart.WRISTS]?.let {
            RiskDot(Modifier.align(Alignment.TopStart).offset(x = 0.dp, y = 80.dp), it)
            RiskDot(Modifier.align(Alignment.TopEnd).offset(x = 0.dp, y = 80.dp), it)
        }
        bodyRisks[BodyPart.LEGS]?.let { RiskDot(Modifier.align(Alignment.BottomCenter).offset(y = (-30).dp), it) }
    }
}

@Composable
fun RiskDot(modifier: Modifier, level: RiskLevel) {
    if (level != RiskLevel.LOW) {
        Box(
            modifier = modifier
                .size(14.dp)
                .background(Color(level.colorHex), CircleShape)
                .border(1.dp, Color.White, CircleShape)
        )
    }
}

// ... (RiskScoreCircle, Sliders - Code เดิม ใช้ต่อได้เลยครับ) ...
@Composable
fun RiskScoreCircle(result: ErgoResult) {
    val color = Color(result.userScoreColor)
    Box(
        modifier = Modifier
            .size(110.dp)
            .background(Color.White, shape = CircleShape)
            .border(8.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${result.userScore}", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = "ระดับความเสี่ยง", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SolutionSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp)
            Text(String.format("%.1f", value), fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 12.sp)
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
            Text(label, fontSize = 12.sp)
            Text("$value", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 12.sp)
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