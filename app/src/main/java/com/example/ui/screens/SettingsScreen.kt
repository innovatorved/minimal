package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.MutedNotificationListenerService
import com.example.ui.LauncherViewModel
import com.example.ui.navigation.AppScreen
import com.example.ui.components.LauncherBackBar
import com.example.ui.components.MinimalToggle
import com.example.util.LauncherUtils

@Composable
fun SettingsScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val is24h by viewModel.is24HourFormat.collectAsState()
    val isWallpaperMatching by viewModel.isWallpaperMatchingEnabled.collectAsState()
    val isSystemDarkSchema by viewModel.isSystemDarkSchemaEnabled.collectAsState()
    val hasSecureSettings = viewModel.hasSecureSettings
    val adbCmd = viewModel.adbGrantCommand
    val deviceAdminAdb = LauncherUtils.deviceAdminAdbCommand()
    val isDeviceAdmin = LauncherUtils.isDeviceAdminActive(context)
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val renamedApps by viewModel.renamedApps.collectAsState()
    val shortcutAppName by viewModel.shortcutAppName.collectAsState()
    val dailyGoal by viewModel.dailyGoalMinutes.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val hasWorkProfile by viewModel.hasWorkProfile.collectAsState()
    val isScheduledBlocking by viewModel.isScheduledBlockingEnabled.collectAsState()
    var showShortcutPicker by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    val allApps by viewModel.allAppConfigs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshInsights()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text("settings", color = Color.LightGray, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 12.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text("lock screen", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                MinimalToggle(
                    label = "matching lock screen wallpaper",
                    isOn = isWallpaperMatching,
                    onToggle = { viewModel.toggleWallpaperMatching(context) }
                )
                Text(
                    text = "sync minimal lock screen",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { viewModel.applyLockWallpaperNow(context) }
                        .padding(vertical = 14.dp)
                )
                Text(
                    text = "customize pixel lock screen",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { viewModel.openLockScreenSettings(context) }
                        .padding(vertical = 14.dp)
                )
                Text(
                    text = "lock screen uses pixel system clock on plain black wallpaper",
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "lock phone now",
                    color = if (isDeviceAdmin) Color.White else Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { viewModel.lockScreen(context) }
                        .padding(vertical = 14.dp)
                )
                Text(
                    text = if (isDeviceAdmin) "double-tap home screen also locks" else "device admin required — enable below",
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "enable device admin",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { LauncherUtils.requestDeviceAdmin(context) }
                        .padding(vertical = 14.dp)
                )
                if (!isDeviceAdmin) {
                    Text(deviceAdminAdb, color = Color.DarkGray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 12.dp))
                }
            }
            item {
                Text("display", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                MinimalToggle(
                    label = "dark schema for entire phone",
                    isOn = isSystemDarkSchema,
                    onToggle = { viewModel.toggleSystemDarkSchema(context) }
                )
                Text(
                    text = "desaturates all apps (grayscale) and forces system dark mode",
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (!hasSecureSettings) {
                    Text(
                        text = "requires adb permission — tap system permissions setup",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(AppScreen.SystemSetup) }
                            .padding(bottom = 8.dp)
                    )
                    Text(adbCmd, color = Color.DarkGray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 12.dp))
                }
                MinimalToggle("time format (24h)", is24h, onToggle = { viewModel.toggle24HourFormat() })
                Text(
                    text = "restore phone colors",
                    color = Color.LightGray,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { viewModel.restoreAppearanceDefaults(context) }.padding(vertical = 14.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showGoalDialog = true }.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("daily screen time goal", color = Color.White, fontSize = 16.sp)
                    Text("$dailyGoal min", color = Color.LightGray, fontSize = 16.sp)
                }
            }
            item {
                Text("insights", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                InsightsPreviewCard(
                    insights = insights,
                    hasUsageAccess = hasUsageAccess,
                    onOpenInsights = { viewModel.navigateTo(AppScreen.Insights) },
                    onGrantUsageAccess = { viewModel.openUsageAccessSettings(context) }
                )
                Text(
                    "all data stays on this device",
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            item {
                Text("permissions", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(AppScreen.SystemSetup) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("system permissions setup", color = Color.White, fontSize = 16.sp)
                    Text(if (hasSecureSettings && isDeviceAdmin) "[ready]" else "[setup]", color = Color.LightGray)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openUsageAccessSettings(context) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("usage access", color = Color.White, fontSize = 16.sp)
                    Text(if (hasUsageAccess) "[active]" else "[needs setup]", color = Color.LightGray)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { MutedNotificationListenerService.openSettings(context) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("notification listener", color = Color.White, fontSize = 16.sp)
                    Text(
                        if (MutedNotificationListenerService.isEnabled(context)) "[active]" else "[needs setup]",
                        color = Color.LightGray
                    )
                }
            }
            item {
                Text("apps", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                if (hasWorkProfile) {
                    Text(
                        "work profile",
                        color = Color.White,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(AppScreen.WorkProfile) }
                            .padding(vertical = 14.dp)
                    )
                }
                Text("widgets", color = Color.White, modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.Widgets) }.padding(vertical = 14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(AppScreen.Focus) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("focus & app limits", color = Color.White, fontSize = 16.sp)
                    Text(if (isScheduledBlocking) "[scheduled on]" else "[open]", color = Color.LightGray)
                }
                Text(
                    "notifications dashboard",
                    color = Color.White,
                    modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.Notifications) }.padding(vertical = 14.dp)
                )
                if (favoriteApps.isNotEmpty()) {
                    Text("pinned home apps", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                    favoriteApps.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(app.appName.lowercase(), color = Color.White)
                            Text("unpin", color = Color.LightGray, modifier = Modifier.clickable { viewModel.unpinApp(app) })
                        }
                    }
                }
                renamedApps.forEach { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.resetAppLabel(app) }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(app.appName.lowercase(), color = Color.White)
                        Text("[reset]", color = Color.LightGray)
                    }
                }
            }
            item {
                Text("shortcuts", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showShortcutPicker = true }.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("time ring long-press", color = Color.White)
                    Text(shortcutAppName?.lowercase() ?: "[not assigned]", color = Color.LightGray)
                }
                if (shortcutAppName != null) {
                    Text("clear shortcut", color = Color.LightGray, modifier = Modifier.clickable { viewModel.clearShortcut() }.padding(vertical = 10.dp))
                }
            }
        }
        LauncherBackBar(onBack = { viewModel.goHome() }, label = "< back")
    }

    if (showShortcutPicker) {
        AlertDialog(
            onDismissRequest = { showShortcutPicker = false },
            containerColor = Color.Black,
            title = { Text("choose shortcut app", color = Color.White) },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(allApps) { app ->
                        Text(app.appName.lowercase(), color = Color.White, modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.assignShortcut(app.appName, app.packageName)
                            showShortcutPicker = false
                        }.padding(vertical = 14.dp))
                    }
                }
            },
            confirmButton = { Text("close", color = Color.White, modifier = Modifier.clickable { showShortcutPicker = false }.padding(16.dp)) }
        )
    }

    if (showGoalDialog) {
        var goalText by remember { mutableStateOf(dailyGoal.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            containerColor = Color.Black,
            title = { Text("daily goal (minutes)", color = Color.White) },
            text = {
                androidx.compose.foundation.text.BasicTextField(
                    value = goalText,
                    onValueChange = { goalText = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White)
                )
            },
            confirmButton = {
                Text("save", color = Color.White, modifier = Modifier.clickable {
                    goalText.toIntOrNull()?.let { viewModel.setDailyGoalMinutes(it) }
                    showGoalDialog = false
                }.padding(16.dp))
            },
            dismissButton = { Text("cancel", color = Color.LightGray, modifier = Modifier.clickable { showGoalDialog = false }.padding(16.dp)) }
        )
    }
}
