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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Data Class จำลองข้อมูลประวัติ (ในอนาคตจะดึงจาก Room Database)
data class HistoryItem(
    val id: String,
    val activityName: String,
    val date: String,
    val riskScore: Int, // คะแนน 1-10
    val riskLevel: String // "Low", "Medium", "High"
)

@Composable
fun HistoryScreen(navController: NavController) {
    // ข้อมูลตัวอย่าง (Mock Data)
    val historyList = listOf(
        HistoryItem("1", "งานยกของหนัก", "12 ม.ค. 67", 8, "High"),
        HistoryItem("2", "งานหน้าคอมพิวเตอร์", "10 ม.ค. 67", 4, "Medium"),
        HistoryItem("3", "งานเชื่อมโลหะ", "05 ม.ค. 67", 2, "Low"),
        HistoryItem("4", "งานบนที่สูง", "28 ธ.ค. 66", 9, "High")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF8E1)) // พื้นหลังสีครีม
    ) {
        // --- 1. ส่วนหัว (Header) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Color(0xFF5C9A81)) // สีเขียวธีม
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ผลตรวจย้อนหลัง",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "ประวัติการประเมินความเสี่ยงของคุณ",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // ปุ่มค้นหา (Optional)
                IconButton(
                    onClick = { print("search") },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. รายการประวัติ (List) ---
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(historyList) { item ->
                HistoryCard(item)
            }
            // พื้นที่ว่างด้านล่างเผื่อ BottomBar
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun HistoryCard(item: HistoryItem) {
    // กำหนดสีตามระดับความเสี่ยง
    val (statusColor, statusText) = when (item.riskLevel) {
        "High" -> Color(0xFFFF5252) to "เสี่ยงสูง" // แดง
        "Medium" -> Color(0xFFFFA726) to "ปานกลาง" // ส้ม
        else -> Color(0xFF66BB6A) to "ปกติ" // เขียว
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon กิจกรรม
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFFF5F5F5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = null,
                    tint = Color(0xFF5C9A81)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // รายละเอียด
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.activityName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF333333),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.date,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Badge สถานะความเสี่ยง
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.riskScore}/10",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}