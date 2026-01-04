package com.kdev.sookta.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // ตั้งค่าภาษาไทย
            val result = tts?.setLanguage(Locale.Builder().setLanguage("th").setRegion("TH").build())

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "ภาษาไทยไม่รองรับในเครื่องนี้ หรือยังไม่ได้ติดตั้งข้อมูลเสียง")
                // กรณีไม่มีภาษาไทย อาจจะ fallback เป็นอังกฤษ หรือแจ้งเตือน
                tts?.language = Locale.US
            } else {
                isReady = true
            }

            val currentLocale = Locale.getDefault()
            updateLanguage(currentLocale)

        } else {
            Log.e("TTS", "การเริ่มต้น TextToSpeech ล้มเหลว")
        }
    }
    fun updateLanguage(locale: Locale) {
        // ใช้ Locale.Builder ตามที่คุณแนะนำ เพื่อความแม่นยำ
        val targetLocale = if (locale.language == "th") {
            Locale.Builder().setLanguage("th").setRegion("TH").build()
        } else {
            Locale.US // หรือ Locale.ENGLISH
        }

        val result = tts?.setLanguage(targetLocale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS", "ภาษา ${targetLocale.displayLanguage} ไม่รองรับในเครื่องนี้")
            // กรณีภาษาไทยใช้ไม่ได้ ให้ Fallback เป็น US
            if (targetLocale.language == "th") {
                tts?.language = Locale.US
            }
        } else {
            isReady = true
        }
    }
    fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}