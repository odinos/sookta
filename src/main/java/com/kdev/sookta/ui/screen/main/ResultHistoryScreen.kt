package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.data.EvaluationEntity
import com.kdev.sookta.model.BodyPart
import com.kdev.sookta.model.RiskLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultHistoryScreen(navController: NavController, historyId: Int) {
    val context = LocalContext.current
    var evaluation by remember { mutableStateOf<EvaluationEntity?>(null) }

    // ดึงข้อมูล
    LaunchedEffect(historyId) {
        val db = AppDatabase.getDatabase(context)
        evaluation = db.evaluationDao().getEvaluationById(historyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("รายละเอียดผลตรวจ", fontWeight = FontWeight.Bold) },
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

            // แปลงคะแนนดิบกลับเป็น User Score 1-9 (ใช้ Helper)
            val beforeScore = mapTechScoreToUserScore(item.scoreBefore)
            val afterScore = mapTechScoreToUserScore(item.scoreAfter)

            val beforeColor = getScoreColor(beforeScore)
            val afterColor = getScoreColor(afterScore)

            // [NEW] ดึงค่าจริงจาก DB
            val lossBefore = item.economicLoss // ค่าที่บันทึกไว้ตอนนั้น (Before)
            val lossAfter = 0 // สมมติ After = 0 หรือถ้าจะเก็บ After ใน DB ก็ต้องเพิ่ม Field แต่ส่วนใหญ่ After จะเป็น 0 ถ้าแก้ได้
            // เพื่อให้ง่าย เราใช้ค่า lossBefore เป็นตัวตั้ง ถ้า afterScore ต่ำ เราถือว่าประหยัดได้หมด
            val moneySaved = if (afterScore <= 3) lossBefore else (lossBefore * 0.5).toInt() // Logic ประมาณการ

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFDF8E1))
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.forLanguageTag("th")).format(Date(item.dateTimestamp))
                Text("บันทึกเมื่อ: $dateStr", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
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
                        Text("ผลการประเมิน", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RiskScoreItem(beforeScore, beforeColor, "ก่อนปรับ")
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.LightGray)
                            RiskScoreItem(afterScore, afterColor, "หลังปรับ")
                        }

                        // แสดงเงิน
                        if (moneySaved > 0) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFFFA000))
                                Spacer(Modifier.width(8.dp))
                                Text("ลดความสูญเสียได้ $moneySaved บาท/ปี", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // --- 2. Body Map (Real Data) ---
                // แปลง String จาก DB กลับเป็น Map
                val bodyRisks = remember(item.bodyMapData) {
                    parseBodyMapString(item.bodyMapData)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("จุดเสี่ยงที่พบ (จากประวัติ)", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        BodyMapVisualization(bodyRisks = bodyRisks)
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
                            Text("แนวทางที่คุณเลือกไว้", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Spacer(Modifier.height(12.dp))
                            val suggestions = item.improvementNote.split(", ")
                            suggestions.forEach { note ->
                                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Check, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(note, fontSize = 14.sp, color = Color.DarkGray)
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

// แปลง String ใน DB กลับเป็น Map<BodyPart, RiskLevel>
fun parseBodyMapString(data: String?): Map<BodyPart, RiskLevel> {
    if (data.isNullOrEmpty()) return emptyMap()
    val map = mutableMapOf<BodyPart, RiskLevel>()
    try {
        // Format: "NECK:HIGH,TRUNK:LOW"
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

// Helper จำลองการแปลง Tech Score กลับเป็น User Score (1-9)
// (ถ้าในอนาคตเก็บ User Score ลง DB เลยก็ไม่ต้องใช้ตัวนี้)
fun mapTechScoreToUserScore(techScore: Double): Int {
    // REBA Logic (simplified)
    return when {
        techScore <= 1 -> 1
        techScore <= 3 -> 3
        techScore <= 7 -> 5
        techScore <= 10 -> 7
        else -> 9
    }.coerceIn(1, 9)
}