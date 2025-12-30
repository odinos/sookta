package com.kdev.sookta.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.kdev.sookta.R

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 1. วางรูปพื้นหลังไว้ชั้นล่างสุด
        Image(
            painter = painterResource(id = R.drawable.app_background),
            contentDescription = null,
            contentScale = ContentScale.Crop, // ปรับรูปให้เต็มจอ (Crop ตัดส่วนเกินออก)
            modifier = Modifier.fillMaxSize()
        )

        // 2. วางเนื้อหาทับลงไป
        content()
    }
}