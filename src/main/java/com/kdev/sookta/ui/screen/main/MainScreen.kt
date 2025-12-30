package com.kdev.sookta.ui.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kdev.sookta.ui.theme.SooktaGreen


data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun MainScreen(rootNavController: NavController) {
    val mainNavController = rememberNavController()

    val navItems = listOf(
        BottomNavItem("หน้าแรก", Icons.Default.Home, "home"),
        BottomNavItem("ผลตรวจ", Icons.Default.History, "history"),
        BottomNavItem("โปรไฟล์", Icons.Default.AccountCircle, "profile")
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                contentColor = Color.White,
                containerColor = SooktaGreen
            ) {
                val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            mainNavController.navigate(item.route) {
                                popUpTo(mainNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(item.label) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.White,
                            selectedTextColor = Color.White,
                            selectedIconColor = SooktaGreen,
                            unselectedIconColor = Color.White,
                            unselectedTextColor = Color.White
                        )
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        // This NavHost controls the content area ABOVE the bottom bar
        NavHost(
            navController = mainNavController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { HomeScreen(rootNavController) }
            composable("history") { HistoryScreen(mainNavController) }
            composable("profile") { ProfileScreen(mainNavController) }
        }
    }
}


