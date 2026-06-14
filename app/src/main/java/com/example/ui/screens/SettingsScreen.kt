package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.service.MutedNotificationListenerService
import com.example.ui.LauncherViewModel
import com.example.ui.navigation.AppScreen
import com.example.ui.components.AppPickerDialog
import com.example.ui.components.LauncherBackBar
import com.example.ui.components.MinimalToggle
import com.example.ui.components.ThemeColorPickerGrouped
import com.example.ui.components.SelectedThemeLabel
import com.example.ui.theme.launcherBackground
import com.example.ui.theme.launcherDialogContainer
import com.example.ui.theme.launcherMuted
import com.example.ui.theme.launcherOnBackground
import com.example.ui.theme.launcherSecondary
import com.example.util.LauncherUtils

@Composable
fun SettingsScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val is24h by viewModel.is24HourFormat.collectAsState()
    val isWallpaperMatching by viewModel.isWallpaperMatchingEnabled.collectAsState()
    val isThemeWallpaperSync by viewModel.isThemeWallpaperSyncEnabled.collectAsState()
    val isSystemDarkSchema by viewModel.isSystemDarkSchemaEnabled.collectAsState()
    val hasSecureSettings = viewModel.hasSecureSettings
    val adbCmd = viewModel.adbGrantCommand
    val deviceAdminAdb = LauncherUtils.deviceAdminAdbCommand()
    val isDeviceAdmin = LauncherUtils.isDeviceAdminActive(context)
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsState()
    val hasContactsAccess by viewModel.hasContactsAccess.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val renamedApps by viewModel.renamedApps.collectAsState()
    val shortcutAppName by viewModel.shortcutAppName.collectAsState()
    val paymentAppName by viewModel.paymentAppName.collectAsState()
    val themeColorId by viewModel.themeColorId.collectAsState()
    val dailyGoal by viewModel.dailyGoalMinutes.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val hasWorkProfile by viewModel.hasWorkProfile.collectAsState()
    val isScheduledBlocking by viewModel.isScheduledBlockingEnabled.collectAsState()
    var showShortcutPicker by remember { mutableStateOf(false) }
    var showPaymentPicker by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showDisplayNameDialog by remember { mutableStateOf(false) }
    val homeDisplayName by viewModel.homeDisplayName.collectAsState()
    val allApps by viewModel.allAppConfigs.collectAsState()

    val onBackground = launcherOnBackground()
    val secondary = launcherSecondary()
    val muted = launcherMuted()

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> viewModel.onContactsPermissionChanged() }

    LaunchedEffect(Unit) {
        viewModel.refreshInsights()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(launcherBackground()).padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text("settings", color = secondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 12.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text("lock screen", color = secondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                MinimalToggle(
                    label = "matching lock screen wallpaper",
                    isOn = isWallpaperMatching,
                    onToggle = { viewModel.toggleWallpaperMatching(context) }
                )
                Text(
                    text = "sync minimal lock screen",
                    color = onBackground,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { viewModel.applyLockWallpaperNow(context) }
                        .padding(vertical = 14.dp)
                )
                Text(
                    text = "customize pixel lock screen",
                    color = onBackground,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { viewModel.openLockScreenSettings(context) }
                        .padding(vertical = 14.dp)
                )
                Text(
                    text = "lock screen uses pixel system clock on plain black wallpaper",
                    color = muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "lock phone now",
                    color = if (isDeviceAdmin) onBackground else secondary,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { viewModel.lockScreen(context) }
                        .padding(vertical = 14.dp)
                )
                Text(
                    text = if (isDeviceAdmin) "double-tap home screen also locks" else "device admin required — enable below",
                    color = muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "enable device admin",
                    color = onBackground,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { LauncherUtils.requestDeviceAdmin(context) }
                        .padding(vertical = 14.dp)
                )
                if (!isDeviceAdmin) {
                    Text(deviceAdminAdb, color = muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 12.dp))
                }
            }
            item {
                Text("display", color = secondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                Text("launcher color", color = onBackground, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                ThemeColorPickerGrouped(
                    selectedThemeId = themeColorId,
                    onThemeSelected = { viewModel.setThemeColorId(it) }
                )
                SelectedThemeLabel(selectedThemeId = themeColorId)
                MinimalToggle(
                    label = "apply color to phone wallpaper",
                    isOn = isThemeWallpaperSync,
                    onToggle = { viewModel.toggleThemeWallpaperSync(context) }
                )
                Text(
                    text = "sets home and lock screen to your launcher color",
                    color = muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                MinimalToggle(
                    label = "dark schema for entire phone",
                    isOn = isSystemDarkSchema,
                    onToggle = { viewModel.toggleSystemDarkSchema(context) }
                )
                Text(
                    text = "desaturates all apps (grayscale) and forces system dark mode",
                    color = muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (!hasSecureSettings) {
                    Text(
                        text = "requires adb permission — tap system permissions setup",
                        color = secondary,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(AppScreen.SystemSetup) }
                            .padding(bottom = 8.dp)
                    )
                    Text(adbCmd, color = muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 12.dp))
                }
                MinimalToggle("time format (24h)", is24h, onToggle = { viewModel.toggle24HourFormat() })
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDisplayNameDialog = true }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("home display name", color = onBackground, fontSize = 16.sp)
                    Text(
                        homeDisplayName,
                        color = secondary,
                        fontSize = 16.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false).padding(start = 16.dp)
                    )
                }
                Text(
                    text = "restore phone colors",
                    color = secondary,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { viewModel.restoreAppearanceDefaults(context) }.padding(vertical = 14.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showGoalDialog = true }.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("daily screen time goal", color = onBackground, fontSize = 16.sp)
                    Text("$dailyGoal min", color = secondary, fontSize = 16.sp)
                }
            }
            item {
                Text("insights", color = secondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                InsightsPreviewCard(
                    insights = insights,
                    hasUsageAccess = hasUsageAccess,
                    onOpenInsights = { viewModel.navigateTo(AppScreen.Insights) },
                    onGrantUsageAccess = { viewModel.openUsageAccessSettings(context) }
                )
                Text(
                    "all data stays on this device",
                    color = muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            item {
                Text("permissions", color = secondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(AppScreen.SystemSetup) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("system permissions setup", color = onBackground, fontSize = 16.sp)
                    Text(if (hasSecureSettings && isDeviceAdmin) "[ready]" else "[setup]", color = secondary)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openUsageAccessSettings(context) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("usage access", color = onBackground, fontSize = 16.sp)
                    Text(if (hasUsageAccess) "[active]" else "[needs setup]", color = secondary)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { MutedNotificationListenerService.openSettings(context) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("notification listener", color = onBackground, fontSize = 16.sp)
                    Text(
                        if (MutedNotificationListenerService.isEnabled(context)) "[active]" else "[needs setup]",
                        color = secondary
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("contacts (filter home alerts)", color = onBackground, fontSize = 16.sp)
                    Text(if (hasContactsAccess) "[active]" else "[needs setup]", color = secondary)
                }
            }
            item {
                Text("apps", color = secondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                if (hasWorkProfile) {
                    Text(
                        "work profile",
                        color = onBackground,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(AppScreen.WorkProfile) }
                            .padding(vertical = 14.dp)
                    )
                }
                Text("widgets", color = onBackground, modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.Widgets) }.padding(vertical = 14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(AppScreen.Focus) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("focus & app limits", color = onBackground, fontSize = 16.sp)
                    Text(if (isScheduledBlocking) "[scheduled on]" else "[open]", color = secondary)
                }
                Text(
                    "notifications dashboard",
                    color = onBackground,
                    modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.Notifications) }.padding(vertical = 14.dp)
                )
                if (favoriteApps.isNotEmpty()) {
                    Text("pinned home apps", color = secondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                    favoriteApps.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(app.appName.lowercase(), color = onBackground)
                            Text("unpin", color = secondary, modifier = Modifier.clickable { viewModel.unpinApp(app) })
                        }
                    }
                }
                renamedApps.forEach { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.resetAppLabel(app) }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(app.appName.lowercase(), color = onBackground)
                        Text("[reset]", color = secondary)
                    }
                }
            }
            item {
                Text("shortcuts", color = secondary, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showShortcutPicker = true }.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("time ring long-press", color = onBackground)
                    Text(shortcutAppName?.lowercase() ?: "[not assigned]", color = secondary)
                }
                if (shortcutAppName != null) {
                    Text("clear shortcut", color = secondary, modifier = Modifier.clickable { viewModel.clearShortcut() }.padding(vertical = 10.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showPaymentPicker = true }.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("default payment app", color = onBackground)
                    Text(paymentAppName?.lowercase() ?: "[not set]", color = secondary)
                }
                if (paymentAppName != null) {
                    Text("clear payment app", color = secondary, modifier = Modifier.clickable { viewModel.clearPaymentApp() }.padding(vertical = 10.dp))
                }
            }
        }
        LauncherBackBar(onBack = { viewModel.goHome() }, label = "< back")
    }

    if (showShortcutPicker) {
        AppPickerDialog(
            title = "choose shortcut app",
            apps = allApps.filter { !it.isHidden },
            onSelect = { app ->
                viewModel.assignShortcut(app.appName, app.packageName)
                showShortcutPicker = false
            },
            onDismiss = { showShortcutPicker = false }
        )
    }

    if (showPaymentPicker) {
        AppPickerDialog(
            title = "choose payment app",
            apps = viewModel.paymentAppCandidates(),
            onSelect = { app ->
                viewModel.assignPaymentApp(app.appName, app.packageName)
                showPaymentPicker = false
            },
            onDismiss = { showPaymentPicker = false }
        )
    }

    if (showGoalDialog) {
        var goalText by remember { mutableStateOf(dailyGoal.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            containerColor = launcherDialogContainer(),
            title = { Text("daily goal (minutes)", color = onBackground) },
            text = {
                BasicTextField(
                    value = goalText,
                    onValueChange = { goalText = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = onBackground, fontSize = 18.sp),
                    cursorBrush = SolidColor(onBackground)
                )
            },
            confirmButton = {
                Text("save", color = onBackground, modifier = Modifier.clickable {
                    goalText.toIntOrNull()?.let { viewModel.setDailyGoalMinutes(it) }
                    showGoalDialog = false
                }.padding(16.dp))
            },
            dismissButton = { Text("cancel", color = secondary, modifier = Modifier.clickable { showGoalDialog = false }.padding(16.dp)) }
        )
    }

    if (showDisplayNameDialog) {
        var nameText by remember(homeDisplayName) { mutableStateOf(homeDisplayName) }
        AlertDialog(
            onDismissRequest = { showDisplayNameDialog = false },
            containerColor = launcherDialogContainer(),
            title = { Text("home display name", color = onBackground) },
            text = {
                Column {
                    BasicTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = onBackground, fontSize = 18.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(onBackground),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                    Text(
                        "shown below the clock on home",
                        color = muted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Text("save", color = onBackground, modifier = Modifier.clickable {
                    if (nameText.trim().isNotEmpty()) viewModel.setHomeDisplayName(nameText)
                    showDisplayNameDialog = false
                }.padding(16.dp))
            },
            dismissButton = {
                Text("cancel", color = secondary, modifier = Modifier.clickable { showDisplayNameDialog = false }.padding(16.dp))
            }
        )
    }
}
