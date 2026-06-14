package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.service.MutedNotificationListenerService
import com.example.ui.LauncherViewModel
import com.example.ui.components.LauncherBackBar
import com.example.usage.UsageStatsRepository
import com.example.util.LauncherUtils
import com.example.ui.theme.launcherBackground
import com.example.ui.theme.launcherDialogContainer
import com.example.ui.theme.launcherMuted
import com.example.ui.theme.launcherOnBackground
import com.example.ui.theme.launcherSecondary

@Composable
fun OnboardingScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().background(launcherBackground()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("minimal", color = launcherOnBackground(), fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        Text("a calm, text-only home screen to reduce mindless scrolling.", color = launcherSecondary(), fontSize = 16.sp, modifier = Modifier.padding(bottom = 32.dp))
        Text("set as default launcher", color = launcherOnBackground(), fontSize = 16.sp, modifier = Modifier.clickable { LauncherUtils.requestDefaultLauncher(context) }.padding(vertical = 14.dp))
        Text("grant usage access", color = launcherOnBackground(), fontSize = 16.sp, modifier = Modifier.clickable {
            UsageStatsRepository(context, com.example.data.LauncherDatabase.getDatabase(context).launcherDao()).openUsageAccessSettings()
        }.padding(vertical = 14.dp))
        Text("continue", color = launcherBackground(), fontSize = 16.sp, modifier = Modifier.fillMaxWidth().background(launcherOnBackground()).clickable { viewModel.completeOnboarding(context) }.padding(vertical = 16.dp))
    }
}

@Composable
fun SystemSetupScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val usageRepo = UsageStatsRepository(context, com.example.data.LauncherDatabase.getDatabase(context).launcherDao())
    val adbCmd = "adb shell pm grant ${BuildConfig.APPLICATION_ID} android.permission.WRITE_SECURE_SETTINGS"

    Column(modifier = Modifier.fillMaxSize().background(launcherBackground()).padding(24.dp)) {
        Text("permissions setup", color = launcherOnBackground(), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

        SetupRow("default launcher", if (LauncherUtils.isDefaultLauncher(context)) "[active]" else "[needs setup]") {
            LauncherUtils.requestDefaultLauncher(context)
        }
        SetupRow("usage access", if (usageRepo.hasUsageAccess()) "[active]" else "[needs setup]") {
            usageRepo.openUsageAccessSettings()
        }
        SetupRow(
            label = "secure settings (dark schema)",
            status = if (viewModel.hasSecureSettings) "[active]" else "[needs adb grant]",
            adbHint = adbCmd
        )
        SetupRow("notification listener", if (MutedNotificationListenerService.isEnabled(context)) "[active]" else "[needs setup]") {
            MutedNotificationListenerService.openSettings(context)
        }
        SetupRow("device admin (double-tap lock)", if (LauncherUtils.isDeviceAdminActive(context)) "[active]" else "[needs setup]") {
            LauncherUtils.requestDeviceAdmin(context)
        }
        if (!LauncherUtils.isDeviceAdminActive(context)) {
            Text("device admin adb:", color = launcherSecondary(), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Text(LauncherUtils.deviceAdminAdbCommand(), color = launcherSecondary(), fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        SetupRow("lock screen wallpaper", "[tap to re-apply]") {
            viewModel.applyLockWallpaperNow(context)
        }
        Text("adb command:", color = launcherSecondary(), fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
        Text(adbCmd, color = launcherSecondary(), fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))

        LauncherBackBar(onBack = { viewModel.goHome() })
    }
}

@Composable
private fun SetupRow(label: String, status: String, adbHint: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(label, color = launcherOnBackground(), fontSize = 16.sp)
            if (adbHint != null && status.contains("needs")) {
                Text(adbHint, color = launcherMuted(), fontSize = 10.sp)
            }
        }
        Text(status, color = launcherSecondary(), fontSize = 14.sp)
    }
}

@Composable
fun NotificationsScreen(viewModel: LauncherViewModel) {
    val notifications by viewModel.mutedNotifications.collectAsState()
    Column(modifier = Modifier.fillMaxSize().background(launcherBackground()).padding(24.dp)) {
        Text("notifications", color = launcherOnBackground(), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        if (notifications.isEmpty()) {
            Text("no captured notifications", color = launcherSecondary(), modifier = Modifier.padding(vertical = 16.dp))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(notifications) { notif ->
                    Column(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.markNotificationRead(notif.id) }.padding(vertical = 14.dp)
                    ) {
                        Text(notif.title.ifEmpty { notif.packageName }, color = if (notif.isRead) launcherSecondary() else launcherOnBackground(), fontSize = 16.sp)
                        Text(notif.text, color = launcherSecondary(), fontSize = 14.sp)
                    }
                }
            }
        }
        LauncherBackBar(onBack = { viewModel.goHome() })
    }
}

@Composable
fun ShortcutPickerScreen(viewModel: LauncherViewModel) {
    val allApps by viewModel.allAppConfigs.collectAsState()
    Column(modifier = Modifier.fillMaxSize().background(launcherBackground()).padding(24.dp)) {
        Text("assign time ring shortcut", color = launcherOnBackground(), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(allApps) { app ->
                Text(
                    app.appName.lowercase(),
                    color = launcherOnBackground(),
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.assignShortcut(app.appName, app.packageName)
                    }.padding(vertical = 14.dp)
                )
            }
        }
        Text("< cancel", color = launcherSecondary(), modifier = Modifier.clickable { viewModel.goHome() }.padding(vertical = 16.dp))
    }
}
