package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.data.EvaluationEntity
import com.kdev.sookta.model.BodyPart
import com.kdev.sookta.model.ErgoResult
import com.kdev.sookta.model.RiskLevel
import kotlinx.coroutines.launch

@Composable
fun FinalResultScreen(navController: NavController, oldScoreArg: Int, newScoreArg: Int, activityNameArg: String) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    // แปลงชื่อกิจกรรม (เผื่อกรณีส่งมาเป็น ID)
    val displayActivityName = activityNameArg.toIntOrNull()?.let { stringResource(it) } ?: activityNameArg
    // 1. ดึงข้อมูล
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val initialResult = savedStateHandle?.get<ErgoResult>("initialResult")
    val finalResult = savedStateHandle?.get<ErgoResult>("finalResult")
    val selectedSuggestions = savedStateHandle?.get<ArrayList<String>>("selectedSuggestions") ?: emptyList<String>()

    // 2. ใช้ User Score
    val beforeScore = initialResult?.userScore ?: oldScoreArg
    val afterScore = finalResult?.userScore ?: newScoreArg

    var isSaved by rememberSaveable { mutableStateOf(false) }

    // [ส่วนสำคัญ] บันทึกลง Database พร้อมข้อมูลใหม่
    LaunchedEffect(Unit) {
        if (!isSaved && initialResult != null && finalResult != null) {

            // แปลง Map BodyPart เป็น String เพื่อเก็บใน DB (Format: "NECK:HIGH,TRUNK:LOW")
            val bodyMapString = initialResult.bodyPartRisks.entries.joinToString(",") { "${it.key.name}:${it.value.name}" }

            val entity = EvaluationEntity(
                activityName = displayActivityName, // แนะนำให้ส่งชื่อกิจกรรมจริงมาด้วยจะดีมาก
                dateTimestamp = System.currentTimeMillis(),
                scoreBefore = initialResult.techScore,
                riskBefore = initialResult.riskLevel.name,
                scoreAfter = finalResult.techScore,
                riskAfter = finalResult.riskLevel.name,
                improvementNote = selectedSuggestions.joinToString(", "),

                // [NEW] บันทึก Economic Loss และ Body Map
                economicLoss = initialResult.economicLoss,
                bodyMapData = bodyMapString
            )

            db.evaluationDao().insertEvaluation(entity)
            isSaved = true
        }
    }

    val beforeColor = initialResult?.userScoreColor?.let { Color(it) } ?: Color.Gray
    val afterColor = finalResult?.userScoreColor?.let { Color(it) } ?: Color(0xFF4CAF50)

    // 3. คำนวณความสูญเสีย
    val lossBefore = initialResult?.economicLoss ?: 0
    val lossAfter = finalResult?.economicLoss ?: 0
    val moneySaved = lossBefore - lossAfter

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF8E1))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))

        // Icon Success
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "บันทึกและสรุปผลสำเร็จ!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        Text(
            text = "ผลลัพธ์จากการจำลองการปรับปรุงของคุณ",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(32.dp))

        // --- ส่วนเปรียบเทียบ Before vs After ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("คะแนนความเสี่ยง (1-9)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF5C9A81))
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RiskScoreItem(score = beforeScore, color = beforeColor, label = "ก่อนปรับ")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.LightGray)
                    RiskScoreItem(score = afterScore, color = afterColor, label = "หลังปรับ")
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                if (moneySaved > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFFFA000))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("คุณลดความสูญเสียได้", fontSize = 14.sp, color = Color.Gray)
                            Text("$moneySaved บาท/ปี", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                } else if (lossAfter == 0) {
                    Text("ยอดเยี่ยม! ไม่มีความเสี่ยงสูญเสียรายได้", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                } else {
                    Text("ยังมีความเสี่ยงสูญเสียรายได้ $lossAfter บาท/ปี", color = Color(0xFFD32F2F), fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // [เพิ่ม] แสดง Body Map ในหน้า Final ด้วย เพื่อให้ครบตาม Requirement
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("จุดเสี่ยงที่พบ", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                if (initialResult != null) {
                    BodyMapVisualization(bodyRisks = initialResult.bodyPartRisks)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- ส่วนแสดงคำแนะนำที่เลือก ---
        if (selectedSuggestions.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbUp, null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "แนวทางที่คุณเลือกปฏิบัติ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    selectedSuggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = suggestion,
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // ปุ่มกลับหน้าหลัก
        Button(
            onClick = {
                navController.navigate("main") {
                    popUpTo("main") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81))
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("กลับสู่หน้าหลัก", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// Reuse Composable
@Composable
fun RiskScoreItem(score: Int, color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "$score", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}