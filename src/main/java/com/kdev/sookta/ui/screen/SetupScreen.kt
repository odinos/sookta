package com.kdev.sookta.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.data.UserPreference
import kotlinx.coroutines.launch


@Composable
fun SetupScreen(navController: NavController) {
    var selectedLanguage by remember { mutableStateOf("TH") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = AppDatabase.getDatabase(context)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Language selection buttons at the top right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { selectedLanguage = "TH" },
                    enabled = selectedLanguage != "TH"
                ) {
                    Text("ไทย")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { selectedLanguage = "EN" },
                    enabled = selectedLanguage != "EN"
                ) {
                    Text("English")
                }
            }

            // Main content
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "จัดการข้อมูลเบื้องต้น", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(32.dp))
                // You can add more setup options here
            }


            // Save button at the bottom
            Button(
                onClick = {
                    coroutineScope.launch {
                        // --- Here is where you save data to SQLite using Room ---
                        val preference = UserPreference(id = 1, language = selectedLanguage)
                        db.userPreferenceDao().insertPreference(preference)
                        // --------------------------------------------------------

                        // Navigate to main screen and remove setup screen from back stack
                        navController.navigate("main") {
                            popUpTo("setup") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Text("บันทึกและเริ่มต้นใช้งาน")
            }
        }
    }
}