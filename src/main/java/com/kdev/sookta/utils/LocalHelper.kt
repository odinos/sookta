package com.kdev.sookta.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "sookta_prefs"
    private const val KEY_LANG = "app_language"

    // อ่านภาษาปัจจุบัน (ถ้าไม่มี default เป็น "th")
    fun getLanguage(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, "th") ?: "th"
    }

    // บันทึกภาษาใหม่
    fun setLanguage(context: Context, language: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, language).apply()
    }

    // ฟังก์ชันสำคัญ! ใช้ห่อ Context ด้วยภาษาใหม่
    fun onAttach(context: Context): Context {
        val lang = getLanguage(context)
        return updateContextLocale(context, lang)
    }

    fun updateContextLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
    }
}