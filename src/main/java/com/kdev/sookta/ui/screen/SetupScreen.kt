package com.kdev.sookta.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.ui.theme.SooktaGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") } // Default value
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF8E1))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()), // เผื่อจอเล็ก
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            text = "ข้อมูลส่วนตัว",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5C9A81)
        )
        Text("กรุณากรอกข้อมูลเบื้องต้น", color = Color.Gray)

        Spacer(Modifier.height(30.dp))

        // Form Fields
        SooktaTextField(value = name, onChange = { name = it }, label = "ชื่อเล่น (Name)")
        Spacer(Modifier.height(12.dp))

        SooktaTextField(value = age, onChange = { age = it }, label = "อายุ (ปี)", isNumber = true)
        Spacer(Modifier.height(12.dp))

        // Gender Selection (Simple Radio or Buttons)
        Text("เพศ", modifier = Modifier.align(Alignment.Start), color = Color(0xFF5C9A81))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            GenderOption("ชาย", selected = gender == "Male") { gender = "Male" }
            GenderOption("หญิง", selected = gender == "Female") { gender = "Female" }
        }

        Spacer(Modifier.height(12.dp))
        SooktaTextField(value = weight, onChange = { weight = it }, label = "น้ำหนัก (กก.)", isNumber = true)
        Spacer(Modifier.height(12.dp))
        SooktaTextField(value = height, onChange = { height = it }, label = "ส่วนสูง (ซม.)", isNumber = true)

        Spacer(Modifier.height(40.dp))

        // Next Button
        Button(
            onClick = {
                scope.launch {
                    // บันทึกข้อมูล
                    db.userPreferenceDao().updatePersonalInfo(
                        name = name,
                        age = age,
                        gender = gender,
                        weight = weight,
                        height = height
                    )
                    // ไปหน้าเลือก Avatar
                    navController.navigate("avatar_selection")
                }
            },
            enabled = name.isNotEmpty() && age.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81))
        ) {
            Text("ถัดไป", fontSize = 18.sp)
        }
    }
}

@Composable
fun SooktaTextField(value: String, onChange: (String) -> Unit, label: String, isNumber: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SooktaGreen,
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White
        ),
        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default
    )
}

@Composable
fun GenderOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF8CC63F) else Color.LightGray
        ),
        modifier = Modifier.width(120.dp)
    ) {
        Text(text)
    }
}