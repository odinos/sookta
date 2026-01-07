package com.kdev.sookta.ui.screen.main

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.R
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.data.EvaluationEntity
import com.kdev.sookta.model.ErgoResult
import com.kdev.sookta.model.RiskLevel
import com.kdev.sookta.ui.component.TTSButton
import com.kdev.sookta.utils.AnalyticsManager
import com.kdev.sookta.utils.TextToSpeechManager

@Composable
fun FinalResultScreen(navController: NavController, oldScoreArg: Int, newScoreArg: Int, activityNameArg: String) {
    val context = LocalContext.current
    val resources = context.resources
    val packageName = context.packageName

    val db = remember { AppDatabase.getDatabase(context) }
    val analyticsManager = remember { AnalyticsManager(context) }

    val displayActivityName = activityNameArg.toIntOrNull()?.let { stringResource(it) } ?: activityNameArg
    val ttsManager = remember { TextToSpeechManager(context) }
    DisposableEffect(Unit) { onDispose { ttsManager.shutdown() } }

    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val initialResult = savedStateHandle?.get<ErgoResult>("initialResult")
    val finalResult = savedStateHandle?.get<ErgoResult>("finalResult")
    val selectedSuggestionKeys = savedStateHandle?.get<ArrayList<String>>("selectedSuggestions") ?: emptyList<String>()

    val selectedSuggestionsDisplay = remember(selectedSuggestionKeys) {
        selectedSuggestionKeys.mapNotNull { key ->
            if (key.all { it.isDigit() }) {
                try {
                    resources.getString(key.toInt())
                } catch (e: Exception) {
                    null
                }
            } else {
                val resId = resources.getIdentifier(key, "string", packageName)
                if (resId != 0) resources.getString(resId) else key
            }
        }
    }

    val beforeScore = initialResult?.userScore ?: oldScoreArg
    val afterScore = finalResult?.userScore ?: newScoreArg

    var isSaved by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isSaved && initialResult != null && finalResult != null) {
            val bodyMapString = initialResult.bodyPartRisks.entries.joinToString(",") { "${it.key.name}:${it.value.name}" }
            val suggestionStringToSave = selectedSuggestionsDisplay.joinToString(", ")

            val entity = EvaluationEntity(
                activityName = displayActivityName,
                dateTimestamp = System.currentTimeMillis(),
                scoreBefore = initialResult.userScore.toDouble(),
                scoreAfter = finalResult.userScore.toDouble(),
                riskBefore = initialResult.riskLevel.name,
                riskAfter = finalResult.riskLevel.name,
                improvementNote = suggestionStringToSave,
                economicLoss = initialResult.economicLoss,
                bodyMapData = bodyMapString
            )

            val moneySavedCalc = initialResult.economicLoss - finalResult.economicLoss

            db.evaluationDao().insertEvaluation(entity)
            analyticsManager.logEvaluationSaved(
                activityName = displayActivityName,
                jobType = "General",
                scoreBefore = initialResult.userScore,
                scoreAfter = finalResult.userScore,
                riskLevelBefore = initialResult.riskLevel.name,
                riskLevelAfter = finalResult.riskLevel.name,
                moneySaved = if(moneySavedCalc > 0) moneySavedCalc else 0
            )
            selectedSuggestionKeys.forEach { key ->
                analyticsManager.logSuggestionSelected(key)
            }
            isSaved = true
        }
    }

    LaunchedEffect(Unit) {
        analyticsManager.logScreenView("FinalResultScreen")
    }

    val beforeColor = initialResult?.userScoreColor?.let { Color(it) } ?: Color.Gray
    val afterColor = finalResult?.userScoreColor?.let { Color(it) } ?: Color(0xFF4CAF50)

    val lossBefore = initialResult?.economicLoss ?: 0
    val lossAfter = finalResult?.economicLoss ?: 0
    val moneySaved = lossBefore - lossAfter

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF8E1))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.final_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        Text(
            text = stringResource(R.string.final_subtitle),
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.risk_score_label), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF5C9A81))
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RiskScoreItem(score = beforeScore, color = beforeColor, label = stringResource(R.string.label_before))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.LightGray)
                    RiskScoreItem(score = afterScore, color = afterColor, label = stringResource(R.string.label_after))
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                // --- ปรับปรุงการแสดงผล Economic Loss ---
                if (moneySaved > 0 || (lossBefore > 0 && lossAfter < lossBefore)) {
                    Text("ผลกระทบทางเศรษฐกิจ", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Before Loss (Red)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$lossBefore",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F) // Red
                            )
                            Text("บาท/ปี", fontSize = 12.sp, color = Color.Gray)
                        }

                        Spacer(Modifier.width(16.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.rotate(-90f)) // Right Arrow
                        Spacer(Modifier.width(16.dp))

                        // After Loss (Green if 0, else Orange/Green)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$lossAfter",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (lossAfter == 0) Color(0xFF4CAF50) else Color(0xFFFF9800) // Green or Orange
                            )
                            Text("บาท/ปี", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Money Saved Badge
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "ประหยัดได้ $moneySaved บาท!",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                } else if (lossAfter == 0 && lossBefore == 0) {
                    Text(stringResource(R.string.money_safe), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                } else {
                    Text(stringResource(R.string.money_risk_remain, lossAfter), color = Color(0xFFD32F2F), fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.risky_point_header), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                if (initialResult != null) {
                    BodyMapVisualization(bodyRisks = initialResult.bodyPartRisks)
                    Spacer(Modifier.height(16.dp))

                    val riskyParts = initialResult.bodyPartRisks.filter { it.value != RiskLevel.LOW }
                    if (riskyParts.isNotEmpty()) {
                        riskyParts.forEach { (part, level) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Box(Modifier.size(10.dp).background(Color(level.colorHex), CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text("${getBodyPartName(part)}: ${getRiskLevelName(level)}", fontSize = 14.sp, color = Color.DarkGray)
                            }
                        }
                    } else {
                        Text(stringResource(R.string.no_risky_parts), fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (selectedSuggestionsDisplay.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbUp, null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.suggestion_selected_header),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    selectedSuggestionsDisplay.forEach { suggestion ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = suggestion,
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            TTSButton(text = suggestion, ttsManager = ttsManager, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                navController.navigate("main") {
                    popUpTo("main") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81))
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_back_home), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// Reuse Composable
@Composable
fun RiskScoreItem(score: Int, color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "$score", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

