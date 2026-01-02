package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.kdev.sookta.R

@Composable
fun InitialRiskScreen(navController: NavController, activityName: String, initialScore: Int) {

    val displayActivityName = activityName.toIntOrNull()?.let { stringResource(it) } ?: activityName

    // รายการวิธีแก้ปัญหา (Mock Data ตามประเภทงาน)
    val potentialSolutions = listOf(
        SolutionItem(stringResource(R.string.sol_mech_aid), reductionPoint = 3),
        SolutionItem(stringResource(R.string.sol_ergo), reductionPoint = 2),
        SolutionItem(stringResource(R.string.sol_weight), reductionPoint = 2),
        SolutionItem(stringResource(R.string.sol_rest), reductionPoint = 1)
    )

    // State สำหรับเก็บว่า User เลือกวิธีไหนบ้าง
    val selectedSolutions = remember { mutableStateListOf<SolutionItem>() }

    val riskLabel = when {
        initialScore >= 7 -> stringResource(R.string.risk_high)
        initialScore >= 4 -> stringResource(R.string.risk_medium)
        else -> stringResource(R.string.risk_low)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.initial_risk_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.label_activity_format, displayActivityName), color = Color.Gray)

        Spacer(Modifier.height(20.dp))

        // แสดงคะแนนความเสี่ยงปัจจุบัน (วงกลมสีแดง)
        RiskScoreCircle(score = initialScore, label = riskLabel)

        Spacer(Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), // สีส้มอ่อน
            modifier = Modifier.fillMaxWidth()
        ) {
            PaddingValues(16.dp)
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.suggestion_title), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(stringResource(R.string.suggestion_desc), fontSize = 14.sp, color = Color.DarkGray)

                Spacer(Modifier.height(10.dp))

                // รายการ Checkbox
                potentialSolutions.forEach { solution ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (selectedSolutions.contains(solution)) {
                                    selectedSolutions.remove(solution)
                                } else {
                                    selectedSolutions.add(solution)
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedSolutions.contains(solution),
                            onCheckedChange = { isChecked ->
                                if (isChecked) selectedSolutions.add(solution) else selectedSolutions.remove(solution)
                            }
                        )
                        Text(solution.name)
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = {
                // คำนวณคะแนนที่ลดลง
                val totalReduction = selectedSolutions.sumOf { it.reductionPoint }
                var finalScore = initialScore - totalReduction
                if (finalScore < 1) finalScore = 1 // คะแนนต่ำสุดคือ 1

                // ไปหน้าผลลัพธ์สุดท้าย
                navController.navigate("final_result/$initialScore/$finalScore")
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81)),
            enabled = selectedSolutions.isNotEmpty() // ต้องเลือกวิธีแก้ก่อนถึงกดได้
        ) {
            Text(stringResource(R.string.btn_re_evaluate))
        }
    }
}

// UI วงกลมแสดงคะแนน
@Composable
fun RiskScoreCircle(score: Int, label: String) {
    val color = when {
        score >= 7 -> Color.Red
        score >= 4 -> Color(0xFFFFA000) // ส้ม
        else -> Color.Green
    }

    Box(
        modifier = Modifier
            .size(150.dp)
            .background(Color.White, shape = RoundedCornerShape(100))
            .border(8.dp, color, RoundedCornerShape(100)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$score/10", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

data class SolutionItem(val name: String, val reductionPoint: Int)