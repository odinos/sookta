package com.kdev.sookta.ui.screen.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.res.stringResource
import com.kdev.sookta.R
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.data.EvaluationEntity
import com.kdev.sookta.model.ErgoResult
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun FinalResultScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. รับค่า Object จากหน้า InitialRiskScreen
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val initialResult = savedStateHandle?.get<ErgoResult>("initialResult")
    val finalResult = savedStateHandle?.get<ErgoResult>("finalResult")

    // รับชื่อกิจกรรมที่ส่งต่อมา (ถ้ามีเก็บไว้ใน savedState หรือ arguments)
    // ถ้าไม่มีให้ Default เป็น "การยกของทั่วไป"
    val activityName = "Assessment Result"

    // ป้องกันกรณีไม่มีข้อมูล (Null Safety)
    if (initialResult == null || finalResult == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ไม่พบข้อมูลผลลัพธ์ โปรดลองใหม่อีกครั้ง")
            Button(onClick = { navController.popBackStack() }) { Text("กลับ") }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(30.dp))

        // Icon Success
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF5C9A81),
            modifier = Modifier.size(100.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(stringResource(R.string.result_success), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C9A81))
        Text(stringResource(R.string.result_after), color = Color.Gray, fontSize = 16.sp)

        Spacer(Modifier.height(40.dp))

        // --- ตารางเปรียบเทียบ Before vs After ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Before
            ResultScoreCard(
                label = stringResource(R.string.label_before_improve),
                result = initialResult,
                isBefore = true
            )

            // Arrow
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(">>>", fontSize = 24.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                // คำนวณ % การลดลง
                val improvement = calculateImprovement(initialResult.score, finalResult.score)
                Text("ลดลง ${improvement}%", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }

            // After
            ResultScoreCard(
                label = stringResource(R.string.label_after_improve),
                result = finalResult,
                isBefore = false
            )
        }

        Spacer(Modifier.height(20.dp))

        // การ์ดสรุปคำแนะนำ
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("สรุปผล:", fontWeight = FontWeight.Bold, color = Color(0xFF33691E))
                Text("ความเสี่ยงลดลงจากระดับ '${initialResult.riskLevel.label}' เป็น '${finalResult.riskLevel.label}'")
                Spacer(Modifier.height(4.dp))
                Text("ข้อแนะนำ: ${finalResult.suggestion}", fontSize = 14.sp, color = Color.DarkGray)
            }
        }

        Spacer(Modifier.height(50.dp))

        // --- ปุ่มบันทึก (Save Logic) ---
        Button(
            onClick = {
                scope.launch {
                    // 1. สร้าง Object สำหรับบันทึก
                    val record = EvaluationEntity(
                        activityName = activityName,
                        dateTimestamp = Date().time,
                        scoreBefore = initialResult.score,
                        riskBefore = initialResult.riskLevel.name,
                        scoreAfter = finalResult.score,
                        riskAfter = finalResult.riskLevel.name,
                        improvementNote = "ปรับปรุงตามคำแนะนำ"
                    )

                    // 2. เรียก Database DAO (ตัวอย่าง Code)
                    val db = AppDatabase.getDatabase(context)
                    db.evaluationDao().insertEvaluation(record)

                    Toast.makeText(context, R.string.save_data_completed, Toast.LENGTH_SHORT).show()

                    // 3. กลับหน้า Home และเคลียร์ Stack
                    navController.navigate("main") { // เปลี่ยน 'main_screen' ตาม Route ของหน้า Home คุณ
                        popUpTo(0) { inclusive = true } // เคลียร์ทุกหน้าทิ้ง
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_save_eval), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        // ปุ่มไม่บันทึก (กลับหน้าแรกเลย)
        TextButton(onClick = {
            navController.navigate("main_screen") { popUpTo(0) }
        }) {
            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("กลับหน้าหลัก (ไม่บันทึก)", color = Color.Gray)
        }
    }
}

// Composable สำหรับแสดงการ์ดคะแนน
@Composable
fun ResultScoreCard(label: String, result: ErgoResult, isBefore: Boolean) {
    val color = Color(result.riskLevel.colorHex) // ใช้สีจริงจาก Enum RiskLevel

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .background(color.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp))
                .border(if(isBefore) 0.dp else 2.dp, color, shape = RoundedCornerShape(20.dp)), // After มีขอบเด่นๆ
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2f", result.score),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = result.riskLevel.label,
                    fontSize = 10.sp,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ฟังก์ชันคำนวณ % การลดลง
fun calculateImprovement(before: Double, after: Double): Int {
    if (before == 0.0) return 0
    val diff = before - after
    return ((diff / before) * 100).toInt().coerceAtLeast(0)
}