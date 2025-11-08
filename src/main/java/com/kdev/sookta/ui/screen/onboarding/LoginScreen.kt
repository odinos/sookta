package com.kdev.sookta.ui.screen.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun LoginScreen(navController: NavController) {
    var phoneNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login Screen")
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("กรุณาระบุหมายเลขโทรศัพท์") }
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.navigate("otp") }) {
            Text("เข้าสู่ระบบด้วยหมายเลขโทรศัพท์")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { navController.navigate("avatar_selection") }) {
            Text("ใช้งานโดยไม่ลงทะเบียน")
        }
    }
}
