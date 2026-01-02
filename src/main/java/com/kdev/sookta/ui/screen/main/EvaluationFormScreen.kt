package com.kdev.sookta.ui.screen.main

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
// Import Helper และ Model ที่เกี่ยวข้อง
import com.kdev.sookta.utils.rememberPermissionHelper
import com.kdev.sookta.utils.ErgoCalculatorHelper
import com.kdev.sookta.model.ErgoInputData
import com.kdev.sookta.model.JobType
import androidx.compose.ui.res.stringResource
import com.kdev.sookta.R
import androidx.compose.material3.ExposedDropdownMenuAnchorType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationFormScreen(navController: NavController, activityNameArg: String) {
    val context = LocalContext.current
    // ดึงชื่อกิจกรรม (ถ้าเป็น ID ก็แปลงเป็น String)
    val activityName = activityNameArg.toIntOrNull()?.let { stringResource(it) } ?: activityNameArg

    // ตรวจสอบประเภทงาน (JobType) จากชื่อกิจกรรมเพื่อเลือกสูตรคำนวณที่ถูกต้อง
    // หมายเหตุ: คุณอาจต้องปรับ Logic การเช็ค string นี้ตาม Resource จริงของคุณ
    val currentJobType = remember(activityName) {
        if (activityName.contains("เข็น") || activityName.contains("Push") || activityName.contains("Pull")) {
            JobType.PUSH_PULL
        } else {
            JobType.LIFTING
        }
    }

    // State เก็บรูปภาพ
    val selectedBitmaps = remember { mutableStateListOf<Bitmap>() }

    // ตัวแปร Dropdown
    val durationOptions = listOf(
        stringResource(R.string.dur_opt_1),
        stringResource(R.string.dur_opt_2),
        stringResource(R.string.dur_opt_3),
        stringResource(R.string.dur_opt_4)
    )
    val frequencyOptions = listOf(
        stringResource(R.string.freq_opt_1),
        stringResource(R.string.freq_opt_2),
        stringResource(R.string.freq_opt_3)
    )
    val weightOptions = listOf(
        stringResource(R.string.weight_opt_0), // e.g., < 5 kg
        stringResource(R.string.weight_opt_1),
        stringResource(R.string.weight_opt_2),
        stringResource(R.string.weight_opt_3),
        stringResource(R.string.weight_opt_4) // e.g., > 20 kg
    )

    // State ข้อมูลจาก Dropdown
    val defaultWeight = stringResource(R.string.weight_opt_0)
    var selectedDuration by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf("") }
    var selectedWeight by remember(defaultWeight) { mutableStateOf(defaultWeight) }

    var expandedDuration by remember { mutableStateOf(false) }
    var expandedFrequency by remember { mutableStateOf(false) }
    var expandedWeight by remember { mutableStateOf(false) }

    // --- State สำหรับข้อมูลเชิงลึก (Advanced Inputs) ที่จำเป็นสำหรับการคำนวณจริง ---
    // สำหรับ Lifting (ISO 11228-1)
    var inputHorizontalDist by remember { mutableStateOf("25") } // H (cm)
    var inputVerticalHeight by remember { mutableStateOf("75") } // V (cm)

    // สำหรับ Push/Pull (ISO 11228-2)
    var inputForceInitial by remember { mutableStateOf("0") } // แรงเริ่มต้น (N)
    var inputForceSustain by remember { mutableStateOf("0") } // แรงขณะเข็น (N)

    // ตัวแปรบอกว่าตอนนี้เรากำลังจะเติมรูปใส่ช่องไหน (Slot Index)
    var activeSlotIndex by remember { mutableIntStateOf(-1) }

    // --- Permission Helper ---
    val permissionHelper = rememberPermissionHelper(
        onCameraCapture = { bitmap ->
            if (selectedBitmaps.size < 4) selectedBitmaps.add(bitmap)
        },
        onGallerySelection = { uri ->
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }

            if (activeSlotIndex in 0 until 4) {
                if (activeSlotIndex < selectedBitmaps.size) {
                    selectedBitmaps[activeSlotIndex] = bitmap
                } else {
                    selectedBitmaps.add(bitmap)
                }
            } else if (selectedBitmaps.size < 4) {
                selectedBitmaps.add(bitmap)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.eval_form_title), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back_desc), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5C9A81))
            )
        },
        containerColor = Color(0xFFFDF8E1)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.eval_activity_prefix, activityName),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(16.dp))

            // --- ส่วนรูปภาพ (เหมือนเดิม) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.eval_images_header, selectedBitmaps.size),
                    fontSize = 14.sp, color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                // Row 1
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageSlotCard(
                        bitmap = selectedBitmaps.getOrNull(0),
                        modifier = Modifier.weight(1f).height(120.dp),
                        onAddClick = { activeSlotIndex = 0; permissionHelper.launchGallery() },
                        onDeleteClick = { selectedBitmaps.removeAt(0) }
                    )
                    ImageSlotCard(
                        bitmap = selectedBitmaps.getOrNull(1),
                        modifier = Modifier.weight(1f).height(120.dp),
                        onAddClick = { activeSlotIndex = 1; permissionHelper.launchGallery() },
                        onDeleteClick = { selectedBitmaps.removeAt(1) },
                        enabled = selectedBitmaps.isNotEmpty()
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Row 2
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageSlotCard(
                        bitmap = selectedBitmaps.getOrNull(2),
                        modifier = Modifier.weight(1f).height(120.dp),
                        onAddClick = { activeSlotIndex = 2; permissionHelper.launchGallery() },
                        onDeleteClick = { selectedBitmaps.removeAt(2) },
                        enabled = selectedBitmaps.size >= 2
                    )
                    ImageSlotCard(
                        bitmap = selectedBitmaps.getOrNull(3),
                        modifier = Modifier.weight(1f).height(120.dp),
                        onAddClick = { activeSlotIndex = 3; permissionHelper.launchGallery() },
                        onDeleteClick = { selectedBitmaps.removeAt(3) },
                        enabled = selectedBitmaps.size >= 3
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Buttons Camera/Gallery
            val isFull = selectedBitmaps.size >= 4
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { permissionHelper.launchCamera() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81)),
                    enabled = !isFull
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_camera_add))
                }

                OutlinedButton(
                    onClick = { activeSlotIndex = -1; permissionHelper.launchGallery() },
                    modifier = Modifier.weight(1f),
                    enabled = !isFull
                ) {
                    Icon(Icons.Default.Image, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_gallery))
                }
            }
            if (isFull) {
                Text(stringResource(R.string.msg_max_images), fontSize = 12.sp, color = Color.Red, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.section_work_info), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333), modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(12.dp))

            // --- Dropdowns Inputs ---
            SooktaDropdown(stringResource(R.string.label_duration), durationOptions, selectedDuration, { selectedDuration = it }, expandedDuration, { expandedDuration = it })
            Spacer(Modifier.height(12.dp))
            SooktaDropdown(stringResource(R.string.label_frequency), frequencyOptions, selectedFrequency, { selectedFrequency = it }, expandedFrequency, { expandedFrequency = it })

            // แสดงช่องน้ำหนักเฉพาะงานยก (Lifting) ถ้าเป็นงานเข็น (Push/Pull) อาจไม่ต้องใช้น้ำหนักวัตถุโดยตรงแต่ใช้แรง
            if (currentJobType == JobType.LIFTING) {
                Spacer(Modifier.height(12.dp))
                SooktaDropdown(stringResource(R.string.label_weight), weightOptions, selectedWeight, { selectedWeight = it }, expandedWeight, { expandedWeight = it })
                Text(stringResource(R.string.note_weight_hint), fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            }

            Spacer(Modifier.height(16.dp))

            // --- Additional Inputs (Manual Entry for Precision) ---
            Text("ข้อมูลการวัดเพิ่มเติม (เพื่อความแม่นยำ)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333), modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))

            if (currentJobType == JobType.LIFTING) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputHorizontalDist,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) inputHorizontalDist = it },
                        label = { Text("ระยะห่างตัว (H) cm") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inputVerticalHeight,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) inputVerticalHeight = it },
                        label = { Text("ความสูงจุดยก (V) cm") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            } else {
                // JobType.PUSH_PULL
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputForceInitial,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) inputForceInitial = it },
                        label = { Text("แรงเริ่มเข็น (Initial) N") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inputForceSustain,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) inputForceSustain = it },
                        label = { Text("แรงขณะเข็น (Sustain) N") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- START ANALYZE BUTTON ---
            Button(
                onClick = {
                    // 1. แปลงค่าจาก Dropdown เป็นตัวเลข (Mapping Logic)
                    // หมายเหตุ: คุณควรปรับตัวเลขด้านขวาให้ตรงกับความหมายของ string ใน R.string ของคุณ
                    val weightVal = when (selectedWeight) {
                        weightOptions.getOrNull(0) -> 5.0  // < 5kg
                        weightOptions.getOrNull(1) -> 10.0
                        weightOptions.getOrNull(2) -> 15.0
                        weightOptions.getOrNull(3) -> 20.0
                        else -> 25.0 // Max case
                    }

                    val freqVal = when (selectedFrequency) {
                        frequencyOptions.getOrNull(0) -> 0.2 // น้อยครั้ง
                        frequencyOptions.getOrNull(1) -> 2.0 // ปานกลาง
                        else -> 6.0 // บ่อยมาก
                    }

                    val durationVal = when (selectedDuration) {
                        durationOptions.getOrNull(0) -> 1.0
                        durationOptions.getOrNull(1) -> 2.0
                        durationOptions.getOrNull(2) -> 4.0
                        else -> 8.0
                    }

                    // 2. สร้าง Input Data Object
                    val inputData = ErgoInputData(
                        jobType = currentJobType,
                        gender = "male", // TODO: ดึงจาก User Profile
                        // Lifting Inputs
                        loadWeight = weightVal,
                        horizontalDist = inputHorizontalDist.toDoubleOrNull() ?: 25.0,
                        verticalHeight = inputVerticalHeight.toDoubleOrNull() ?: 75.0,
                        liftFrequency = freqVal,
                        durationHours = durationVal,
                        // Push/Pull Inputs
                        initialForce = inputForceInitial.toDoubleOrNull() ?: 0.0,
                        sustainForce = inputForceSustain.toDoubleOrNull() ?: 0.0
                    )

                    // 3. เรียก Helper คำนวณความเสี่ยงจริง
                    val result = ErgoCalculatorHelper.calculateRisk(inputData)

                    // 4. ส่งข้อมูลไปยังหน้าถัดไป
                    // ส่ง Object ผลลัพธ์ผ่าน SavedStateHandle (วิธีที่ถูกต้องสำหรับข้อมูลซับซ้อน)
                    navController.currentBackStackEntry?.savedStateHandle?.set("riskResult", result)
                    navController.currentBackStackEntry?.savedStateHandle?.set("inputData", inputData)

                    // Navigate โดยส่งคะแนนและ ID ไปใน Route ด้วย (เผื่อใช้ display เบื้องต้น)
                    navController.navigate("initial_risk/$activityNameArg/${result.score}")
                },
                // ปิดปุ่มถ้าข้อมูลยังไม่ครบ (ปรับเงื่อนไขตามความเหมาะสม)
                enabled = selectedBitmaps.isNotEmpty() && selectedDuration.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_start_analyze), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(50.dp))
        }
    }
}

// ... (ส่วน ImageSlotCard และ SooktaDropdown คงเดิมตามโค้ดที่คุณส่งมา) ...
@Composable
fun ImageSlotCard(bitmap: Bitmap?, modifier: Modifier = Modifier, onAddClick: () -> Unit, onDeleteClick: () -> Unit, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) Color(0xFFF5F5F5) else Color(0xFFE0E0E0))
            .border(1.dp, if (enabled) Color.LightGray else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled && bitmap == null) { onAddClick() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.8f)).clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        } else if (enabled) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray)
                Text(stringResource(R.string.form_add_photo), fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray.copy(alpha=0.5f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SooktaDropdown(label: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit, expanded: Boolean, onExpandedChange: (Boolean) -> Unit) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = selectedOption, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF5C9A81), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onOptionSelected(option); onExpandedChange(false) }) }
        }
    }
}