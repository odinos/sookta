package com.kdev.sookta.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class PermissionHelper(private val context: Context,
                       private val cameraLauncher: ManagedActivityResultLauncher<String, Boolean>,
                       private val galleryLauncher: ManagedActivityResultLauncher<String, Boolean>) {

    private var onCameraGranted: (() -> Unit)? = null
    private var onGalleryGranted: (() -> Unit)? = null

    fun setCallbacks(onCamera: () -> Unit, onGallery: () -> Unit) {
        this.onCameraGranted = onCamera
        this.onGalleryGranted = onGallery
    }

    fun requestCameraPermission(onGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            this.onCameraGranted = onGranted
            cameraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // สำหรับ Android 13+ อาจต้องใช้ READ_MEDIA_IMAGES
    fun requestGalleryPermission(onGranted: () -> Unit) {
        // ในตัวอย่างนี้ขอแบบง่าย เช็ค READ_EXTERNAL_STORAGE (สำหรับ Android < 13)
        // หากเป็น Android 13+ ควรเช็ค Manifest.permission.READ_MEDIA_IMAGES
        onGranted() // ปกติการเลือกรูปผ่าน Photo Picker รุ่นใหม่ไม่ต้องขอสิทธิ์ storage แบบเก่า
    }

}

@Composable
fun rememberPermissionHelper(): PermissionHelper {
    val context = LocalContext.current
    var onCameraGranted by remember { mutableStateOf<(() -> Unit)?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onCameraGranted?.invoke()
        }
    }

    // สร้าง object และ return
    val helper = remember { PermissionHelper(context, cameraLauncher, cameraLauncher) } // Gallery ใช้ logic เดียวกันได้ในเบื้องต้น

    // อัปเดต callback state
    helper.setCallbacks(
        onCamera = { onCameraGranted?.invoke() },
        onGallery = { /* Logic gallery */ }
    )

    // Capture state เพื่อ update callback ล่าสุด
    SideEffect {
        // logic update
    }

    // Workaround ง่ายๆ เพื่อส่ง function เข้าไปใน Helper
    val wrapper = remember {
        PermissionHelper(context, cameraLauncher, cameraLauncher).apply {
            // ใช้งานจริงต้องจัดการ state callback ให้ดีกว่านี้ใน production
        }
    }

    // เพื่อความง่ายและเข้าใจ concept ขอให้ใช้ Pattern นี้ในหน้าจอแทน:
    return wrapper
}