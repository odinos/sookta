package com.kdev.sookta.ui.screen.main

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.ui.component.AppBackground
import java.io.File
import com.kdev.sookta.ui.component.rememberTextToSpeech
import com.kdev.sookta.ui.component.SpeakButton
import androidx.compose.ui.res.stringResource // ✅ อย่าลืม import
import com.kdev.sookta.R
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val tts = rememberTextToSpeech()


    val userPref by db.userPreferenceDao().getPreference().collectAsState(initial = null)
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Image
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    if (userPref?.avatarPath != null) {
                        val imgFile = File(userPref!!.avatarPath!!)
                        if (imgFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp).fillMaxSize(),
                                tint = Color.Gray
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp).fillMaxSize(),
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Greeting Text
                Column {
                    Text(
                        text = stringResource(R.string.home_hello),
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = userPref?.userName ?: stringResource(R.string.home_user_mock),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C9A81)
                    )
                    val welcomeSpeech = stringResource(R.string.welcome_speech, userPref?.userName ?: "")
                    SpeakButton(
                        textToSpeak = (welcomeSpeech), ttsManager = tts, modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.home_start_eval),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- ส่วนเมนู (Grid Menu) ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    HomeMenuCard(
                        title = stringResource(R.string.menu_eval_risk),
                        icon = Icons.Default.Info, // แก้เป็น Info ซึ่งมีแน่นอน
                        color = Color(0xFFE8F5E9),
                        onClick = { navController.navigate("evaluation_menu") }
                    )
                }
                item {
                    HomeMenuCard(
                        title = stringResource(R.string.menu_exercise),
                        icon = Icons.Default.AccessibilityNew,
                        color = Color(0xFFE1F5FE),
                        onClick = { /* Navigate to Exercise */ }
                    )
                }
                item {
                    HomeMenuCard(
                        title = stringResource(R.string.menu_knowledge),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        color = Color(0xFFFFF3E0),
                        onClick = { /* Navigate to Knowledge */ }
                    )
                }

            }
        }
    }
}

// --- Component ย่อย: การ์ดเมนู ---
@Composable
fun HomeMenuCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .height(150.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

// ลบ Helper function ด้านล่างทิ้งไปได้เลยครับ เพราะเราเปลี่ยนไปใช้ Icons.Default.Info แล้ว