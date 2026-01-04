package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.R
import com.kdev.sookta.utils.TextToSpeechManager

// Data Class
data class TermsItem(val title: String, val content: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(navController: NavController) {
    val context = LocalContext.current

    // 1. TTS Manager
    val ttsManager = remember { TextToSpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    // 2. ดึงข้อมูลจาก String Array (เสถียรกว่าการ Parse HTML)
    val rawArray = stringArrayResource(id = R.array.terms_list_data)
    val termsList = remember(rawArray) {
        rawArray.map { itemString ->
            // แยก Title กับ Content ด้วยเครื่องหมาย "|"
            val parts = itemString.split("|")
            if (parts.size >= 2) {
                TermsItem(title = parts[0].trim(), content = parts[1].trim())
            } else {
                TermsItem(title = "", content = itemString) // Fallback
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.terms_title), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5C9A81))
            )
        },
        containerColor = Color(0xFFFDF8E1)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header
            Spacer(Modifier.height(16.dp))
            Image(
                painter = painterResource(id = R.drawable.logo_app),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.terms_read_instruction),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF2E7D32)
            )
            Spacer(Modifier.height(16.dp))

            // 3. แสดงรายการด้วย LazyColumn
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(termsList) { item ->
                    TermsItemCard(item, ttsManager)
                }
            }
        }
    }
}

@Composable
fun TermsItemCard(item: TermsItem, ttsManager: TextToSpeechManager) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // หัวข้อ
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )

                // ปุ่ม TTS
                IconButton(
                    onClick = {
                        ttsManager.speak("${item.title} ${item.content}")
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Listen",
                        tint = Color(0xFF5C9A81)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(Modifier.height(8.dp))

            // เนื้อหา
            Text(
                text = item.content,
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 22.sp
            )
        }
    }
}