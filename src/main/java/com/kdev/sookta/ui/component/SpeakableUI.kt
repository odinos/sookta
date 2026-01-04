package com.kdev.sookta.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kdev.sookta.utils.TextToSpeechManager

// Helper เพื่อจำค่า TTS ไว้ใช้ตลอด Lifecycle ของหน้านั้นๆ
@Composable
fun rememberTextToSpeech(): TextToSpeechManager {
    val context = LocalContext.current
    val ttsManager = remember { TextToSpeechManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }
    return ttsManager
}

// ปุ่มรูปลำโพงสำเร็จรูป
@Composable
fun SpeakButton(
    textToSpeak: String,
    ttsManager: TextToSpeechManager,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    IconButton(
        onClick = { ttsManager.speak(textToSpeak) },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "อ่านออกเสียง: $textToSpeak", // ดีสำหรับ TalkBack ของคนตาบอด
            tint = color
        )
    }
}

// ข้อความพร้อมปุ่มลำโพง (Option เสริม)
@Composable
fun TextWithSpeaker(
    text: String,
    ttsManager: TextToSpeechManager,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.width(8.dp))
        SpeakButton(textToSpeak = text, ttsManager = ttsManager)
    }
}

@Composable
fun TTSButton(text: String, ttsManager: TextToSpeechManager, modifier: Modifier = Modifier) {
    IconButton(
        onClick = { ttsManager.speak(text) },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp, // หรือ Icons.Rounded.VolumeUp
            contentDescription = "Read aloud",
            tint = Color(0xFF5C9A81) // สีเขียวธีมแอป
        )
    }
}