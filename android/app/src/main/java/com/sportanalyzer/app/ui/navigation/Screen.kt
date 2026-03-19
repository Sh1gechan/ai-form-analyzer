package com.sportanalyzer.app.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList()
) {
    object Home    : Screen("home")
    object History : Screen("history")
    object Settings: Screen("settings")

    object Analysis : Screen(
        route = "analysis/{videoUri}",
        arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
    ) {
        fun createRoute(videoUri: String) = "analysis/$videoUri"
    }

    object Results : Screen(
        route = "results/{analysisId}",
        arguments = listOf(navArgument("analysisId") { type = NavType.StringType })
    ) {
        fun createRoute(analysisId: String) = "results/$analysisId"
    }

    object Summary : Screen(
        route = "summary/{analysisId}",
        arguments = listOf(navArgument("analysisId") { type = NavType.StringType })
    ) {
        fun createRoute(analysisId: String) = "summary/$analysisId"
    }

    object HistoryDetail : Screen(
        route = "history_detail/{recordId}",
        arguments = listOf(navArgument("recordId") { type = NavType.StringType })
    ) {
        fun createRoute(id: String) = "history_detail/$id"
    }
}
