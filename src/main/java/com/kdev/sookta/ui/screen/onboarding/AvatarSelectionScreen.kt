package com.kdev.sookta.ui.screen.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.kdev.sookta.R
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.utils.rememberPermissionHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun AvatarSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // --- 1. ตรวจสอบสถานะ (Edit หรือ New) ---
    val userPref by db.userPreferenceDao().getPreference().collectAsState(initial = null)
    val isEditMode = remember(userPref) { userPref?.isSetupCompleted == true }

    // --- State ---
    var customBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedAvatarResId by remember { mutableStateOf<Int?>(null) }

    // รายการ Avatar ตัวอย่าง
    val avatarList = listOf(
        R.drawable.logo_app,
        R.drawable.sookta_logo,
        // เพิ่มรูปอื่นๆ ตรงนี้
    )

    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- Permission Helper ---
    val permissionHelper = rememberPermissionHelper(
        onCameraCapture = { bitmap ->
            customBitmap = bitmap
            selectedAvatarResId = null
        },
        onGallerySelection = { uri ->
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
            customBitmap = bitmap
            selectedAvatarResId = null
        }
    )

    // Camera Launcher
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
                customBitmap = bitmap
                selectedAvatarResId = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun launchCameraSafe() {
        val uri = createImageUri(context)
        tempImageUri = uri
        cameraLauncher.launch(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            text = if (isEditMode) "เปลี่ยนรูปโปรไฟล์" else stringResource(R.string.avatar_title),
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
                onClick = { launchCameraSafe() },
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

        // --- 4. ปุ่มยืนยัน (ปรับปรุง Logic การบันทึก) ---
        Button(
            onClick = {
                scope.launch {
                    // เตรียมข้อมูล Avatar String
                    val avatarValue = if (selectedAvatarResId != null) {
                        // กรณีเลือกรูปการ์ตูน: เก็บ Resource ID
                        "res:${selectedAvatarResId}"
                    } else if (customBitmap != null) {
                        // กรณีถ่ายรูป/เลือกรูป: บันทึกไฟล์ลงเครื่อง แล้วเก็บ Path
                        val savedPath = saveImageToInternalStorage(context, customBitmap!!)
                        savedPath // เก็บ Path เช่น /data/user/0/.../profile.jpg
                    } else {
                        null
                    }

                    if (avatarValue != null) {
                        // บันทึกและจบการ Setup
                        db.userPreferenceDao().updateAvatarAndFinish(avatarValue)

                        // --- 5. Navigation Logic (แยกกรณี Edit/New) ---
                        if (isEditMode) {
                            navController.popBackStack() // กลับไปหน้า Profile
                        } else {
                            navController.navigate("main") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                }
            },
            enabled = customBitmap != null || selectedAvatarResId != null,
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

// ... createImageUri เหมือนเดิม ...
fun createImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "images")
    directory.mkdirs()
    val file = File.createTempFile("selected_image_", ".jpg", directory)
    val authority = "${context.packageName}.provider"
    return FileProvider.getUriForFile(context, authority, file)
}

// --- ฟังก์ชันเพิ่มใหม่: บันทึก Bitmap ลง Internal Storage ---
fun saveImageToInternalStorage(context: Context, bitmap: Bitmap): String {
    // ตั้งชื่อไฟล์ (ใช้ timestamp เพื่อไม่ให้ซ้ำ)
    val fileName = "profile_${System.currentTimeMillis()}.jpg"

    // เปิด Output Stream เพื่อเขียนไฟล์ลงใน private storage ของแอป
    context.openFileOutput(fileName, Context.MODE_PRIVATE).use { stream ->
        // บีบอัดเป็น JPG คุณภาพ 90%
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    }

    // คืนค่า Absolute Path กลับไปเพื่อบันทึกลง Database
    return File(context.filesDir, fileName).absolutePath
}