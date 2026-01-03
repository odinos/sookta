package com.kdev.sookta.ui.screen.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.kdev.sookta.R
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.utils.rememberPermissionHelper
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AvatarSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // --- 1. ตรวจสอบสถานะ (Edit หรือ New) ---
    // ใช้ collectAsState เพื่อรอข้อมูล
    val userPref by db.userPreferenceDao().getPreference().collectAsState(initial = null)

    // ตรวจสอบว่าโหลดข้อมูลเสร็จหรือยัง (ป้องกัน userPref เป็น null)
    val isDataLoaded = userPref != null
    val isEditMode = remember(userPref) { userPref?.isSetupCompleted == true }

    // --- State ---
    var customBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedAvatarResId by remember { mutableStateOf<Int?>(null) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    // รายการ Avatar ตัวอย่าง
    val avatarList = listOf(
        R.drawable.male_01,
        R.drawable.female_01,
        R.drawable.male_02,
        R.drawable.female_02,
        // เพิ่มรูปอื่นๆ ตรงนี้ได้
    )

    // --- Camera Launcher (รับผลลัพธ์จากกล้อง) ---
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageUri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, tempImageUri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, tempImageUri!!)
                    ImageDecoder.decodeBitmap(source)
                }
                // ย่อภาพเพื่อประหยัด Memory
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 600, 600, true)
                customBitmap = scaledBitmap
                selectedAvatarResId = null
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.error_load_image), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ฟังก์ชันเปิดกล้องอย่างปลอดภัย
    fun launchCameraSafe() {
        try {
            val uri = createImageUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            val msg = context.getString(R.string.error_camera_open, e.message)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    // --- Permission Check ---
    // ตัวขอ Permission กล้อง ถ้าได้สิทธิ์แล้วค่อยเปิดกล้อง
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraSafe()
        } else {
            Toast.makeText(context, context.getString(R.string.permission_camera_required), Toast.LENGTH_SHORT).show()
        }
    }

    // --- Permission Helper (สำหรับ Gallery) ---
    val permissionHelper = rememberPermissionHelper(
        onCameraCapture = {
            // ไม่ใช้ Callback นี้ เพราะเราใช้ Custom Camera Launcher ด้านบน
        },
        onGallerySelection = { uri ->
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                customBitmap = bitmap
                selectedAvatarResId = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(if (isEditMode) R.string.avatar_edit_title else R.string.avatar_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5C9A81)
        )
        Text(
            text = stringResource(R.string.avatar_subtitle),
            color = Color.Gray
        )

        Spacer(Modifier.height(30.dp))

        // --- ส่วนแสดงผลรูปปัจจุบัน ---
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .border(4.dp, Color(0xFF5C9A81), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (customBitmap != null) {
                Image(
                    bitmap = customBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (selectedAvatarResId != null) {
                Image(
                    painter = painterResource(id = selectedAvatarResId!!),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // แสดงรูปเดิมถ้ามี (ในโหมดแก้ไข) หรือแสดง Icon ว่าง
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- ปุ่มถ่ายรูป / อัลบั้ม ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    // ตรวจสอบสิทธิ์ก่อนเปิดกล้อง
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        launchCameraSafe()
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81))
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.avatar_take_photo))
            }

            OutlinedButton(
                onClick = { permissionHelper.launchGallery() }
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.avatar_gallery))
            }
        }

        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        // --- Grid เลือก Avatar การ์ตูน ---
        Text(
            stringResource(R.string.avatar_select_hint),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(avatarList) { resId ->
                AvatarItem(
                    resId = resId,
                    isSelected = selectedAvatarResId == resId,
                    onClick = {
                        selectedAvatarResId = resId
                        customBitmap = null
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- 4. ปุ่มยืนยัน ---
        Button(
            onClick = {
                scope.launch {
                    val avatarValue = if (selectedAvatarResId != null) {
                        "res:$selectedAvatarResId"
                    } else if (customBitmap != null) {
                        saveImageToInternalStorage(context, customBitmap!!)
                    } else {
                        null
                    }

                    if (avatarValue != null) {
                        // บันทึกข้อมูล
                        db.userPreferenceDao().updateAvatarAndFinish(avatarValue)

                        // นำทาง (Navigation)
                        if (isEditMode) {
                            navController.popBackStack()
                        } else {
                            // ไปหน้า MainScreen (หลังจาก Setup เสร็จ)
                            navController.navigate("main") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.avatar_select_warning), Toast.LENGTH_SHORT).show()
                    }
                }
            },
            // ปุ่มจะกดได้เมื่อเลือกรูปแล้ว และข้อมูล userPref โหลดเสร็จแล้ว
            enabled = (customBitmap != null || selectedAvatarResId != null) && isDataLoaded,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text(stringResource(R.string.avatar_confirm), fontSize = 18.sp)
        }
    }
}

// ... AvatarItem เหมือนเดิม ...
@Composable
fun AvatarItem(resId: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF5C9A81) else Color.LightGray,
                shape = CircleShape
            )
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF5C9A81).copy(alpha = 0.3f))
            )
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// --- ฟังก์ชันสร้าง URI สำหรับเก็บรูปชั่วคราว ---
fun createImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "images")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = File.createTempFile("selected_image_", ".jpg", directory)
    // ตรงนี้สำคัญ: authorities ต้องตรงกับใน AndroidManifest
    val authority = "${context.packageName}.provider"
    return FileProvider.getUriForFile(context, authority, file)
}

// --- ฟังก์ชันบันทึกรูปลงเครื่อง ---
fun saveImageToInternalStorage(context: Context, bitmap: Bitmap): String {
    val fileName = "profile_${System.currentTimeMillis()}.jpg"
    context.openFileOutput(fileName, Context.MODE_PRIVATE).use { stream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    }
    return File(context.filesDir, fileName).absolutePath
}