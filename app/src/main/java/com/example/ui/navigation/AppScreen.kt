package com.example.ui.navigation

sealed interface AppScreen {
    object Home : AppScreen
    object AppDrawer : AppScreen
    object Focus : AppScreen
    object Widgets : AppScreen
    object Settings : AppScreen
    object SystemSetup : AppScreen
    object Notifications : AppScreen
    object Onboarding : AppScreen
    object ShortcutPicker : AppScreen
    object Insights : AppScreen
    object WorkProfile : AppScreen
    data class MindfulLaunch(
        val packageName: String,
        val appName: String,
        val limitMinutes: Int,
        val isScheduled: Boolean = false
    ) : AppScreen
    data class BlockedScreen(val appName: String, val blockedUntil: String) : AppScreen
}

enum class NavigationDirection {
    Forward,
    Back
}
