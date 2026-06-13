package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.MinimalFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfigEntity
import com.example.service.MutedNotificationListenerService
import com.example.ui.LauncherViewModel
import com.example.ui.components.BreathingPauseOverlay
import com.example.ui.components.HomeWidget
import com.example.ui.components.pixelHomeDrawerSwipe
import com.example.ui.navigation.AppScreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val priorityNotifications by viewModel.priorityNotifications.collectAsState()
    val is24h by viewModel.is24HourFormat.collectAsState()
    val showBreathing by viewModel.showBreathingPause.collectAsState()
    val shortcutPkg by viewModel.shortcutPackageName.collectAsState()
    val activeWidgets by viewModel.activeWidgets.collectAsState()
    val totalMinutes by viewModel.totalScreenTimeMinutes.collectAsState()
    val dailyGoal by viewModel.dailyGoalMinutes.collectAsState()
    val isFocusActive by viewModel.isFocusActive.collectAsState()
    val focusSecsLeft by viewModel.focusTimerSeconds.collectAsState()
    val homeDisplayName by viewModel.homeDisplayName.collectAsState()

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(is24h) {
        while (true) {
            val formatTime = if (is24h) "HH:mm" else "hh:mm a"
            currentTime = SimpleDateFormat(formatTime, Locale.getDefault()).format(Date())
            currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    var selectedAppForMenu by remember { mutableStateOf<AppConfigEntity?>(null) }
    var selectedAppForRename by remember { mutableStateOf<AppConfigEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    val sweepProgress = if (dailyGoal > 0) totalMinutes.toFloat() / dailyGoal else 0f

    LaunchedEffect(Unit) {
        viewModel.refreshPriorityNotifications()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pixelHomeDrawerSwipe(onSwipeUp = { viewModel.openDrawer() })
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { viewModel.lockScreen(context) },
                        onLongPress = { viewModel.navigateTo(AppScreen.Widgets) }
                    )
                },
            horizontalAlignment = Alignment.Start
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .combinedClickable(
                        onClick = { viewModel.navigateTo(AppScreen.Focus) },
                        onLongClick = {
                            if (shortcutPkg != null) {
                                viewModel.launchShortcut(context)
                            } else {
                                viewModel.navigateTo(AppScreen.ShortcutPicker)
                            }
                        }
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                activeWidgets.forEach { widgetId ->
                    HomeWidget(
                        widgetId = widgetId,
                        currentTime = currentTime,
                        currentDate = currentDate,
                        homeDisplayName = homeDisplayName,
                        sweepProgress = sweepProgress,
                        isFocusActive = isFocusActive,
                        focusSecsLeft = focusSecsLeft
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().weight(2f),
                verticalArrangement = Arrangement.Top
            ) {
                if (priorityNotifications.isNotEmpty()) {
                    priorityNotifications.forEach { notification ->
                        Text(
                            text = "${notification.appName.lowercase()} · ${notification.summary}",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            fontFamily = MinimalFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("priority_notification_${notification.packageName}")
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (!MutedNotificationListenerService.isEnabled(context)) {
                    Text(
                        text = "enable notification listener in settings to show alerts here",
                        color = Color.DarkGray,
                        fontSize = 11.sp,
                        fontFamily = MinimalFontFamily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(AppScreen.Settings) }
                            .padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (favoriteApps.isEmpty()) {
                    Text(
                        text = "no apps pinned. swipe up anywhere for all apps.",
                        color = Color.DarkGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(favoriteApps.take(8)) { app ->
                            Text(
                                text = app.appName.lowercase(),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontFamily = MinimalFontFamily,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { viewModel.tryLaunchApp(context, app.packageName, app.appName) },
                                        onLongClick = { selectedAppForMenu = app }
                                    )
                                    .padding(vertical = 8.dp)
                                    .testTag("pinned_app_${app.packageName}")
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "all apps",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { viewModel.openDrawer() }
                        .padding(vertical = 16.dp)
                        .testTag("all_apps_link")
                )
            }
        }
        BreathingPauseOverlay(visible = showBreathing)
    }

    selectedAppForMenu?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForMenu = null },
            containerColor = Color.Black,
            title = { Text(app.appName.lowercase(), color = Color.White, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("rename", "move up", "move down", "remove from home").forEach { action ->
                        Text(
                            text = action,
                            color = if (action == "remove from home") Color.LightGray else Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (action) {
                                        "rename" -> { renameInputText = app.appName; selectedAppForRename = app }
                                        "move up" -> viewModel.movePinnedApp(app, true)
                                        "move down" -> viewModel.movePinnedApp(app, false)
                                        "remove from home" -> viewModel.unpinApp(app)
                                    }
                                    selectedAppForMenu = null
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Text("close", color = Color.White, modifier = Modifier.clickable { selectedAppForMenu = null }.padding(16.dp))
            }
        )
    }

    selectedAppForRename?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForRename = null },
            containerColor = Color.Black,
            title = { Text("rename app", color = Color.White) },
            text = {
                BasicTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            },
            confirmButton = {
                Text("save", color = Color.White, modifier = Modifier.clickable {
                    if (renameInputText.trim().isNotEmpty()) viewModel.renameApp(app, renameInputText.trim())
                    selectedAppForRename = null
                }.padding(16.dp))
            },
            dismissButton = {
                Text("cancel", color = Color.LightGray, modifier = Modifier.clickable { selectedAppForRename = null }.padding(16.dp))
            }
        )
    }
}

