package com.kdev.sookta.ui.screen.onboarding

import android.app.Activity
import android.content.Context
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.R
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.data.UserPreference
import com.kdev.sookta.utils.LocaleHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // ใช้ remember เพื่อไม่ให้สร้าง DB ใหม่ทุกครั้งที่ recompose
    val db = remember { AppDatabase.getDatabase(context) }

    val isEditMode = navController.previousBackStackEntry != null

    Scaffold(
        topBar = {
            if (isEditMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.change_language), color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5C9A81))
                )
            }
        },
        containerColor = Color(0xFFFDF8E1)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.sookta_logo),
                contentDescription = "Sookta Logo",
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = stringResource(R.string.welcome_to),
                fontSize = 20.sp,
                color = Color.Gray
            )

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C9A81)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.select_lang_desc),
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(40.dp))

            // เลือกภาษาไทย
            LanguageOptionCard(
                title = stringResource(R.string.lang_thai),
                subtitle = "ภาษาไทย",
                flagEmoji = "🇹🇭",
                onClick = {
                    scope.launch {
                        saveLanguageAndNavigate(db, "TH", navController, isEditMode, context)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // เลือกภาษาอังกฤษ
            LanguageOptionCard(
                title = stringResource(R.string.lang_eng),
                subtitle = "English",
                flagEmoji = "🇬🇧",
                onClick = {
                    scope.launch {
                        saveLanguageAndNavigate(db, "EN", navController, isEditMode, context)
                    }
                }
            )
        }
    }
}

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
            Text(text = flagEmoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(20.dp))
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
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF5C9A81).copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

suspend fun saveLanguageAndNavigate(
    db: AppDatabase,
    langDB: String, // "TH" หรือ "EN" (สำหรับเก็บลง DB)
    navController: NavController,
    isEditMode: Boolean,
    context: Context
) {
    // แปลงเป็นรหัส locale สำหรับ LocaleHelper ("th" หรือ "en")
    val localeCode = if (langDB.equals("TH", ignoreCase = true)) "th" else "en"

    // 1. บันทึกลง SharedPreferences ทันที
    LocaleHelper.setLanguage(context, localeCode)

    val dao = db.userPreferenceDao()

    try {
        if (isEditMode) {
            // โหมดแก้ไข: อัปเดต DB แล้วรีสตาร์ท Activity เพื่อเปลี่ยนภาษา
            dao.updateLanguage(langDB)
            if (context is Activity) {
                context.recreate()
            }
        } else {
            // โหมดผู้ใช้ใหม่: ใช้ insertOrUpdate หรือ try-catch กันแอปเด้ง
            // เนื่องจากเราไม่เห็นโค้ด DAO ผมเลยใช้ logic ง่ายๆ คือเช็คก่อนว่ามีไหม (ถ้า DAO ไม่มี insertOrUpdate)
            // หรือวิธีที่ปลอดภัยที่สุดคือ try-catch การ insert ครับ
            try {
                dao.insertPreference(UserPreference(id = 1, language = langDB))
            } catch (e: Exception) {
                // ถ้า insert ไม่ได้ (เพราะมี id=1 อยู่แล้ว) ให้ update แทน
                dao.updateLanguage(langDB)
            }

            // บังคับเปลี่ยน Locale ของ Context ปัจจุบันก่อนไปหน้าถัดไป
            LocaleHelper.updateContextLocale(context, localeCode)

            // ไปหน้าถัดไป
            navController.navigate("avatar_selection") {
                popUpTo("language_selection") { inclusive = true }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        // กรณีเกิด Error จริงๆ ก็ยังให้เปลี่ยนหน้าไปได้ เพื่อไม่ให้ user ติดอยู่หน้านี้
        navController.navigate("avatar_selection") {
            popUpTo("language_selection") { inclusive = true }
        }
    }
}