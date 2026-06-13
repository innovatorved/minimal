package com.example.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Stack

class LauncherNavigator(initialScreen: AppScreen) {

    private val _currentScreen = MutableStateFlow(initialScreen)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _navigationDirection = MutableStateFlow(NavigationDirection.Forward)
    val navigationDirection: StateFlow<NavigationDirection> = _navigationDirection.asStateFlow()

    private val screenHistory = Stack<AppScreen>()

    fun navigateTo(screen: AppScreen, clearHistory: Boolean = false, onLeaveWorkProfile: () -> Unit = {}) {
        if (_currentScreen.value == AppScreen.WorkProfile && screen != AppScreen.WorkProfile) {
            onLeaveWorkProfile()
        }
        _navigationDirection.value = NavigationDirection.Forward
        if (clearHistory) screenHistory.clear()
        else screenHistory.push(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun goHome(onLeaveWorkProfile: () -> Unit = {}) {
        if (_currentScreen.value == AppScreen.Home) return
        onLeaveWorkProfile()
        _navigationDirection.value = NavigationDirection.Back
        screenHistory.clear()
        _currentScreen.value = AppScreen.Home
    }

    fun openDrawer() {
        if (_currentScreen.value == AppScreen.Home) {
            navigateTo(AppScreen.AppDrawer)
        }
    }

    /** Pixel swipe up: home opens drawer; every other screen returns home. */
    fun onSwipeUp(onLeaveWorkProfile: () -> Unit = {}) {
        when (_currentScreen.value) {
            AppScreen.Home -> openDrawer()
            AppScreen.Onboarding -> Unit
            else -> goHome(onLeaveWorkProfile)
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }
}
