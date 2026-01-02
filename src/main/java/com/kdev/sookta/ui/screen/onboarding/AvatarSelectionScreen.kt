package com.kdev.sookta.ui.screen.onboarding

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.kdev.sookta.data.AppDatabase
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.kdev.sookta.R

@Composable
fun AvatarSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // กล้อง Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        capturedBitmap = bitmap
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF8E1))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "เลือกรูปโปรไฟล์",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5C9A81)
        )
        Text("แตะที่วงกลมเพื่อถ่ายรูป", color = Color.Gray)

        Spacer(Modifier.height(40.dp))

        // วงกลม Avatar
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(4.dp, Color(0xFF8CC63F), CircleShape)
                .clickable {
                    cameraLauncher.launch(null)
                },
            contentAlignment = Alignment.Center
        ) {
            if (capturedBitmap != null) {
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Icon กล้อง หรือ Placeholder
                Text("📷", fontSize = 60.sp)
            }
        }

        Spacer(Modifier.height(60.dp))

        Button(
            onClick = {
                scope.launch {
                    // Logic บันทึกรูปจริงๆ ต้อง save bitmap ลง Internal Storage แล้วเอา path มาใส่
                    // ตรงนี้จำลองใส่ path ว่า "camera_capture" ไปก่อน
                    db.userPreferenceDao().updateAvatarAndFinish("camera_capture")

                    navController.navigate("setup") {
                        // popUpTo อาจจะยังไม่ต้องล้างหมดก็ได้ หรือจะล้างแค่ avatar_selection ออก
                        popUpTo("avatar_selection") { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81))
        ) {
            Text(stringResource(R.string.btn_save_and_start), fontSize = 18.sp)
        }
    }
}