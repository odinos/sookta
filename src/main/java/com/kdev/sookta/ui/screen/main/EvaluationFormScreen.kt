package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun EvaluationFormScreen(navController: NavController, activityName: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Evaluation Form for: $activityName")
        Spacer(Modifier.height(20.dp))
        // Add your form fields here
        Button(onClick = { navController.navigate("result") }) {
            Text("คำนวณคะแนน")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}
