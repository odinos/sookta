package com.kdev.sookta.ui.screen.onboarding


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kdev.sookta.R
import com.kdev.sookta.ui.theme.LightBrown
import kotlinx.coroutines.delay


@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(key1 = true) {
        delay(2000L)
        // Navigate to the new first screen: Language Selection
        navController.navigate("language_selection") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(color = LightBrown)
    ) {
        Image(
            painter = painterResource(id = R.drawable.sookta_logo),
            contentDescription = "SookTa Logo",
            modifier = Modifier.width(250.dp)
        )
    }
}
