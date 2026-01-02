package com.kdev.sookta.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource // ✅ Import
import com.kdev.sookta.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // ดึงข้อมูลเดิมจาก Database (ถ้ามี)
    val userPref by db.userPreferenceDao().getPreference().collectAsState(initial = null)

    // State สำหรับเก็บข้อมูลในฟอร์ม
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    // ตัวแปรเช็คว่าโหลดข้อมูลเสร็จหรือยัง (เพื่อไม่ให้ทับสิ่งที่ User กำลังแก้)
    var isDataLoaded by remember { mutableStateOf(false) }

    // เมื่อโหลดข้อมูล UserPref มาแล้ว ให้เอามาใส่ในช่องต่างๆ
    LaunchedEffect(userPref) {
        if (!isDataLoaded && userPref != null) {
            name = userPref?.userName ?: ""
            age = userPref?.age ?: ""
            gender = userPref?.gender ?: "Male"
            weight = userPref?.weight ?: ""
            height = userPref?.height ?: ""
            isDataLoaded = true // ล็อคไว้ ไม่ให้โหลดซ้ำ
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setup_page_title), color = Color.White) },
                navigationIcon = {
                    // ปุ่ม Back: กดแล้วย้อนกลับไปหน้าก่อนหน้า (Profile)
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back_desc), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5C9A81))
            )
        },
        containerColor = Color(0xFFFDF8E1) // สีพื้นหลัง
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.setup_header_edit),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C9A81),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(20.dp))

            // Form Fields
            SooktaTextField(value = name, onChange = { name = it }, label = stringResource(R.string.label_name))
            Spacer(Modifier.height(12.dp))

            SooktaTextField(value = age, onChange = { age = it }, label = stringResource(R.string.label_age), isNumber = true)
            Spacer(Modifier.height(12.dp))

            // Gender Selection
            Text(stringResource(R.string.label_gender), modifier = Modifier.align(Alignment.Start), color = Color(0xFF5C9A81))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                GenderOption(stringResource(R.string.gender_male), selected = gender == "Male") { gender = "Male" }
                GenderOption(stringResource(R.string.gender_female), selected = gender == "Female") { gender = "Female" }
            }

            Spacer(Modifier.height(12.dp))
            SooktaTextField(value = weight, onChange = { weight = it }, label = stringResource(R.string.label_weight_kg), isNumber = true)
            Spacer(Modifier.height(12.dp))
            SooktaTextField(value = height, onChange = { height = it }, label = stringResource(R.string.label_height_cm), isNumber = true)

            Spacer(Modifier.height(40.dp))

            // ปุ่มบันทึก
            Button(
                onClick = {
                    scope.launch {
                        // บันทึกข้อมูลลง Database
                        db.userPreferenceDao().updatePersonalInfo(
                            name = name,
                            age = age,
                            gender = gender,
                            weight = weight,
                            height = height
                        )

                        // บันทึกเสร็จแล้ว ถอยกลับไปหน้า Profile
                        navController.popBackStack()
                    }
                },
                enabled = name.isNotEmpty() && age.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81))
            ) {
                Text(stringResource(R.string.btn_save_data), fontSize = 18.sp)
            }
        }
    }
}

// Helper Components (เหมือนเดิม)
@Composable
fun SooktaTextField(value: String, onChange: (String) -> Unit, label: String, isNumber: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF5C9A81),
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