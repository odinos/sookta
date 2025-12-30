package com.kdev.sookta.ui.screen.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.data.UserPreference
import kotlinx.coroutines.launch

@Composable
fun LanguageSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // พื้นหลังสีครีมอ่อนๆ ตามสไตล์ Sookta
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF8E1)) // สีพื้นหลัง
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo หรือ Image (ถ้ามี resource รูป logo ใส่ที่นี่)
        // Image(painter = painterResource(id = R.drawable.logo), contentDescription = null)

        Text(
            text = "Welcome to",
            fontSize = 28.sp,
            color = Color(0xFF5C9A81) // สีเขียวธีม
        )
        Text(
            text = "SOOK-TA",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5C9A81)
        )

        Spacer(modifier = Modifier.height(60.dp))

        Text("Select Language / เลือกภาษา", color = Color.Gray)

        Spacer(modifier = Modifier.height(20.dp))

        // ปุ่มภาษาไทย
        LanguageButton(text = "ภาษาไทย") {
            scope.launch {
                saveLanguageAndNavigate(db, "TH", navController)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ปุ่มภาษาอังกฤษ
        LanguageButton(text = "English") {
            scope.launch {
                saveLanguageAndNavigate(db, "EN", navController)
            }
        }
    }
}

@Composable
fun LanguageButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(55.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8CC63F)), // สีเขียวอ่อน
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

suspend fun saveLanguageAndNavigate(db: AppDatabase, lang: String, navController: NavController) {
    // บันทึกภาษาและสร้าง Row ใหม่ถ้ายังไม่มี
    val dao = db.userPreferenceDao()
    dao.insertPreference(UserPreference(id = 1, language = lang))
    dao.updateLanguage(lang)

    // ไปหน้า Setup (กรอกข้อมูล)
    navController.navigate("setup")
}