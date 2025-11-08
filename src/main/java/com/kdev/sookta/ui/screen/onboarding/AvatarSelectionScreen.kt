package com.kdev.sookta.ui.screen.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun AvatarSelectionScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Avatar Selection Screen")
        Spacer(Modifier.height(20.dp))
        Text("Grid of avatars will be here")
        // Add Avatar grid here
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            // Navigate to main and clear the entire back stack
            navController.navigate("main") {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        }) {
            Text("เลือก AVARTAR")
        }
    }
}
