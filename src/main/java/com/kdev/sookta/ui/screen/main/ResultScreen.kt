package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ResultScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Result Screen")
        Text("จุดเสี่ยง: หลังล่าง, คะแนน: 9")
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            // Go back to the home screen in the main flow
            navController.popBackStack("home", inclusive = false)
        }) {
            Text("กลับสู่หน้าหลัก")
        }
    }
}
