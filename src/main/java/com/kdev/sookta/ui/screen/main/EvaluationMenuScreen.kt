package com.kdev.sookta.ui.screen.main

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.kdev.sookta.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationMenuScreen(navController: NavController) {

    val activities = listOf(
        R.string.job_transplanting,
        R.string.job_fertilizing,
        R.string.job_pesticide,
        R.string.job_pruning,
        R.string.job_harvesting,
        R.string.job_transport
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
                items(activities) { activity ->
                    ActivityCard(name = activity) {
                        // ส่งชื่อกิจกรรมไปหน้า Form
                        navController.navigate("evaluation_form/$activity")
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityCard(name: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .height(120.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(name),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp, // ปรับขนาดตัวอักษรให้ใหญ่ขึ้นนิดนึงให้อ่านง่าย
                color = Color(0xFF2E7D32),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}