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
import java.util.Locale

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

// --- ส่วนที่แก้ไขสำคัญ ---
@Suppress("DEPRECATION") // จำเป็นต้องใช้ updateConfiguration เพื่อให้เปลี่ยนภาษาทันทีโดยไม่ Recreate ในรอบแรก
suspend fun saveLanguageAndNavigate(
    db: AppDatabase,
    langDB: String, // "TH" หรือ "EN"
    navController: NavController,
    isEditMode: Boolean,
    context: Context
) {
    // 1. แปลงรหัสภาษา
    val localeCode = if (langDB.equals("TH", ignoreCase = true)) "th" else "en"

    // 2. บันทึกลง SharedPreferences
    LocaleHelper.setLanguage(context, localeCode)

    // 3. ตั้งค่า Locale ให้ JVM
    val locale = Locale.forLanguageTag(localeCode)
    Locale.setDefault(locale)

    val dao = db.userPreferenceDao()

    try {
        if (isEditMode) {
            // --- กรณีแก้ไข (มาจากหน้า Profile) ---
            dao.updateLanguage(langDB)

            // ใช้ recreate() เพื่อรีโหลดแอพใหม่ให้ภาษาเปลี่ยนทุกหน้า
            if (context is Activity) {
                context.recreate()
            }
        } else {
            // --- กรณีติดตั้งใหม่ (First Run) ---
            try {
                dao.insertOrUpdate(UserPreference(id = 1, language = langDB))
            } catch (e: Exception) {
                dao.updateLanguage(langDB)
            }

            // [FIX] บังคับอัปเดต Configuration ทันที เพื่อให้หน้าถัดไป (Setup) เป็นภาษาใหม่
            // โดยไม่ต้องสั่ง recreate() ซึ่งจะทำให้หลุดไปหน้า Splash
            val resources = context.resources
            val configuration = resources.configuration
            configuration.setLocale(locale)
            resources.updateConfiguration(configuration, resources.displayMetrics)

            // [FIX] สั่ง Navigate ไปหน้า Setup โดยตรง
            navController.navigate("setup") {
                // ลบหน้าเลือกภาษาออกจาก Stack เพื่อไม่ให้กด Back กลับมาได้
                popUpTo("language_selection") { inclusive = true }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        navController.popBackStack()
    }
}
