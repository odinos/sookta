package com.kdev.sookta.ui.screen.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun LanguageSelectionScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Language Selection Screen")
        Spacer(Modifier.height(20.dp))
        Button(onClick = { navController.navigate("login") }) {
            Text("เลือกภาษาไทย")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { navController.navigate("login") }) {
            Text("Select English")
        }
    }
}

