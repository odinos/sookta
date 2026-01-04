package com.kdev.sookta.ui.screen.main

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.data.AppDatabase
import java.io.File
import androidx.compose.ui.res.stringResource
import com.kdev.sookta.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    // ดึงข้อมูล User
    val userPref by db.userPreferenceDao().getPreference().collectAsState(initial = null)

    Scaffold(
        containerColor = Color(0xFFFDF8E1) // สีพื้นหลังครีมอ่อน
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. ส่วนหัว (Header) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                // พื้นหลังสีเขียวโค้ง
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(Color(0xFF5C9A81))
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.height(24.dp))

                    // รูปโปรไฟล์
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(4.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {

                        val avatarPath = userPref?.avatarPath


                        if (avatarPath != null) {

                            if (avatarPath.startsWith("res:")) {
                                // [กรณี 1] เป็นรูปการ์ตูน (Resource ID)
                                val resId = avatarPath.removePrefix("res:").toIntOrNull()
                                if (resId != null) {
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                val imgFile = File(avatarPath)
                                if (imgFile.exists()) {
                                    val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Profile Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(60.dp))
                                }
                            }
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(60.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ชื่อผู้ใช้งาน
                    Text(
                        text = userPref?.userName ?: stringResource(R.string.default_username),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            // --- 2. ส่วนข้อมูลสุขภาพ (Stats) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(
                    title = stringResource(R.string.stat_age),
                    value = userPref?.age ?: "-",
                    unit = stringResource(R.string.unit_year),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                StatCard(
                    title = stringResource(R.string.stat_weight),
                    value = userPref?.weight ?: "-",
                    unit = stringResource(R.string.unit_kg),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                StatCard(
                    title = stringResource(R.string.stat_height),
                    value = userPref?.height ?: "-",
                    unit = stringResource(R.string.unit_cm),
                    modifier = Modifier.weight(1f)
                )

            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                StatCard(
                    title = "รายได้ต่อปี",
                    value = userPref?.incomePerYear ?: "-",
                    unit = "บาท",
                    modifier = Modifier.fillMaxWidth() // ให้กว้างเต็มจอหรือปรับตามความสวยงาม
                )
            }
            Spacer(Modifier.height(30.dp))

            // --- 3. เมนูการตั้งค่า (Menu List) ---
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // 1. จัดการโปรไฟล์ (แก้ไขข้อมูล)
                ProfileMenuItem(
                    icon = Icons.Default.Edit,
                    text = stringResource(R.string.profile_edit),
                    onClick = { navController.navigate("setup") } // ไปหน้า Setup
                )

                // 2. เปลี่ยนภาษา
                ProfileMenuItem(
                    icon = Icons.Default.Language,
                    text = stringResource(R.string.menu_language),
                    onClick = { navController.navigate("language_selection") } // ไปหน้าเลือกภาษา
                )

                // 3. ข้อกำหนดและเงื่อนไข
                ProfileMenuItem(
                    icon = Icons.Default.Description,
                    text = stringResource(R.string.menu_terms),
                    onClick = { navController.navigate("terms") }
                )

                // 4. ความช่วยเหลือ และคำแนะนำการใช้งาน
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    text = stringResource(R.string.menu_help),
                    onClick = { navController.navigate("help") }
                )

                // 5. ติดต่อเรา
                ProfileMenuItem(
                    icon = Icons.Default.Call,
                    text = stringResource(R.string.menu_contact),
                    onClick = { navController.navigate("contact") }
                )
                // 6. ออกจากระบบ (Exit App)
                ProfileMenuItem(
                    icon = Icons.Default.Close, // ใช้ไอคอนกากบาท
                    text = stringResource(R.string.logout), // "ออกจากระบบ"
                    onClick = {
                        // ปิด Activity ปัจจุบันเพื่อออกจากแอป
                        (context as? android.app.Activity)?.finish()
                    }
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// --- Component ย่อย ---

@Composable
fun StatCard(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, fontSize = 12.sp, color = Color.Gray)
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C9A81))
            Text(text = unit, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon ด้านหน้า
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF5C9A81),
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(16.dp))

            // ข้อความเมนู
            Text(
                text = text,
                fontSize = 16.sp,
                color = Color.DarkGray,
                modifier = Modifier.weight(1f)
            )

            // Icon ลูกศรด้านหลัง
            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}