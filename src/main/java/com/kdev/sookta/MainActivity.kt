package com.kdev.sookta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kdev.sookta.ui.screen.main.EvaluationFormScreen
import com.kdev.sookta.ui.screen.main.MainScreen
import com.kdev.sookta.ui.screen.main.ResultScreen
import com.kdev.sookta.ui.screen.onboarding.AvatarSelectionScreen
import com.kdev.sookta.ui.screen.onboarding.LanguageSelectionScreen
import com.kdev.sookta.ui.screen.onboarding.LoginScreen
import com.kdev.sookta.ui.screen.onboarding.OTPScreen
import com.kdev.sookta.ui.screen.onboarding.SplashScreen
import com.kdev.sookta.ui.theme.SooktaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SooktaTheme {
                AppNavigation()
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
        composable("login") { LoginScreen(navController) }
        composable("otp") { OTPScreen(navController) }
        composable("avatar_selection") { AvatarSelectionScreen(navController) }

        // --- Main App Flow ---
        composable("main") { MainScreen() }

        // --- Standalone screens outside main navigation ---
        // These are pages you navigate to from within the main flow
        composable("evaluation_form/{activityName}") { backStackEntry ->
            val activityName = backStackEntry.arguments?.getString("activityName") ?: "Unknown"
            EvaluationFormScreen(navController, activityName)
        }
        composable("result") { ResultScreen(navController) }
    }
}