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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdev.sookta.R
import com.kdev.sookta.data.AppDatabase
import com.kdev.sookta.model.ErgoInputData
import com.kdev.sookta.model.ErgoResult
import com.kdev.sookta.model.JobType
import com.kdev.sookta.model.RebaInputData
import com.kdev.sookta.utils.AnalyticsManager
import com.kdev.sookta.utils.ErgoCalculatorHelper
import com.kdev.sookta.utils.PoseEstimatorHelper
import com.kdev.sookta.utils.rememberPermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationFormScreen(navController: NavController, activityNameArg: String) {
    val context = LocalContext.current
    val analyticsManager = remember { AnalyticsManager(context) }
    val poseHelper = remember { PoseEstimatorHelper(context) }
    val activityName = activityNameArg.toIntOrNull()?.let { stringResource(it) } ?: activityNameArg
    val userDao = remember { AppDatabase.getDatabase(context).userPreferenceDao() }

    // ดึงข้อมูล User (รวมถึงรายได้)
    val userPrefState by userDao.getPreference().collectAsState(initial = null)

    // [Updated] คำนวณรายได้ต่อวัน (ถ้าไม่มีให้ใช้ Default 300)
    val userDailyIncome = remember(userPrefState) {
        val yearly = userPrefState?.incomePerYear?.toDoubleOrNull()
        if (yearly != null && yearly > 0) yearly / 365.0 else 300.0
    }

    val currentJobType = remember(activityName) {
        // แปลงเป็น lowercase เพื่อให้เช็คง่ายทั้ง EN/TH
        val name = activityName.lowercase()

        when {
            // กลุ่มงานเข็น (Push/Pull) - ถ้าชื่อมีคำว่า เข็น, push, pull, cart
            name.contains("เข็น") || name.contains("push") || name.contains("pull") || name.contains("cart") -> JobType.PUSH_PULL

            // กลุ่มงานขนย้าย (Transport) - อาจเป็นยกหรือเข็น (ให้ Default เป็น Lifting ถ้าไม่มีคำว่าเข็น)
            name.contains("ขนย้าย") || name.contains("transport") || name.contains("แบก") || name.contains("ยก") || name.contains("lifting") -> JobType.LIFTING

            // นอกนั้นให้เป็นงานท่าทาง (REBA) ทั้งหมด
            // (ปลูก, ใส่ปุ๋ย, พ่นยา, ตัดแต่ง, เก็บเกี่ยว -> ล้วนเน้นท่าทาง)
            else -> JobType.REBA
        }
    }

    val selectedBitmaps = remember { mutableStateListOf<Bitmap>() }

    // --- Options ---
    val durationOptions = listOf(
        stringResource(R.string.opt_dur_1h),
        stringResource(R.string.opt_dur_2h),
        stringResource(R.string.opt_dur_4h),
        stringResource(R.string.opt_dur_8h)
    )
    val frequencyOptions = listOf(
        stringResource(R.string.opt_freq_low),
        stringResource(R.string.opt_freq_med),
        stringResource(R.string.opt_freq_high)
    )
    val weightOptions = listOf(
        stringResource(R.string.opt_weight_5),
        stringResource(R.string.opt_weight_10),
        stringResource(R.string.opt_weight_15),
        stringResource(R.string.opt_weight_20),
        stringResource(R.string.opt_weight_max)
    )

    // REBA Options (Fully Localized)
    // ใช้ stringResource ดึงข้อความมาแสดงผล
    val trunkOptions = listOf(
        stringResource(R.string.opt_reba_trunk_1),
        stringResource(R.string.opt_reba_trunk_2),
        stringResource(R.string.opt_reba_trunk_3),
        stringResource(R.string.opt_reba_trunk_4)
    )

    val neckOptions = listOf(
        stringResource(R.string.opt_reba_neck_1),
        stringResource(R.string.opt_reba_neck_2)
    )

    val legOptions = listOf(
        stringResource(R.string.opt_reba_leg_1),
        stringResource(R.string.opt_reba_leg_2)
    )

    val upperArmOptions = listOf(
        stringResource(R.string.opt_reba_ua_1),
        stringResource(R.string.opt_reba_ua_2),
        stringResource(R.string.opt_reba_ua_3),
        stringResource(R.string.opt_reba_ua_4)
    )

    val loadScoreOptions = listOf(
        stringResource(R.string.opt_reba_load_0),
        stringResource(R.string.opt_reba_load_1),
        stringResource(R.string.opt_reba_load_2)
    )

    // --- State ---
    var selectedDuration by remember { mutableStateOf(durationOptions[0]) }
    var selectedFrequency by remember { mutableStateOf(frequencyOptions[0]) }
    var selectedWeight by remember { mutableStateOf(weightOptions[0]) }
    var expandedDuration by remember { mutableStateOf(false) }
    var expandedFrequency by remember { mutableStateOf(false) }
    var expandedWeight by remember { mutableStateOf(false) }

    // REBA State
    var rebaTrunk by remember { mutableStateOf(trunkOptions[0]) }
    var rebaNeck by remember { mutableStateOf(neckOptions[0]) }
    var rebaLeg by remember { mutableStateOf(legOptions[0]) }
    var rebaUpperArm by remember { mutableStateOf(upperArmOptions[0]) }
    var rebaLoad by remember { mutableStateOf(loadScoreOptions[0]) }
    var expTrunk by remember { mutableStateOf(false) }
    var expNeck by remember { mutableStateOf(false) }
    var expLeg by remember { mutableStateOf(false) }
    var expUpperArm by remember { mutableStateOf(false) }
    var expLoad by remember { mutableStateOf(false) }

    // Advanced Inputs
    var inputHorizontalDist by remember { mutableStateOf("25") }
    var inputVerticalHeight by remember { mutableStateOf("75") }
    var inputForceInitial by remember { mutableStateOf("0") }
    var inputForceSustain by remember { mutableStateOf("0") }

    var activeSlotIndex by remember { mutableIntStateOf(-1) }

    val permissionHelper = rememberPermissionHelper(
        onCameraCapture = { bitmap -> if (selectedBitmaps.size < 4) selectedBitmaps.add(bitmap) },
        onGallerySelection = { uri ->
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
            if (activeSlotIndex in 0 until 4) {
                if (activeSlotIndex < selectedBitmaps.size) selectedBitmaps[activeSlotIndex] = bitmap
                else selectedBitmaps.add(bitmap)
            } else if (selectedBitmaps.size < 4) {
                selectedBitmaps.add(bitmap)
            }
        }
    )

    LaunchedEffect(Unit) {
        analyticsManager.logScreenView("EvaluationFormScreen")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.eval_form_title), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(16.dp))

            // --- Images Section ---
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageSlotCard(selectedBitmaps.getOrNull(0), Modifier.weight(1f).height(120.dp), { activeSlotIndex = 0; permissionHelper.launchGallery() }, { selectedBitmaps.removeAt(0) })
                    ImageSlotCard(selectedBitmaps.getOrNull(1), Modifier.weight(1f).height(120.dp), { activeSlotIndex = 1; permissionHelper.launchGallery() }, { selectedBitmaps.removeAt(1) }, enabled = selectedBitmaps.isNotEmpty())
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageSlotCard(selectedBitmaps.getOrNull(2), Modifier.weight(1f).height(120.dp), { activeSlotIndex = 2; permissionHelper.launchGallery() }, { selectedBitmaps.removeAt(2) }, enabled = selectedBitmaps.size >= 2)
                    ImageSlotCard(selectedBitmaps.getOrNull(3), Modifier.weight(1f).height(120.dp), { activeSlotIndex = 3; permissionHelper.launchGallery() }, { selectedBitmaps.removeAt(3) }, enabled = selectedBitmaps.size >= 3)
                }
            }

            Spacer(Modifier.height(16.dp))
            val isFull = selectedBitmaps.size >= 4
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { permissionHelper.launchCamera() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C9A81)), enabled = !isFull) {
                    Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_camera_add))
                }
                OutlinedButton(onClick = { activeSlotIndex = -1; permissionHelper.launchGallery() }, modifier = Modifier.weight(1f), enabled = !isFull) {
                    Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_gallery))
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.form_section_info), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333), modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(12.dp))

            // --- Form Inputs ---
            when (currentJobType) {
                JobType.LIFTING -> {
                    SooktaDropdown(stringResource(R.string.label_duration), durationOptions, selectedDuration, { selectedDuration = it }, expandedDuration, { expandedDuration = it })
                    Spacer(Modifier.height(12.dp))
                    SooktaDropdown(stringResource(R.string.label_frequency), frequencyOptions, selectedFrequency, { selectedFrequency = it }, expandedFrequency, { expandedFrequency = it })
                    Spacer(Modifier.height(12.dp))
                    SooktaDropdown(stringResource(R.string.label_weight), weightOptions, selectedWeight, { selectedWeight = it }, expandedWeight, { expandedWeight = it })
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.form_section_advanced), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = inputHorizontalDist, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) inputHorizontalDist = it }, label = { Text(stringResource(R.string.label_dist_h)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF5C9A81),
                            cursorColor = Color(0xFF5C9A81)
                        ))
                        OutlinedTextField(value = inputVerticalHeight, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) inputVerticalHeight = it }, label = { Text(stringResource(R.string.label_dist_v)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF5C9A81),
                            cursorColor = Color(0xFF5C9A81)
                        ))
                    }
                }
                JobType.PUSH_PULL -> {
                    SooktaDropdown(stringResource(R.string.label_duration), durationOptions, selectedDuration, { selectedDuration = it }, expandedDuration, { expandedDuration = it })
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.form_section_force), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = inputForceInitial, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) inputForceInitial = it }, label = { Text(stringResource(R.string.label_force_initial)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF5C9A81),
                            cursorColor = Color(0xFF5C9A81)
                        ))
                        OutlinedTextField(value = inputForceSustain, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) inputForceSustain = it }, label = { Text(stringResource(R.string.label_force_sustain)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF5C9A81),
                            cursorColor = Color(0xFF5C9A81)
                        ))
                    }
                }
                JobType.REBA -> {
                    Text(stringResource(R.string.form_section_reba_trunk), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    SooktaDropdown(stringResource(R.string.reba_trunk), trunkOptions, rebaTrunk, { rebaTrunk = it }, expTrunk, { expTrunk = it })
                    Spacer(Modifier.height(8.dp))
                    SooktaDropdown(stringResource(R.string.reba_neck), neckOptions, rebaNeck, { rebaNeck = it }, expNeck, { expNeck = it })
                    Spacer(Modifier.height(8.dp))
                    SooktaDropdown(stringResource(R.string.reba_legs), legOptions, rebaLeg, { rebaLeg = it }, expLeg, { expLeg = it })
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.form_section_reba_arm), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    SooktaDropdown(stringResource(R.string.reba_upper_arm), upperArmOptions, rebaUpperArm, { rebaUpperArm = it }, expUpperArm, { expUpperArm = it })
                    Spacer(Modifier.height(8.dp))
                    SooktaDropdown(stringResource(R.string.reba_load), loadScoreOptions, rebaLoad, { rebaLoad = it }, expLoad, { expLoad = it })
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- START ANALYZE BUTTON ---
            Button(
                onClick = {
                    val result: ErgoResult
                    val inputData: Any
                    analyticsManager.logAction("click_analyze", label = currentJobType.name)
                    if (currentJobType == JobType.REBA) {

                        var maxAiTrunk = 1
                        var maxAiNeck = 1
                        var maxAiUpperArm = 1
                        val maxAiLeg = 1

                        if (selectedBitmaps.isNotEmpty()) {
                            selectedBitmaps.forEach { bitmap ->
                                // ให้ AI วิเคราะห์ทีละรูป
                                val aiResult = poseHelper.estimatePoseAndGetReba(bitmap, userDailyIncome)

                                // เปรียบเทียบหาค่าความเสี่ยงสูงสุด (Worst Case) เก็บไว้
                                if (aiResult.trunkScore > maxAiTrunk) maxAiTrunk = aiResult.trunkScore
                                if (aiResult.neckScore > maxAiNeck) maxAiNeck = aiResult.neckScore
                                if (aiResult.upperArmScore > maxAiUpperArm) maxAiUpperArm = aiResult.upperArmScore
                                // (Leg AI ยังไม่เก่ง อาจจะข้ามไป หรือใช้ logic เดียวกันก็ได้)
                                // if (aiResult.legScore > maxAiLeg) maxAiLeg = aiResult.legScore
                            }
                        }

                        // 2. ผสมค่าจาก AI (Max) กับค่าจาก Dropdown (User Input)
                        // Logic: เลือกค่าที่ "มากกว่า" ระหว่าง Dropdown กับ AI (Safety First)
                        // แต่ถ้า User ตั้งใจเลือกค่าต่ำ (Dropdown) เราอาจจะยอมให้ User Override ได้
                        // ในที่นี้ใช้ maxOf เพื่อความปลอดภัย (ถ้า AI เห็นว่าเสี่ยง ให้ถือว่าเสี่ยง)
                        val mergedInput = RebaInputData(
                            dailyIncome = userDailyIncome,

                            // เทียบค่า User เลือก vs ค่าสูงสุดที่ AI เจอ
                            trunkScore = maxOf(trunkOptions.indexOf(rebaTrunk) + 1, maxAiTrunk),
                            neckScore = maxOf(neckOptions.indexOf(rebaNeck) + 1, maxAiNeck),
                            upperArmScore = maxOf(upperArmOptions.indexOf(rebaUpperArm) + 1, maxAiUpperArm),
                            legScore = maxOf(legOptions.indexOf(rebaLeg) + 1, maxAiLeg), // หรือใช้ค่าจาก Dropdown อย่างเดียวถ้าไม่มั่นใจ AI ขา

                            // ส่วนที่ AI ดูไม่ได้ ต้องเชื่อ User 100%
                            loadScore = loadScoreOptions.indexOf(rebaLoad),

                            // ค่า Default อื่นๆ
                            lowerArmScore = 1,
                            wristScore = 1,
                            couplingScore = 0,
                            activityScore = 0
                        )
                        inputData = mergedInput
                        result = ErgoCalculatorHelper.calculateRebaRisk(mergedInput)
                    } else {
                        // ISO 11228 Logic
                        val weightVal = when (selectedWeight) {
                            weightOptions[0] -> 5.0
                            weightOptions[1] -> 10.0
                            weightOptions[2] -> 15.0
                            weightOptions[3] -> 20.0
                            else -> 25.0
                        }
                        val freqVal = if (selectedFrequency.contains("<")) 0.2 else if (selectedFrequency.contains("1-4")) 2.0 else 6.0
                        val durationVal = if (selectedDuration.contains("1")) 1.0 else if (selectedDuration.contains("2")) 2.0 else if (selectedDuration.contains("4")) 4.0 else 8.0

                        // [Updated] ส่ง dailyIncome เข้าไปใน ErgoInputData
                        val ergoInput = ErgoInputData(
                            jobType = currentJobType,
                            gender = userPrefState?.gender ?: "male",
                            dailyIncome = userDailyIncome, // <-- สำคัญ
                            loadWeight = weightVal,
                            horizontalDist = inputHorizontalDist.toDoubleOrNull() ?: 25.0,
                            verticalHeight = inputVerticalHeight.toDoubleOrNull() ?: 75.0,
                            liftFrequency = freqVal,
                            durationHours = durationVal,
                            initialForce = inputForceInitial.toDoubleOrNull() ?: 0.0,
                            sustainForce = inputForceSustain.toDoubleOrNull() ?: 0.0
                        )
                        inputData = ergoInput

                        result = if (currentJobType == JobType.LIFTING) {
                            ErgoCalculatorHelper.calculateLiftingRisk(ergoInput)
                        } else {
                            ErgoCalculatorHelper.calculatePushPullRisk(ergoInput)
                        }
                    }

                    // 3. Navigate with Results
                    // ส่ง Object ทั้งก้อนไปเลย ปลายทางจะได้รับครบทุกฟิลด์ (userScore, economicLoss)
                    navController.currentBackStackEntry?.savedStateHandle?.set("riskResult", result)
                    navController.currentBackStackEntry?.savedStateHandle?.set("inputData", inputData)

                    // เปลี่ยน Route Argument เป็น userScore แทน score เดิม (เพื่อให้ url ดูง่ายขึ้น)
                    navController.navigate("initial_risk/$activityNameArg/${result.userScore}")
                },
                enabled = selectedBitmaps.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_start_analyze), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(50.dp))
        }
    }
}

// ... Helper Composables (ImageSlotCard, SooktaDropdown) เหมือนเดิม ...
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
            Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(Color.Red.copy(0.8f)).clickable { onDeleteClick() }, Alignment.Center) {
                Icon(Icons.Default.Close, "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        } else if (enabled) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray)
                Text(stringResource(R.string.form_add_photo), fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            Icon(Icons.Default.Image, null, tint = Color.Gray.copy(0.5f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SooktaDropdown(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit, expanded: Boolean, onExpandedChange: (Boolean) -> Unit) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(

                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = Color(0xFF5C9A81),
                unfocusedBorderColor = Color.LightGray
            ),
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(option); onExpandedChange(false) }) }
        }
    }
}