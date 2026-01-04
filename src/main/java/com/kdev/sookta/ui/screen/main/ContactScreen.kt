package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.contact_title), color = Color.White, fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // 1. โลโก้แอป (หรือโลโก้มหาวิทยาลัยถ้ามี)
            Image(
                painter = painterResource(id = R.drawable.logo_app),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(16.dp)
            )

            Spacer(Modifier.height(24.dp))

            // 2. ข้อความเกริ่นนำ
            Text(
                text = stringResource(R.string.contact_subtitle),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(32.dp))

            // 3. Card ข้อมูลทีมวิจัย (Highlight)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // หัวข้อผู้รับผิดชอบ
                    Text(
                        text = stringResource(R.string.contact_team_label),
                        fontSize = 14.sp,
                        color = Color(0xFF5C9A81),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    // ชื่อทีมวิจัย (ตัวใหญ่)
                    Text(
                        text = stringResource(R.string.contact_team_name), // "ทีมวิจัยระดับปริญญาเอก"
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    // มหาวิทยาลัยธรรมศาสตร์
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.contact_university), // "มหาวิทยาลัยธรรมศาสตร์"
                            fontSize = 16.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // คณะ (ถ้าต้องการแสดง)
                    Text(
                        text = stringResource(R.string.contact_faculty),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(24.dp))

                    // ข้อมูลติดต่อ (Email / Phone)
                    ContactRowItem(
                        icon = Icons.Default.Email,
                        label = stringResource(R.string.contact_label_email),
                        value = stringResource(R.string.contact_email_value)
                    )

                    Spacer(Modifier.height(16.dp))

                    ContactRowItem(
                        icon = Icons.Default.Phone,
                        label = stringResource(R.string.contact_label_phone),
                        value = stringResource(R.string.contact_phone_value)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Footer แสดง Version
            Text(
                text = stringResource(R.string.app_version, "1.0.2"),
                fontSize = 12.sp,
                color = Color.LightGray
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// --- Helper Composable สำหรับแถวข้อมูลติดต่อ ---
@Composable
fun ContactRowItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon ในวงกลมสีเขียวอ่อน
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color(0xFFE8F5E9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF2E7D32))
        }

        Spacer(Modifier.width(16.dp))

        Column {
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}