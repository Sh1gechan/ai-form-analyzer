package com.sportanalyzer.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sportanalyzer.app.ui.MainViewModel
import com.sportanalyzer.app.ui.screens.analysis.AnalysisScreen
import com.sportanalyzer.app.ui.screens.camera.CameraScreen
import com.sportanalyzer.app.ui.screens.history.HistoryDetailScreen
import com.sportanalyzer.app.ui.screens.history.HistoryScreen
import com.sportanalyzer.app.ui.screens.home.HomeScreen
import com.sportanalyzer.app.ui.screens.results.ResultsScreen
import com.sportanalyzer.app.ui.screens.settings.SettingsScreen
import com.sportanalyzer.app.ui.screens.summary.SummaryScreen
import com.sportanalyzer.app.ui.theme.*

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem(Screen.Home.route,     "ホーム",  Icons.Default.Home),
    TabItem(Screen.History.route,  "記録",    Icons.Default.History),
    TabItem(Screen.Settings.route, "設定",    Icons.Default.Settings)
)

// ボトムバーを表示しないルート
private val fullScreenRoutes = setOf(
    "camera",
    "analysis",
    "summary",
    "results",
    "history_detail"
)

@Composable
fun AppNavigation() {
    val navController  = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route

    val showBottomBar = currentRoute != null &&
        fullScreenRoutes.none { prefix -> currentRoute.startsWith(prefix) }

    val mainViewModel: MainViewModel = hiltViewModel()

    Scaffold(
        containerColor = SystemBlack,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SystemDark,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEach { tab ->
                        val selected = backStackEntry?.destination
                            ?.hierarchy?.any { it.route == tab.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (selected) iOSBlue else SecondaryLabel
                                )
                            },
                            label = {
                                Text(
                                    text     = tab.label,
                                    fontSize = 10.sp,
                                    color    = if (selected) iOSBlue else SecondaryLabel
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = iOSBlue,
                                unselectedIconColor = SecondaryLabel,
                                selectedTextColor   = iOSBlue,
                                unselectedTextColor = SecondaryLabel,
                                indicatorColor      = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController = navController, viewModel = mainViewModel)
            }

            composable(Screen.History.route) {
                HistoryScreen(navController = navController, viewModel = mainViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController, viewModel = mainViewModel)
            }

            composable("camera") {
                CameraScreen(navController = navController)
            }

            composable(
                route     = Screen.Analysis.route,
                arguments = Screen.Analysis.arguments
            ) { entry ->
                AnalysisScreen(
                    navController = navController,
                    videoUri      = entry.arguments?.getString("videoUri") ?: "",
                    viewModel     = mainViewModel
                )
            }

            composable(
                route     = Screen.Summary.route,
                arguments = Screen.Summary.arguments
            ) { entry ->
                SummaryScreen(
                    navController = navController,
                    analysisId    = entry.arguments?.getString("analysisId") ?: "",
                    viewModel     = mainViewModel
                )
            }

            composable(
                route     = Screen.Results.route,
                arguments = Screen.Results.arguments
            ) { entry ->
                ResultsScreen(
                    navController = navController,
                    analysisId    = entry.arguments?.getString("analysisId") ?: "",
                    viewModel     = mainViewModel
                )
            }

            composable(
                route     = Screen.HistoryDetail.route,
                arguments = Screen.HistoryDetail.arguments
            ) { entry ->
                HistoryDetailScreen(
                    navController = navController,
                    recordId      = entry.arguments?.getString("recordId") ?: "",
                    viewModel     = mainViewModel
                )
            }
        }
    }
}
