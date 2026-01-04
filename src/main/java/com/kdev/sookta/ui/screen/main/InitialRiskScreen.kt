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
    // อย่าลืม shutdown เมื่อออกจากหน้านี้
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

    // เก็บข้อมูลตั้งต้น (Original) ไว้เทียบและ reset
    val originalIso = remember { if (inputDataRaw is ErgoInputData) inputDataRaw else null }
    val originalReba = remember { if (inputDataRaw is RebaInputData) inputDataRaw else null }
    val jobType = originalIso?.jobType ?: JobType.REBA

    // State สำหรับ Simulation (ค่าที่จะถูกเปลี่ยน)
    var solutionInputIso by remember { mutableStateOf(originalIso) }
    var solutionInputReba by remember { mutableStateOf(originalReba) }

    // เก็บรายการ Key ที่เลือก (เช่น ["act_reduce_weight", "act_avoid_bend"])
    val selectedSuggestionKeys = remember { mutableStateListOf<String>() }

    // --- Logic 1: สร้างรายการคำแนะนำ (Key + Label) ---
    val suggestionOptions = remember(initialResult, activityNameArg) {
        val options = mutableListOf<Pair<String, String>>()

        // 1. จากผลประเมิน (Base)
        val baseKeys = initialResult.suggestionList.ifEmpty { listOf(initialResult.suggestion) }
        baseKeys.forEach { key ->
            val label = if (key.all { it.isDigit() }) {
                try { context.getString(key.toInt()) } catch (e: Exception) { "" }
            } else {
                getResString(context, key)
            }
            if (!label.isNullOrEmpty()) options.add(key to label)
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

        options.distinctBy { it.first } // ลบตัวซ้ำ
    }

    // --- Logic 2: คำนวณค่าใหม่เมื่อติ๊กเลือก (Auto-Simulation) ---
    LaunchedEffect(selectedSuggestionKeys.size) { // ทำงานเมื่อมีการเลือก/เลิกเลือก
        // 1. Reset กลับไปค่าเดิมก่อน
        var newIso = originalIso?.copy()
        var newReba = originalReba?.copy()

        // 2. วนลูปดูว่าเลือกอะไรบ้าง แล้วปรับค่าตาม Logic
        selectedSuggestionKeys.forEach { key ->
            when (key) {
                // กลุ่ม ISO (ยก/เข็น)
                "act_reduce_weight", "act_reduce_load_tool", "act_extra_fert_cart" -> {
                    // ลดน้ำหนักลง 30% หรือเหลือ 10kg แล้วแต่ว่าอะไรน้อยกว่า
                    newIso = newIso?.copy(loadWeight = newIso.loadWeight * 0.7)
                    newReba = newReba?.copy(loadScore = max(0, newReba.loadScore - 1))
                }
                "act_check_wheels", "act_use_legs" -> {
                    // ลดแรงเข็นลง 20%
                    newIso = newIso?.copy(
                        initialForce = newIso.initialForce * 0.8,
                        sustainForce = newIso.sustainForce * 0.8
                    )
                }

                // กลุ่ม REBA (ท่าทาง)
                "act_avoid_bend" -> {
                    // ลดคะแนนหลัง (Trunk)
                    newReba = newReba?.copy(trunkScore = max(1, newReba.trunkScore - 1))
                }
                "act_adj_eye_level" -> {
                    // ลดคะแนนคอ (Neck)
                    newReba = newReba?.copy(neckScore = max(1, newReba.neckScore - 1))
                }
                "act_reduce_arm_raise", "act_extra_prune_tool", "act_extra_prune_ladder" -> {
                    // ลดคะแนนแขน (Upper Arm)
                    newReba = newReba?.copy(upperArmScore = max(1, newReba.upperArmScore - 1))
                }
                "act_adj_wrist" -> {
                    // ลดคะแนนข้อมือ
                    newReba = newReba?.copy(wristScore = max(1, newReba.wristScore - 1))
                }
                "act_rest_stretch" -> {
                    // พักผ่อน อาจลดความถี่ลงเล็กน้อย (ใน ISO)
                    newIso = newIso?.copy(liftFrequency = max(0.2, newIso.liftFrequency * 0.8))
                }
            }
        }

        // 3. อัปเดต State เพื่อให้ UI และคะแนนเปลี่ยน
        solutionInputIso = newIso
        solutionInputReba = newReba
    }

    // Recalculate Final Result
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.initial_risk_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Text(stringResource(R.string.eval_activity_prefix, displayActivityName), color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(24.dp))

        // --- Result & Body Map ---
        Text(stringResource(R.string.risk_result_header), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF333333))
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                RiskScoreCircle(result = initialResult)
                Spacer(Modifier.height(12.dp))
                if (initialResult.economicLoss > 0) {
                    Text(stringResource(R.string.loss_label), fontSize = 12.sp, color = Color.Gray)
                    Text(stringResource(R.string.loss_unit_year, initialResult.economicLoss), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.risky_parts_header), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                BodyMapVisualization(bodyRisks = initialResult.bodyPartRisks)
                Spacer(Modifier.height(12.dp))

                val riskyParts = initialResult.bodyPartRisks.filter { it.value != RiskLevel.LOW }
                if (riskyParts.isNotEmpty()) {
                    riskyParts.forEach { (part, level) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Box(Modifier.size(10.dp).background(Color(level.colorHex), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("${getBodyPartName(part)} (${getRiskLevelName(level)})", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                } else {
                    Text(stringResource(R.string.no_risky_parts), fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.5f))
        Spacer(Modifier.height(24.dp))

        // --- Checklist (แก้ไขให้ใช้ Key ในการเลือก) ---
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
                    .padding(vertical = 4.dp)
                    .clickable {
                        if (isSelected) selectedSuggestionKeys.remove(key) else selectedSuggestionKeys.add(key)
                    }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { if (it) selectedSuggestionKeys.add(key) else selectedSuggestionKeys.remove(key) },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2E7D32))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label, // แสดงข้อความภาษาไทย/อังกฤษ
                        fontSize = 14.sp,
                        color = if (isSelected) Color(0xFF2E7D32) else Color.DarkGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    TTSButton(text = label, ttsManager = ttsManager)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- Simulation Sliders (ยังคงทำงานร่วมกันได้) ---
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFFE65100))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.simulation_title), fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                }
                Spacer(Modifier.height(16.dp))

                if (jobType == JobType.LIFTING && solutionInputIso != null) {
                    SolutionSlider(stringResource(R.string.sim_weight), solutionInputIso!!.loadWeight.toFloat(), 0f..max(40f, solutionInputIso!!.loadWeight.toFloat())) { solutionInputIso = solutionInputIso!!.copy(loadWeight = it.toDouble()) }
                    SolutionSlider(stringResource(R.string.sim_height), solutionInputIso!!.verticalHeight.toFloat(), 0f..180f) { solutionInputIso = solutionInputIso!!.copy(verticalHeight = it.toDouble()) }
                } else if (jobType == JobType.PUSH_PULL && solutionInputIso != null) {
                    SolutionSlider(stringResource(R.string.sim_force), solutionInputIso!!.sustainForce.toFloat(), 0f..max(40f, solutionInputIso!!.sustainForce.toFloat())) { solutionInputIso = solutionInputIso!!.copy(sustainForce = it.toDouble()) }
                } else if (jobType == JobType.REBA && solutionInputReba != null) {
                    Text(stringResource(R.string.sim_reduce_posture), fontSize = 12.sp, color = Color.Gray)
                    SolutionSliderInt(stringResource(R.string.sim_trunk), solutionInputReba!!.trunkScore, 1..originalReba!!.trunkScore) { solutionInputReba = solutionInputReba!!.copy(trunkScore = it) }
                    SolutionSliderInt(stringResource(R.string.sim_upper_arm), solutionInputReba!!.upperArmScore, 1..originalReba!!.upperArmScore) { solutionInputReba = solutionInputReba!!.copy(upperArmScore = it) }
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.simulation_score_label), fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    // แสดงคะแนนใหม่ (Final Result)
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
                // ส่ง ArrayList<String> ที่เป็น Keys ไป
                navController.currentBackStackEntry?.savedStateHandle?.set("selectedSuggestions", ArrayList(selectedSuggestionKeys))
                navController.navigate("final_result/${initialResult.userScore}/${finalResult.userScore}/$activityNameArg")
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text(stringResource(R.string.btn_summarize), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(50.dp))
    }
}

