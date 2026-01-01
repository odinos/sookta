package com.kdev.sookta.ui.screen.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import com.kdev.sookta.R
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.data.UserPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // ตรวจสอบว่ามีหน้าก่อนหน้านี้ไหม? (ถ้ามี = มาจาก Profile, ถ้าไม่มี = มาจาก Splash/Userใหม่)
    val isEditMode = navController.previousBackStackEntry != null

    Scaffold(
        topBar = {
            // แสดงปุ่ม Back เฉพาะตอนที่เข้ามาแก้ไข (Edit Mode)
            if (isEditMode) {
                TopAppBar(
                    title = { Text("เปลี่ยนภาษา", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5C9A81))
                )
            }
        },
        containerColor = Color(0xFFFDF8E1) // สีพื้นหลังครีม
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Logo App (แทนที่ Text เดิม)
            Image(
                painter = painterResource(id = R.drawable.sookta_logo),
                contentDescription = "Sookta Logo",
                modifier = Modifier
                    .size(200.dp) // ปรับขนาดโลโก้
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "ยินดีต้อนรับสู่",
                fontSize = 20.sp,
                color = Color.Gray
            )

            Text(
                text = "SOOK-TA",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C9A81) // สีเขียวธีม
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "กรุณาเลือกภาษาเพื่อเริ่มต้นใช้งาน",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 2. ตัวเลือกภาษาแบบ Card สวยงาม
            LanguageOptionCard(
                title = "ภาษาไทย",
                subtitle = "Thai",
                flagEmoji = "🇹🇭", // ใส่ Emoji ธงชาติเพิ่มความสวยงาม (หรือจะเอาออกก็ได้)
                onClick = {
                    scope.launch {
                        saveLanguageAndNavigate(db, "TH", navController, isEditMode)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LanguageOptionCard(
                title = "English",
                subtitle = "อังกฤษ",
                flagEmoji = "🇬🇧",
                onClick = {
                    scope.launch {
                        saveLanguageAndNavigate(db, "EN", navController, isEditMode)
                    }
                }
            )
        }
    }
}

// Component การ์ดตัวเลือกภาษา
@Composable
fun LanguageOptionCard(
    title: String,
    subtitle: String,
    flagEmoji: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ส่วนธงชาติ (Emoji text)
            Text(text = flagEmoji, fontSize = 32.sp)

            Spacer(modifier = Modifier.width(20.dp))

            // ส่วนข้อความ
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Icon ลูกศรหรือเครื่องหมายถูก
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF5C9A81).copy(alpha = 0.3f), // สีจางๆ ตกแต่ง
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ฟังก์ชันบันทึกและเปลี่ยนหน้า (รองรับทั้ง User ใหม่ และ Edit Mode)
suspend fun saveLanguageAndNavigate(
    db: AppDatabase,
    lang: String,
    navController: NavController,
    isEditMode: Boolean
) {
    val dao = db.userPreferenceDao()

    if (isEditMode) {
        // กรณี: มาจากหน้า Profile -> แค่อัปเดตภาษาแล้วกลับไป
        dao.updateLanguage(lang)
        navController.popBackStack()
    } else {
        // กรณี: เริ่มต้นใช้งานครั้งแรก -> สร้างข้อมูลใหม่ แล้วไปหน้า Setup
        // (Insert แบบ REPLACE จะล้างข้อมูลเก่า เหมาะสำหรับ User ใหม่)
        dao.insertPreference(UserPreference(id = 1, language = lang))
        navController.navigate("setup")
    }
}