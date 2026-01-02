package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun FinalResultScreen(navController: NavController, oldScore: Int, newScore: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF5C9A81),
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.result_success), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C9A81))
        Text(stringResource(R.string.result_after), color = Color.Gray)

        Spacer(Modifier.height(40.dp))

        // ตารางเปรียบเทียบ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreCard(stringResource(R.string.label_before_improve), oldScore, Color.Red)
            Text(">>>", modifier = Modifier.align(Alignment.CenterVertically), fontSize = 24.sp, color = Color.Gray)
            ScoreCard(stringResource(R.string.label_after_improve), newScore, Color(0xFF2E7D32)) // สีเขียวเข้ม
        }

        Spacer(Modifier.height(60.dp))

        // ปุ่มบันทึก
        Button(
            onClick = {
                // TODO: บันทึกลง Room Database ตรงนี้

                // กลับหน้า Home
                navController.navigate("main") {
                    popUpTo("main") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81))
        ) {
            Text(stringResource(R.string.btn_save_eval))
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = { navController.popBackStack() }) {
            Text(stringResource(R.string.btn_back_edit), color = Color.Gray)
        }
    }
}

@Composable
fun ScoreCard(label: String, score: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(color.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp))
                .border(2.dp, color, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("$score", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}