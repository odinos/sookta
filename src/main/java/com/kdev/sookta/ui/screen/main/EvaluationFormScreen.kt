package com.kdev.sookta.ui.screen.main

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
// Import Helper ตัวใหม่
import com.kdev.sookta.utils.rememberPermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationFormScreen(navController: NavController, activityName: String) {
    val context = LocalContext.current

    // State เก็บรูปภาพ
    val selectedBitmaps = remember { mutableStateListOf<Bitmap>() }

    // State ข้อมูลอื่นๆ
    var selectedDuration by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf("") }
    var selectedWeight by remember { mutableStateOf("0 กก. (ไม่ได้ยก/แบก)") }

    // Dropdown States
    var expandedDuration by remember { mutableStateOf(false) }
    var expandedFrequency by remember { mutableStateOf(false) }
    var expandedWeight by remember { mutableStateOf(false) }

    // ตัวแปรบอกว่าตอนนี้เรากำลังจะเติมรูปใส่ช่องไหน (Slot Index)
    var activeSlotIndex by remember { mutableStateOf(-1) }

    // --- เรียกใช้ PermissionHelper ตัวใหม่ (สั้นและง่าย) ---
    val permissionHelper = rememberPermissionHelper(
        onCameraCapture = { bitmap ->
            // Logic เมื่อถ่ายรูปเสร็จ (bitmap)
            if (selectedBitmaps.size < 4) {
                selectedBitmaps.add(bitmap)
            }
        },
        onGallerySelection = { uri ->
            // Logic เมื่อเลือกรูปเสร็จ (Uri) -> แปลงเป็น Bitmap
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }

            // จัดการว่าจะเอา Bitmap ไปใส่ตรงไหน
            if (activeSlotIndex in 0 until 4) {
                // กรณีกดจากช่องว่าง (Image Slot)
                if (activeSlotIndex < selectedBitmaps.size) {
                    selectedBitmaps[activeSlotIndex] = bitmap // แทนที่
                } else {
                    selectedBitmaps.add(bitmap) // เพิ่มใหม่
                }
                activeSlotIndex = -1 // Reset
            } else if (selectedBitmaps.size < 4) {
                // กรณีกดปุ่ม "อัลบั้ม" ด้านล่าง
                selectedBitmaps.add(bitmap)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("แบบฟอร์มประเมิน", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5C9A81))
            )
        },
        containerColor = Color(0xFFFDF8E1)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "กิจกรรม: $activityName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(16.dp))

            // --- ส่วนแสดงรูปภาพ (Grid) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "รูปภาพประกอบ (${selectedBitmaps.size}/4)",
                    fontSize = 14.sp, color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                // Row 1
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageSlotCard(
                        bitmap = selectedBitmaps.getOrNull(0),
                        modifier = Modifier.weight(1f).height(120.dp),
                        onAddClick = {
                            activeSlotIndex = 0
                            permissionHelper.launchGallery() // เรียกผ่าน Helper ง่ายๆ
                        },
                        onDeleteClick = { selectedBitmaps.removeAt(0) }
                    )
                    ImageSlotCard(
                        bitmap = selectedBitmaps.getOrNull(1),
                        modifier = Modifier.weight(1f).height(120.dp),
                        onAddClick = {
                            activeSlotIndex = 1
                            permissionHelper.launchGallery()
                        },
                        onDeleteClick = { selectedBitmaps.removeAt(1) },
                        enabled = selectedBitmaps.isNotEmpty()
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Row 2
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageSlotCard(
                        bitmap = selectedBitmaps.getOrNull(2),
                        modifier = Modifier.weight(1f).height(120.dp),
                        onAddClick = {
                            activeSlotIndex = 2
                            permissionHelper.launchGallery()
                        },
                        onDeleteClick = { selectedBitmaps.removeAt(2) },
                        enabled = selectedBitmaps.size >= 2
                    )
                    ImageSlotCard(
                        bitmap = selectedBitmaps.getOrNull(3),
                        modifier = Modifier.weight(1f).height(120.dp),
                        onAddClick = {
                            activeSlotIndex = 3
                            permissionHelper.launchGallery()
                        },
                        onDeleteClick = { selectedBitmaps.removeAt(3) },
                        enabled = selectedBitmaps.size >= 3
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- ปุ่มควบคุมหลัก (Quick Buttons) ---
            val isFull = selectedBitmaps.size >= 4
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // ปุ่มถ่ายรูป (ใช้ Helper)
                Button(
                    onClick = { permissionHelper.launchCamera() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81)),
                    enabled = !isFull
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("ถ่ายเพิ่ม")
                }

                // ปุ่มอัลบั้ม (ใช้ Helper)
                OutlinedButton(
                    onClick = {
                        activeSlotIndex = -1
                        permissionHelper.launchGallery()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isFull
                ) {
                    Icon(Icons.Default.Image, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("อัลบั้ม")
                }
            }
            if (isFull) {
                Text("เพิ่มได้สูงสุด 4 รูป", fontSize = 12.sp, color = Color.Red, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(24.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            // --- ส่วนข้อมูลการทำงาน (เหมือนเดิม) ---
            Text("ข้อมูลการทำงาน", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333), modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(12.dp))

            SooktaDropdown("ระยะเวลาที่ทำต่อเนื่อง (Duration)", listOf("น้อยกว่า 1 ชั่วโมง", "1 - 2 ชั่วโมง", "2 - 4 ชั่วโมง", "มากกว่า 4 ชั่วโมง"), selectedDuration, { selectedDuration = it }, expandedDuration, { expandedDuration = it })
            Spacer(Modifier.height(12.dp))
            SooktaDropdown("ความถี่ในการทำ (Frequency)", listOf("ทำทุกวัน", "2-3 ครั้งต่อสัปดาห์", "ทำนานๆ ครั้ง"), selectedFrequency, { selectedFrequency = it }, expandedFrequency, { expandedFrequency = it })
            Spacer(Modifier.height(12.dp))
            SooktaDropdown("น้ำหนักวัตถุ (ระบุเฉพาะงานยก/แบก)", listOf("0 กก. (ไม่ได้ยก/แบก)", "น้อยกว่า 5 กก.", "5 - 10 กก.", "10 - 20 กก.", "มากกว่า 20 กก."), selectedWeight, { selectedWeight = it }, expandedWeight, { expandedWeight = it })
            Text("* หากไม่ใช่กิจกรรมยก/แบก ให้คงค่าไว้ที่ 0 กก.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, top = 4.dp))

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val initialScore = (3..9).random()
                    navController.navigate("initial_risk/$activityName/$initialScore")
                },
                enabled = selectedBitmaps.isNotEmpty() && selectedDuration.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("เริ่มวิเคราะห์ความเสี่ยง", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(50.dp))
        }
    }
}

// Component ย่อย: ImageSlotCard และ SooktaDropdown (ใช้ของเดิมได้เลย หรือ Copy ด้านล่างไปแปะท้ายไฟล์)
@Composable
fun ImageSlotCard(bitmap: Bitmap?, modifier: Modifier = Modifier, onAddClick: () -> Unit, onDeleteClick: () -> Unit, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) Color(0xFFF5F5F5) else Color(0xFFE0E0E0))
            .border(1.dp, if (enabled) Color.LightGray else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled && bitmap == null) { onAddClick() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.8f)).clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        } else if (enabled) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray)
                Text("เพิ่มรูป", fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray.copy(alpha=0.5f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SooktaDropdown(label: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit, expanded: Boolean, onExpandedChange: (Boolean) -> Unit) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = selectedOption, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF5C9A81), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onOptionSelected(option); onExpandedChange(false) }) }
        }
    }
}