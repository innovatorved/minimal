package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LauncherViewModel
import com.example.ui.launcherScreenTransition
import com.example.ui.navigation.AppScreen
import com.example.ui.components.launcherSwipeBack
import com.example.ui.components.pixelBottomSwipeHome
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_SHOW_BREATHING = "extra_show_breathing"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
        setContent {
            MyApplicationTheme {
                val viewModel: LauncherViewModel = viewModel()
                if (intent.getBooleanExtra(EXTRA_SHOW_BREATHING, false)) {
                    DisposableEffect(Unit) {
                        viewModel.triggerBreathingPause()
                        onDispose {}
                    }
                }
                LauncherContainer(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun LauncherContainer(viewModel: LauncherViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val navigationDirection by viewModel.navigationDirection.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val canSwipeBack = currentScreen != AppScreen.Home &&
        currentScreen != AppScreen.Onboarding &&
        currentScreen != AppScreen.AppDrawer
    // Home → drawer locally. AppDrawer scrolls freely. Other screens: bottom-edge swipe only.
    val bottomSwipeHomeEnabled = currentScreen != AppScreen.Onboarding &&
        currentScreen != AppScreen.AppDrawer &&
        currentScreen != AppScreen.Home

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = currentScreen != AppScreen.Home && currentScreen != AppScreen.Onboarding) {
        viewModel.goHome()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pixelBottomSwipeHome(
                    enabled = bottomSwipeHomeEnabled,
                    onSwipeHome = { viewModel.goHome() }
                )
                .launcherSwipeBack(enabled = canSwipeBack, onBack = { viewModel.goHome() })
        ) {
            AnimatedContent(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                targetState = currentScreen,
                transitionSpec = { launcherScreenTransition(navigationDirection) },
                label = "launcher_screen"
            ) { screen ->
                when (screen) {
                    is AppScreen.Onboarding -> OnboardingScreen(viewModel)
                    is AppScreen.Home -> HomeScreen(viewModel)
                    is AppScreen.AppDrawer -> AppDrawerScreen(viewModel)
                    is AppScreen.Focus -> FocusScreen(viewModel)
                    is AppScreen.Widgets -> WidgetsScreen(viewModel)
                    is AppScreen.Settings -> SettingsScreen(viewModel)
                    is AppScreen.Insights -> InsightsScreen(viewModel)
                    is AppScreen.SystemSetup -> SystemSetupScreen(viewModel)
                    is AppScreen.Notifications -> NotificationsScreen(viewModel)
                    is AppScreen.ShortcutPicker -> ShortcutPickerScreen(viewModel)
                    is AppScreen.MindfulLaunch -> MindfulLaunchScreen(
                        viewModel = viewModel,
                        packageName = screen.packageName,
                        appName = screen.appName,
                        limitMinutes = screen.limitMinutes
                    )
                    is AppScreen.BlockedScreen -> BlockedScreen(
                        viewModel = viewModel,
                        appName = screen.appName,
                        blockedUntil = screen.blockedUntil
                    )
                    is AppScreen.WorkProfile -> WorkProfileScreen(viewModel)
                }
            }
        }
    }
}
