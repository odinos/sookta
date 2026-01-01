package com.kdev.sookta.ui.screen.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    // อ่านค่าจาก Database (State จะเปลี่ยนเมื่อข้อมูลมา)
    val userPref by db.userPreferenceDao().getPreference().collectAsState(initial = null)

    LaunchedEffect(userPref) {
        delay(2000L) // โชว์โลโก้อย่างน้อย 2 วิ

        if (userPref != null && userPref!!.isSetupCompleted) {
            // ถ้าเคยบันทึกแล้ว ไปหน้า Main เลย
            navController.navigate("main") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            // ถ้ายังไม่เคย หรือ Database ว่างเปล่า ไปหน้าเริ่ม (Language หรือ Setup)
            navController.navigate("language_selection") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(Color(0xFFFDF8E1))
    ) {
        Image(
            painter = painterResource(id = R.drawable.sookta_logo), // เรียกไฟล์รูปที่นี่
            contentDescription = "Sookta Logo",
            modifier = Modifier.size(250.dp) // กำหนดขนาดโลโก้ตามต้องการ (ปรับเลขได้)
        )
    }
}