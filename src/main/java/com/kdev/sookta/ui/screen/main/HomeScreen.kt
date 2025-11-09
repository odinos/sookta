package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Home Screen")
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            // Pass data (activity name) to the next screen
            navController.navigate("evaluation_form/ขุดหลุมปลูก")
        }) {
            Text("เลือกกิจกรรม: ขุดหลุมปลูก")
        }
    }
}
