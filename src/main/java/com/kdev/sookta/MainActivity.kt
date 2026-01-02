package com.kdev.sookta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kdev.sookta.ui.screen.SetupScreen
import com.kdev.sookta.ui.screen.main.ContactScreen
import com.kdev.sookta.ui.screen.main.EvaluationFormScreen
import com.kdev.sookta.ui.screen.main.EvaluationMenuScreen
import com.kdev.sookta.ui.screen.main.FinalResultScreen
import com.kdev.sookta.ui.screen.main.HelpScreen
import com.kdev.sookta.ui.screen.main.InitialRiskScreen
import com.kdev.sookta.ui.screen.main.MainScreen
import com.kdev.sookta.ui.screen.main.TermsScreen
import com.kdev.sookta.ui.screen.onboarding.AvatarSelectionScreen
import com.kdev.sookta.ui.screen.onboarding.LanguageSelectionScreen
import com.kdev.sookta.ui.screen.onboarding.SplashScreen
import com.kdev.sookta.ui.theme.SooktaTheme

import android.content.Context
import com.kdev.sookta.ui.screen.main.HistoryScreen
import com.kdev.sookta.utils.LocaleHelper

class MainActivity : ComponentActivity() {

    // 1. เพิ่มฟังก์ชันนี้เพื่อดักเปลี่ยนภาษาตั้งแต่ระดับ Base Context
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Box(modifier = Modifier.fillMaxSize()) {

                // 1. เลเยอร์พื้นหลัง (อยู่ล่างสุด)
                Image(
                    painter = painterResource(id = R.drawable.app_background),
                    contentDescription = "App Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // เพื่อให้ภาพเต็มจอโดยไม่เสียสัดส่วน
                )

                // 2. เลเยอร์เนื้อหาแอป (อยู่ด้านบน)
                SooktaTheme {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        // --- Onboarding Flow ---
        composable("splash") { SplashScreen(navController) }
        composable("language_selection") { LanguageSelectionScreen(navController) }
        composable("setup") { SetupScreen(navController) }
        composable("avatar_selection") { AvatarSelectionScreen(navController) }
        composable("main") { MainScreen(navController) }
        composable("evaluation_menu") { EvaluationMenuScreen(navController) }

        composable("evaluation_form/{activityName}") { backStackEntry ->
            val activityName = backStackEntry.arguments?.getString("activityName") ?: "General"
            EvaluationFormScreen(navController, activityName)
        }
        composable("initial_risk/{activityName}/{score}") { backStackEntry ->
            val activityName = backStackEntry.arguments?.getString("activityName") ?: "-"
            val score = backStackEntry.arguments?.getString("score")
            InitialRiskScreen(navController, activityNameArg = activityName, initialScoreArg = score)
        }
        composable("final_result_screen") {
            FinalResultScreen(navController)
        }

        composable("terms") { TermsScreen(navController) }
        composable("help") { HelpScreen(navController) }
        composable("contact") { ContactScreen(navController) }
        composable("history") { HistoryScreen(navController) }
    }
}