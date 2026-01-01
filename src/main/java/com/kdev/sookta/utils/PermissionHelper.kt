package com.kdev.sookta.utils

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Interface สำหรับเก็บคำสั่งที่เราจะเรียกใช้ในหน้า UI (กดปุ่มแล้วให้ทำอะไร)
 */
data class PermissionActions(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)

/**
 * Helper แบบ Composable ที่ทันสมัย
 * รวม Logic การขอ Permission และ Launcher ไว้ในนี้ที่เดียว
 * ทำให้หน้าจอ UI สะอาด และเรียกใช้ง่าย
 */
@Composable
fun rememberPermissionHelper(
    onCameraCapture: (Bitmap) -> Unit,
    onGallerySelection: (Uri) -> Unit
): PermissionActions {
    val context = LocalContext.current

    // --- 1. ส่วนจัดการกล้อง (Camera) ---

    // Launcher A: สำหรับถ่ายรูปจริง (ได้ Bitmap กลับมา)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) onCameraCapture(bitmap)
    }

    // Launcher B: สำหรับขออนุญาตใช้กล้อง
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // ถ้ากดอนุญาต -> ให้เปิดกล้องต่อเลย
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "จำเป็นต้องอนุญาตให้ใช้กล้องเพื่อถ่ายรูป", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 2. ส่วนจัดการอัลบั้ม (Photo Picker) ---
    // ใช้ระบบ PickVisualMedia (Android Photo Picker) ที่ทันสมัย ไม่ต้องขอ Permission Storage
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onGallerySelection(uri)
    }

    // --- 3. ส่งคำสั่งกลับไปให้ UI เรียกใช้ ---
    return remember {
        PermissionActions(
            launchCamera = {
                val permission = Manifest.permission.CAMERA
                // ตรวจสอบสิทธิ์ก่อน
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    // ถ้ามีสิทธิ์แล้ว -> เปิดกล้อง
                    cameraLauncher.launch(null)
                } else {
                    // ถ้ายังไม่มี -> ขอสิทธิ์
                    permissionLauncher.launch(permission)
                }
            },
            launchGallery = {
                // เปิด Photo Picker (เลือกเฉพาะรูปภาพ)
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }
}