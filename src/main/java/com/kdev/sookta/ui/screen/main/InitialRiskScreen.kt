package com.kdev.sookta.ui.screen.main

import android.content.Context
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
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.kdev.sookta.ui.component.TTSButton
import com.kdev.sookta.utils.ErgoCalculatorHelper
import com.kdev.sookta.utils.TextToSpeechManager
import kotlin.math.max

@Composable
fun InitialRiskScreen(navController: NavController, activityNameArg: String, scoreArg: Int) {
    val context = LocalContext.current
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val initialResult = savedStateHandle?.get<ErgoResult>("riskResult")
    val inputDataRaw = savedStateHandle?.get<Any>("inputData")
    val ttsManager = remember { TextToSpeechManager(context) }

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    if (initialResult == null || inputDataRaw == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.risk_not_found), color = Color.Red)
        }
        return
    }

    val displayActivityName = activityNameArg.toIntOrNull()?.let { stringResource(it) } ?: activityNameArg

    // เก็บรายการ Key ที่เลือก (สำหรับส่งไปหน้าสรุป)
    val selectedSuggestionKeys = remember { mutableStateListOf<String>() }

    // --- สร้างรายการคำแนะนำ (Key + Label) ---
    // [Fix 1] ใช้ getResString แบบ safe ไม่เรียก context ใน scope ที่ผิด
    // ใช้ derivedStateOf เพื่อให้คำนวณใหม่เมื่อ input เปลี่ยนเท่านั้น (และ context มักไม่เปลี่ยน)
    val suggestionOptions = remember(initialResult, activityNameArg) {
        val options = mutableListOf<Pair<String, String>>()

        // 1. จากผลประเมิน
        val baseKeys = initialResult.suggestionList.ifEmpty { listOf(initialResult.suggestion) }
        baseKeys.forEach { key ->
            val label = getResString(context, key) ?: ""
            if (label.isNotEmpty()) options.add(key to label)
        }

        // 2. จากประเภทงาน (Extra)
        val actNameLower = activityNameArg.lowercase()
        if (actNameLower.contains("พ่น") || actNameLower.contains("spray")) {
            getResString(context, "act_extra_spray_strap")?.let { options.add("act_extra_spray_strap" to it) }
        } else if (actNameLower.contains("ตัดแต่ง") || actNameLower.contains("prun")) {
            getResString(context, "act_extra_prune_ladder")?.let { options.add("act_extra_prune_ladder" to it) }
        } else if (actNameLower.contains("ใส่ปุ๋ย") || actNameLower.contains("fert")) {
            getResString(context, "act_extra_fert_cart")?.let { options.add("act_extra_fert_cart" to it) }
        }

        options.distinctBy { it.first }
    }

    // --- เตรียมข้อความเสียงสำหรับผลลัพธ์ (Result TTS) ---
    // [Fix 2] ดึงค่า String Resource มาเก็บเป็นตัวแปร Composable ปกติก่อน
    // เพื่อหลีกเลี่ยงการเรียก context หรือ stringResource ในที่ที่ไม่ควร
    val riskLevelName = getRiskLevelName(initialResult.riskLevel)
    val riskLabel = stringResource(R.string.risk_level_label)
    val lossLabel = stringResource(R.string.loss_label)
    val riskyPartsHeader = stringResource(R.string.risky_parts_header)
    val noRiskyPartsLabel = stringResource(R.string.no_risky_parts) // ดึงค่าตรงนี้เลย ไม่ต้อง try-catch

    val neckStr = stringResource(R.string.part_neck)
    val trunkStr = stringResource(R.string.part_trunk)
    val legsStr = stringResource(R.string.part_legs)
    val armsStr = stringResource(R.string.part_arms)
    val wristsStr = stringResource(R.string.part_wrists)

    // คำนวณ String สุดท้าย
    val resultTTSString = remember(initialResult, riskLevelName, riskLabel, lossLabel, riskyPartsHeader, noRiskyPartsLabel, neckStr, trunkStr, legsStr, armsStr, wristsStr) {
        val bodyPartNames = initialResult.bodyPartRisks
            .filter { it.value != RiskLevel.LOW }
            .keys.joinToString(", ") { part ->
                when (part) {
                    BodyPart.NECK -> neckStr
                    BodyPart.TRUNK -> trunkStr
                    BodyPart.LEGS -> legsStr
                    BodyPart.ARMS -> armsStr
                    BodyPart.WRISTS -> wristsStr
                }
            }

        buildString {
            append(displayActivityName)
            append(". ")
            append(riskLevelName)
            append(". ")
            append("$riskLabel ${initialResult.userScore}")
            append(". ")
            if (initialResult.economicLoss > 0) {
                append("$lossLabel ${initialResult.economicLoss} baht")
                append(". ")
            }
            if (bodyPartNames.isNotEmpty()) {
                append("$riskyPartsHeader: $bodyPartNames")
            } else {
                append(noRiskyPartsLabel)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header ---
        Text(stringResource(R.string.initial_risk_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Text(stringResource(R.string.eval_activity_prefix, displayActivityName), color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(24.dp))

        // --- Result Card (ผลประเมิน + ฟังเสียง) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header Row with TTS Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.risk_result_header), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF333333))

                    // ปุ่มฟังผลประเมินทั้งหมด
                    FilledIconButton(
                        onClick = { ttsManager.speak(resultTTSString) },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Play Result", tint = Color(0xFF2E7D32))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Score Circle
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        RiskScoreCircle(result = initialResult)
                        Spacer(Modifier.height(12.dp))
                        if (initialResult.economicLoss > 0) {
                            Text(stringResource(R.string.loss_label), fontSize = 12.sp, color = Color.Gray)
                            Text(stringResource(R.string.loss_unit_year, initialResult.economicLoss), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        }
                    }

                    // Body Map & List
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.risky_parts_header), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))

                        // Body Map
                        BodyMapVisualization(bodyRisks = initialResult.bodyPartRisks)

                        Spacer(Modifier.height(8.dp))

                        // Text List of Body Parts
                        val riskyParts = initialResult.bodyPartRisks.filter { it.value != RiskLevel.LOW }
                        if (riskyParts.isNotEmpty()) {
                            riskyParts.forEach { (part, level) ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                                    Box(Modifier.size(8.dp).background(Color(level.colorHex), CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                    // [Fix] ใช้ getBodyPartName Composable ที่แก้ไขแล้ว
                                    Text(getBodyPartName(part), fontSize = 12.sp, color = Color.DarkGray)
                                }
                            }
                        } else {
                            Text(noRiskyPartsLabel, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))

        // --- Suggestions Checklist (แนวทางปรับปรุง + ฟังเสียง) ---
        Text(stringResource(R.string.improvement_header), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1565C0), modifier = Modifier.align(Alignment.Start))
        Text(stringResource(R.string.improvement_desc), fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(12.dp))

        suggestionOptions.forEach { (key, label) ->
            val isSelected = selectedSuggestionKeys.contains(key)

            Card(
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable {
                        if (isSelected) selectedSuggestionKeys.remove(key) else selectedSuggestionKeys.add(key)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { if (it) selectedSuggestionKeys.add(key) else selectedSuggestionKeys.remove(key) },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2E7D32))
                    )
                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = label,
                        fontSize = 15.sp, // เพิ่มขนาดตัวอักษรให้อ่านง่าย
                        color = if (isSelected) Color(0xFF2E7D32) else Color.Black,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(4.dp))
                    // ปุ่มฟังเสียงคำแนะนำรายข้อ
                    TTSButton(text = label, ttsManager = ttsManager)
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        // --- Summarize Button ---
        Button(
            onClick = {
                navController.currentBackStackEntry?.savedStateHandle?.set("initialResult", initialResult)
                // เนื่องจากตัด Simulation ออก finalResult จึงเท่ากับ initialResult
                navController.currentBackStackEntry?.savedStateHandle?.set("finalResult", initialResult)
                navController.currentBackStackEntry?.savedStateHandle?.set("selectedSuggestions", ArrayList(selectedSuggestionKeys))
                navController.navigate("final_result/${initialResult.userScore}/${initialResult.userScore}/$activityNameArg")
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text(stringResource(R.string.btn_summarize), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(50.dp))
    }
}

// --- Helper Functions ---

// [Fix] ใช้ logic ที่ปลอดภัย ไม่เรียก context.getString ใน Composable scope โดยตรง
fun getResString(context: Context, key: String): String? {
    return try {
        // 1. ลองแปลง key เป็น Int (กรณีเป็น Resource ID "213123...")
        if (key.all { it.isDigit() }) {
            val resId = key.toInt()
            // ใช้ context.getString ได้เพราะนี่คือ Normal Function (ไม่ใช่ Composable Scope โดยตรง)
            context.getString(resId)
        } else {
            // 2. กรณีเป็น String Key ("act_reduce_weight")
            val resId = context.resources.getIdentifier(key, "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        }
    } catch (e: Exception) {
        null // คืนค่า null หากเกิดข้อผิดพลาดใดๆ
    }
}

@Composable
fun getBodyPartName(part: BodyPart): String {
    // ใช้ stringResource โดยตรงใน Composable function
    return when (part) {
        BodyPart.NECK -> stringResource(R.string.part_neck)
        BodyPart.TRUNK -> stringResource(R.string.part_trunk)
        BodyPart.LEGS -> stringResource(R.string.part_legs)
        BodyPart.ARMS -> stringResource(R.string.part_arms)
        BodyPart.WRISTS -> stringResource(R.string.part_wrists)
    }
}

@Composable
fun getRiskLevelName(level: RiskLevel): String {
    return when (level) {
        RiskLevel.LOW -> stringResource(R.string.risk_lvl_low)
        RiskLevel.MEDIUM -> stringResource(R.string.risk_lvl_med)
        RiskLevel.HIGH -> stringResource(R.string.risk_lvl_high)
        RiskLevel.VERY_HIGH -> stringResource(R.string.risk_lvl_vhigh)
    }
}

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
            Text(text = stringResource(R.string.risk_level_label), fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun BodyMapVisualization(bodyRisks: Map<BodyPart, RiskLevel>) {
    Box(
        modifier = Modifier
            .width(100.dp)
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