package com.kdev.sookta.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsManager(context: Context) {
    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    // 1. Log เมื่อมีการบันทึกผลการประเมิน (สำคัญที่สุดตามโจทย์)
    fun logEvaluationSaved(
        activityName: String,
        jobType: String,     // LIFTING, PUSH_PULL, REBA
        scoreBefore: Int,
        scoreAfter: Int,
        riskLevelBefore: String,
        riskLevelAfter: String,
        moneySaved: Int
    ) {
        val bundle = Bundle().apply {
            putString("activity_name", activityName)
            putString("job_type", jobType)
            putInt("score_before", scoreBefore)
            putInt("score_after", scoreAfter)
            putString("risk_before", riskLevelBefore)
            putString("risk_after", riskLevelAfter)
            putInt("money_saved", moneySaved)

            // เพิ่ม Timestamp เพื่อดูช่วงเวลา
            putLong("timestamp", System.currentTimeMillis())
        }
        firebaseAnalytics.logEvent("evaluation_saved", bundle)
    }

    // 2. Log เมื่อเข้าใช้งานหน้าจอต่างๆ (Screen View)
    fun logScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    // 3. Log การกระทำทั่วไป (Button Click / Checkbox)
    fun logAction(actionName: String, label: String? = null) {
        val bundle = Bundle().apply {
            putString("action_type", actionName)
            if (label != null) putString("label", label)
        }
        firebaseAnalytics.logEvent("user_action", bundle)
    }

    // 4. Log เมื่อมีการเลือกคำแนะนำ (Insight ว่าคำแนะนำไหนคนชอบเลือก)
    fun logSuggestionSelected(suggestionKey: String) {
        val bundle = Bundle().apply {
            putString("suggestion_key", suggestionKey)
        }
        firebaseAnalytics.logEvent("suggestion_selected", bundle)
    }
}