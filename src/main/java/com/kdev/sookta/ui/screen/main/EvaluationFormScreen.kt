package com.kdev.sookta.ui.screen.main

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
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
import com.kdev.sookta.ui.component.AppBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationFormScreen(navController: NavController, activityName: String) {
    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Launcher สำหรับถ่ายรูป
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedBitmap = bitmap
        }
    }

    // Launcher สำหรับเลือกรูปจาก Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            selectedBitmap = bitmap
        }
    }
    AppBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ประเมิน: $activityName", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5C9A81))
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // คำแนะนำ
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Padding(padding = 16.dp) {
                        Text(
                            text = "กรุณาถ่ายรูปหรือเลือกรูปภาพตำแหน่งที่ต้องการประเมิน เพื่อให้ระบบวิเคราะห์ความเสี่ยง",
                            fontSize = 16.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // พื้นที่แสดงรูปภาพ (Image Preview Area)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(2.dp, Color(0xFF8CC63F), RoundedCornerShape(16.dp)) // ขอบสีเขียวอ่อน
                        .clickable {
                            // กดที่รูปเพื่อเลือกจาก Gallery (Default)
                            galleryLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop // หรือ Fit ตามความเหมาะสม
                        )
                        // ปุ่มลบ/แก้รูปเล็กๆ (Optional)
                        Box(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            SmallFloatingActionButton(
                                onClick = { selectedBitmap = null },
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ) {
                                Text("X", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("แตะเพื่อเพิ่มรูปภาพ", color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ปุ่มเมนูเลือกรูป (Camera / Gallery Buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ปุ่มถ่ายรูป
                    MenuActionButton(
                        text = "ถ่ายรูป",
                        icon = Icons.Default.CameraAlt,
                        color = Color(0xFF5C9A81),
                        modifier = Modifier.weight(1f),
                        onClick = { cameraLauncher.launch() }
                    )

                    // ปุ่มเลือกจากเครื่อง
                    MenuActionButton(
                        text = "อัลบั้ม",
                        icon = Icons.Default.Image,
                        color = Color(0xFF8CC63F), // เขียวอ่อนลงหน่อย
                        modifier = Modifier.weight(1f),
                        onClick = { galleryLauncher.launch("image/*") }
                    )
                }

                Spacer(Modifier.height(32.dp))

                // ปุ่มประเมินผล (Main Action)
                Button(
                    onClick = {
                        // สมมติว่าคำนวณคะแนนเบื้องต้นได้ 8 (High Risk) จาก AI หรือ Form
                        // ส่งคะแนนเบื้องต้น (8) และชื่อกิจกรรม ไปหน้าเลือกวิธีแก้
                        val initialScore = 8
                        navController.navigate("initial_risk/$activityName/$initialScore")
                    },
                    enabled = selectedBitmap != null, // ต้องมีรูปก่อนถึงกดได้
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE65100), // สีส้มเน้นปุ่ม Action (หรือใช้สีธีมก็ได้)
                        disabledContainerColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("เริ่มการวิเคราะห์", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Component ปุ่มเมนูย่อย
@Composable
fun MenuActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

// Helper สำหรับ Padding (เพื่อความสะดวก)
@Composable
fun Padding(padding: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(padding)) { content() }
}