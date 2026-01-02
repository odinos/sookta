package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.model.ErgoResult
import com.kdev.sookta.model.RiskLevel

@Composable
fun FinalResultScreen(navController: NavController, oldScoreArg: Int, newScoreArg: Int) {
    // 1. พยายามดึงข้อมูล Object เต็มๆ จาก SavedStateHandle ก่อน
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val initialResult = savedStateHandle?.get<ErgoResult>("initialResult")
    val finalResult = savedStateHandle?.get<ErgoResult>("finalResult")

    // Fallback: ถ้าไม่มี Object ให้สร้าง Dummy จากตัวเลขที่ส่งมาทาง Route (กัน Crash)
    val beforeScore = initialResult?.score ?: oldScoreArg.toDouble()
    val afterScore = finalResult?.score ?: newScoreArg.toDouble()

    // คำนวณ % การลดลง (Improvement)
    val improvement = if (beforeScore > 0) ((beforeScore - afterScore) / beforeScore) * 100 else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF8E1)) // สีพื้นหลังครีม
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "บันทึกผลการประเมินสำเร็จ!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        Text(
            text = "ระบบได้บันทึกข้อมูลของคุณเรียบร้อยแล้ว",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(32.dp))

        // --- ส่วนเปรียบเทียบ (Comparison Card) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("สรุปผลการปรับปรุง (Summary)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Before
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ก่อนปรับ", fontSize = 14.sp, color = Color.Gray)
                        Text(
                            text = String.format("%.2f", beforeScore),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE57373) // แดงอ่อน
                        )
                        // ถ้ามีข้อมูล RiskLevel ให้โชว์ด้วย
                        initialResult?.let {
                            RiskTag(it.riskLevel)
                        }
                    }

                    // Arrow
                    Text("➝", fontSize = 32.sp, color = Color.Gray)

                    // After
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("หลังปรับ", fontSize = 14.sp, color = Color.Gray)
                        Text(
                            text = String.format("%.2f", afterScore),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50) // เขียว
                        )
                        finalResult?.let {
                            RiskTag(it.riskLevel)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                if (improvement > 0) {
                    Text(
                        text = "ความเสี่ยงลดลง ${String.format("%.1f", improvement)}%",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Text(
                        text = "ความเสี่ยงเท่าเดิม หรือเพิ่มขึ้น",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f)) // ดันปุ่มไปล่างสุด

        // ปุ่มกลับหน้าหลัก
        Button(
            onClick = {
                // เคลียร์ BackStack ทั้งหมดแล้วกลับไปหน้า Menu หรือ Main
                navController.navigate("main") {
                    popUpTo("main") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81))
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("กลับสู่หน้าหลัก", fontSize = 16.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// Helper Composable เล็กๆ สำหรับแสดงป้ายสีระดับความเสี่ยง
@Composable
fun RiskTag(level: RiskLevel) {
    Surface(
        color = Color(level.colorHex),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Text(
            text = level.label,
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}