package com.kdev.sookta.ui.screen.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun OTPScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("OTP Screen")
        Spacer(Modifier.height(20.dp))
        Text("กรุณากรอกรหัส OTP 4 หลัก")
        // Add OTP input field here
        Spacer(Modifier.height(20.dp))
        Button(onClick = { navController.navigate("avatar_selection") }) {
            Text("ยืนยันเพื่อเข้าระบบ")
        }
    }
}