// ... (Helper Functions ด้านล่างคงเดิม: getResString, getBodyPartName, etc.) ...
fun getResString(context: Context, key: String): String? {
    val resId = context.resources.getIdentifier(key, "string", context.packageName)
    return if (resId != 0) context.getString(resId) else null
}

@Composable
fun getBodyPartName(part: BodyPart): String {
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

// ... อย่าลืม BodyMapVisualization และ Sliders ...

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
            // หัว
            drawCircle(colorBody, radius = w * 0.12f, center = Offset(w / 2, h * 0.15f), style = stroke)
            // ลำตัว
            drawLine(colorBody, start = Offset(w / 2, h * 0.2f), end = Offset(w / 2, h * 0.55f), strokeWidth = 5f, cap = StrokeCap.Round)
            // แขนซ้าย
            drawLine(colorBody, start = Offset(w / 2, h * 0.25f), end = Offset(w * 0.15f, h * 0.4f), strokeWidth = 5f, cap = StrokeCap.Round)
            // แขนขวา
            drawLine(colorBody, start = Offset(w / 2, h * 0.25f), end = Offset(w * 0.85f, h * 0.4f), strokeWidth = 5f, cap = StrokeCap.Round)
            // ขาซ้าย
            drawLine(colorBody, start = Offset(w / 2, h * 0.55f), end = Offset(w * 0.25f, h * 0.9f), strokeWidth = 5f, cap = StrokeCap.Round)
            // ขาขวา
            drawLine(colorBody, start = Offset(w / 2, h * 0.55f), end = Offset(w * 0.75f, h * 0.9f), strokeWidth = 5f, cap = StrokeCap.Round)
        }

        // Overlay จุดสีตามความเสี่ยง (ใช้ RiskLevel.colorHex ถ้ามี หรือใช้ Helper Map)
        // หมายเหตุ: ตรงนี้ถ้า RiskLevel ของคุณไม่มี field colorHex ให้แจ้งผมครับ ผมจะแก้เป็น function map สีให้
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
                .background(Color(level.colorHex), CircleShape) // ต้องมี colorHex ใน RiskLevel
                .border(1.dp, Color.White, CircleShape)
        )
    }
}

// --- Helper: Sliders ---
@Composable
fun SolutionSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp)
            // แสดงทศนิยม 1 ตำแหน่ง
            Text(String.format("%.1f", value), fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 12.sp)
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
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF1565C0),
                activeTrackColor = Color(0xFF1565C0),
                inactiveTrackColor = Color(0xFFBBDEFB)
            )
        )
    }
}