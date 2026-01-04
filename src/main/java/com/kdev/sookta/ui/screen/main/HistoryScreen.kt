package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.kdev.sookta.R
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.data.EvaluationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    var historyList by remember { mutableStateOf(emptyList<EvaluationEntity>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db = AppDatabase.getDatabase(context)
        historyList = db.evaluationDao().getAllHistory()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF8E1))
    ) {
        // --- Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Color(0xFF5C9A81))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.history_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(stringResource(R.string.history_subtitle), fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }
                IconButton(onClick = { /* TODO */ }, modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- List ---
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF5C9A81)) }
        } else if (historyList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Text("ยังไม่มีประวัติการประเมิน", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyList) { item ->
                    // [แก้ไข 1] ส่ง Lambda onClick ไปให้ HistoryCard
                    HistoryCard(item) {
                        // ไปหน้า ResultHistoryScreen พร้อมส่ง ID
                        navController.navigate("result_history/${item.id}")
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// [แก้ไข 2] รับพารามิเตอร์ onClick: () -> Unit
@Composable
fun HistoryCard(item: EvaluationEntity, onClick: () -> Unit) {
    val formattedDate = remember(item.dateTimestamp) {
        val sdf = SimpleDateFormat("dd MMM yy HH:mm", Locale.forLanguageTag("th"))
        sdf.format(Date(item.dateTimestamp))
    }

    val (statusColor, statusText) = when (item.riskAfter) {
        "HIGH" -> Color(0xFFFF5252) to stringResource(R.string.risk_high)
        "MEDIUM" -> Color(0xFFFFA726) to stringResource(R.string.risk_medium)
        else -> Color(0xFF66BB6A) to stringResource(R.string.risk_low)
    }

    Card(
        onClick = onClick, // [แก้ไข 3] ใส่ onClick ให้ Card (Card ของ Material3 รองรับ onClick โดยตรง)
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).background(Color(0xFFF5F5F5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Assignment, null, tint = Color(0xFF5C9A81))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.activityName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF333333), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formattedDate, fontSize = 12.sp, color = Color.Gray)

                if (item.scoreBefore > item.scoreAfter) {
                    val improvement = ((item.scoreBefore - item.scoreAfter) / item.scoreBefore * 100).toInt()
                    Text("ความเสี่ยงลดลง $improvement%", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(String.format("%.2f", item.scoreAfter), fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}