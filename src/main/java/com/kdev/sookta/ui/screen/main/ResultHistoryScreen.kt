package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.R
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.data.EvaluationEntity
import com.kdev.sookta.model.BodyPart
import com.kdev.sookta.model.RiskLevel
import com.kdev.sookta.ui.component.TTSButton
import com.kdev.sookta.utils.TextToSpeechManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultHistoryScreen(navController: NavController, historyId: Int) {
    val context = LocalContext.current
    var evaluation by remember { mutableStateOf<EvaluationEntity?>(null) }
    val ttsManager = remember { TextToSpeechManager(context) }
    DisposableEffect(Unit) { onDispose { ttsManager.shutdown() } }
    // ดึงข้อมูล
    LaunchedEffect(historyId) {
        val db = AppDatabase.getDatabase(context)
        evaluation = db.evaluationDao().getEvaluationById(historyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_detail_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFDF8E1))
            )
        }
    ) { padding ->
        if (evaluation == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF5C9A81))
            }
        } else {
            val item = evaluation!!

            val beforeScore = item.scoreBefore.toInt()
            val afterScore = item.scoreAfter.toInt()

            val beforeColor = getScoreColor(beforeScore)
            val afterColor = getScoreColor(afterScore)

            // คำนวณเงินที่ประหยัดได้ (Logic ประมาณการ)
            val lossBefore = item.economicLoss
            val moneySaved = if (afterScore <= 3) lossBefore else (lossBefore * 0.5).toInt()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFDF8E1))
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // แสดงวันที่ตาม Locale ของเครื่อง
                val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault()).format(Date(item.dateTimestamp))
                Text(stringResource(R.string.history_date_prefix, dateStr), fontSize = 14.sp, color = Color.Gray)

                Spacer(Modifier.height(8.dp))
                // ชื่อกิจกรรม (อาจจะเป็น Key หรือ Text ก็ได้ แต่แสดงผลไปเลย)
                Text(item.activityName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C9A81))

                Spacer(Modifier.height(24.dp))

                // --- 1. คะแนน Before / After ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.history_result_header), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RiskScoreItem(beforeScore, beforeColor, stringResource(R.string.label_before))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.LightGray)
                            RiskScoreItem(afterScore, afterColor, stringResource(R.string.label_after))
                        }

                        // แสดงเงิน
                        if (moneySaved > 0) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFFFA000))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.money_saved_title) + " $moneySaved " + stringResource(R.string.money_saved_unit).replace("%1\$d", ""), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // --- 2. Body Map (Real Data) ---
                val bodyRisks = remember(item.bodyMapData) { parseBodyMapString(item.bodyMapData) }

                Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.history_risky_point_header), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))

                        // 1. วาด Body Map
                        BodyMapVisualization(bodyRisks = bodyRisks)

                        Spacer(Modifier.height(16.dp))

                        // 2. [NEW] วนลูปแสดงรายชื่ออวัยวะ (ใช้ Logic เดียวกับ FinalResult)
                        val riskyParts = bodyRisks.filter { it.value != RiskLevel.LOW }
                        if (riskyParts.isNotEmpty()) {
                            riskyParts.forEach { (part, level) ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Box(Modifier.size(10.dp).background(Color(level.colorHex), CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Text("${getBodyPartName(part)}: ${getRiskLevelName(level)}", fontSize = 14.sp, color = Color.DarkGray)
                                }
                            }
                        } else {
                            Text(stringResource(R.string.no_risky_parts), fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // --- 3. คำแนะนำ ---
                if (!item.improvementNote.isNullOrEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.history_suggestion_header), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Spacer(Modifier.height(12.dp))
                            val suggestions = item.improvementNote.split(", ")
                            suggestions.forEach { note ->
                                // แปลง Key กลับเป็น Text (ถ้า note เป็น Key)
                                val displayNote = if (note.all { it.isDigit() }) {
                                    try { context.getString(note.toInt()) } catch (e: Exception) { note }
                                } else {
                                    getResString(context, note) ?: note
                                }

                                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(displayNote, fontSize = 14.sp, color = Color.DarkGray,modifier = Modifier.weight(1f))
                                    TTSButton(text = displayNote, ttsManager = ttsManager, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Helper Functions ---

fun parseBodyMapString(data: String?): Map<BodyPart, RiskLevel> {
    if (data.isNullOrEmpty()) return emptyMap()
    val map = mutableMapOf<BodyPart, RiskLevel>()
    try {
        val pairs = data.split(",")
        for (pair in pairs) {
            val parts = pair.split(":")
            if (parts.size == 2) {
                val bodyPart = BodyPart.valueOf(parts[0])
                val riskLevel = RiskLevel.valueOf(parts[1])
                map[bodyPart] = riskLevel
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return map
}

fun getScoreColor(score: Int): Color {
    return when {
        score <= 3 -> Color(0xFF8BC34A)
        score <= 6 -> Color(0xFFFFEB3B)
        else -> Color(0xFFFF5252)
    }
}

fun mapTechScoreToUserScore(techScore: Double): Int {
    return when {
        techScore <= 1 -> 1
        techScore <= 3 -> 3
        techScore <= 7 -> 5
        techScore <= 10 -> 7
        else -> 9
    }.coerceIn(1, 9)
}

