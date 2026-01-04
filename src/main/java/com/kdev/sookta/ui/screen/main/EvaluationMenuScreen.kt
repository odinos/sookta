package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.R

data class ActivityItem(
    val nameRes: Int,
    val imageRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationMenuScreen(navController: NavController) {

    val activities = listOf(
        ActivityItem(R.string.job_transplanting, R.drawable.img_transplanting), // ตัวอย่าง: ไฟล์ชื่อ img_transplanting.png
        ActivityItem(R.string.job_fertilizing, R.drawable.img_fertilizing),
        ActivityItem(R.string.job_pesticide, R.drawable.img_pesticide),
        ActivityItem(R.string.job_pruning, R.drawable.img_pruning),
        ActivityItem(R.string.job_harvesting, R.drawable.img_harvesting),
        ActivityItem(R.string.job_transport, R.drawable.img_transport)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.eval_menu_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back_desc),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5C9A81) // สีเขียวธีม Sookta
                )
            )
        },
        containerColor = Color(0xFFFDF8E1) // สีพื้นหลังครีมอ่อน
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.eval_select_desc),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )

            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f), // [สำคัญ 1] ใส่ weight เพื่อให้ Grid ขยายเต็มพื้นที่ที่เหลือ
                contentPadding = PaddingValues(bottom = 32.dp) // [สำคัญ 2] เผื่อที่ด้านล่างไว้ 32dp กันปุ่มโดนบัง
            ) {
                items(activities) { item ->
                    val activityName = stringResource(item.nameRes)
                    // ส่งทั้งชื่อและรูปไปให้ ActivityCard
                    ActivityCard(
                        name = activityName,
                        imageRes = item.imageRes,
                        onClick = {
                            navController.navigate("evaluation_form/$activityName")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityCard(name: String, imageRes: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .height(160.dp) // [ปรับขนาด] เพิ่มความสูงเพื่อให้ใส่รูปได้พอดี (เดิม 100-120dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, // จัดกึ่งกลางแนวนอน
            verticalArrangement = Arrangement.Center // จัดกึ่งกลางแนวตั้ง
        ) {
            // ส่วนรูปภาพ
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                contentScale = ContentScale.Fit, // ปรับรูปให้พอดีไม่โดนตัด (หรือใช้ Crop ถ้าอยากให้เต็ม)
                modifier = Modifier
                    .size(80.dp) // กำหนดขนาดรูป
                    .padding(bottom = 8.dp)
            )

            // ส่วนชื่อกิจกรรม
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF2E7D32),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp, // จัดระยะบรรทัดกรณีชื่อยาว 2 บรรทัด
                maxLines = 2
            )
        }
    }
}