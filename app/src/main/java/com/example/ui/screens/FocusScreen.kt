package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfigEntity
import com.example.ui.LauncherViewModel
import com.example.ui.components.LauncherBackBar
import com.example.ui.components.MinimalToggle
import com.example.ui.theme.launcherBackground
import com.example.ui.theme.launcherDialogContainer
import com.example.ui.theme.launcherMuted
import com.example.ui.theme.launcherOnBackground
import com.example.ui.theme.launcherSecondary

@Composable
fun FocusScreen(viewModel: LauncherViewModel) {
    val allApps by viewModel.allAppConfigs.collectAsState()
    val totalMinutes by viewModel.totalScreenTimeMinutes.collectAsState()
    val isWebsiteBlocking by viewModel.isWebsiteBlockingEnabled.collectAsState()
    val isScheduledBlocking by viewModel.isScheduledBlockingEnabled.collectAsState()
    val schedStartH by viewModel.scheduledStartHour.collectAsState()
    val schedStartM by viewModel.scheduledStartMinute.collectAsState()
    val schedEndH by viewModel.scheduledEndHour.collectAsState()
    val schedEndM by viewModel.scheduledEndMinute.collectAsState()
    val schedDays by viewModel.scheduledDays.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val limitedOrBlockedApps = allApps.filter { it.isBlocked || it.dailyLimitMinutes > 0 }

    var showAddAppSelector by remember { mutableStateOf(false) }
    var runningLimitConfigApp by remember { mutableStateOf<AppConfigEntity?>(null) }
    var manualLimitMinutes by remember { mutableStateOf("") }
    var isEditingScheduleRange by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(launcherBackground())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text("today: ${hours}h ${minutes}m", color = launcherOnBackground(), fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))

        Text("start focus session", color = launcherSecondary(), fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 24.dp)) {
            listOf(15, 30, 45).forEach { mins ->
                Text(
                    text = "[$mins mins]",
                    color = launcherOnBackground(),
                    modifier = Modifier.clickable { viewModel.startFocusSession(mins) }.padding(vertical = 12.dp)
                )
            }
        }

        Text("restricted apps & limits", color = launcherSecondary(), fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(limitedOrBlockedApps) { app ->
                val displayLimit = if (app.isBlocked) "blocked" else "${app.dailyLimitMinutes} min"
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { runningLimitConfigApp = app }.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(app.appName.lowercase(), color = launcherOnBackground(), fontSize = 16.sp)
                    Text(displayLimit, color = launcherSecondary(), fontSize = 14.sp)
                }
            }
            item {
                Text(
                    text = "+ add app",
                    color = launcherOnBackground(),
                    modifier = Modifier.fillMaxWidth().clickable { showAddAppSelector = true }.padding(vertical = 14.dp)
                )
            }
        }

        MinimalToggle("scheduled blocking", isScheduledBlocking, onToggle = { viewModel.toggleScheduledBlocking() })
        if (isScheduledBlocking) {
            Text(
                text = "${schedStartH}:${String.format("%02d", schedStartM)} – ${schedEndH}:${String.format("%02d", schedEndM)} on ${schedDays.joinToString(", ")} (tap to edit)",
                color = launcherSecondary(),
                fontSize = 12.sp,
                modifier = Modifier.clickable { isEditingScheduleRange = true }.padding(vertical = 8.dp)
            )
        }
        MinimalToggle("block websites in blocked apps", isWebsiteBlocking, onToggle = { viewModel.toggleWebsiteBlocking(context) })

        LauncherBackBar(onBack = { viewModel.goHome() })
    }

    if (showAddAppSelector) {
        val nonFocusApps = allApps.filter { !it.isBlocked && it.dailyLimitMinutes == 0 }
        AlertDialog(
            onDismissRequest = { showAddAppSelector = false },
            containerColor = launcherBackground(),
            title = { Text("add app to limit", color = launcherOnBackground()) },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(nonFocusApps) { app ->
                        Text(
                            text = app.appName.lowercase(),
                            color = launcherOnBackground(),
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.updateDailyLimit(app, 15)
                                showAddAppSelector = false
                            }.padding(vertical = 14.dp)
                        )
                    }
                }
            },
            confirmButton = { Text("close", color = launcherOnBackground(), modifier = Modifier.clickable { showAddAppSelector = false }.padding(16.dp)) }
        )
    }

    runningLimitConfigApp?.let { app ->
        AlertDialog(
            onDismissRequest = { runningLimitConfigApp = null },
            containerColor = launcherBackground(),
            title = { Text("manage ${app.appName.lowercase()}", color = launcherOnBackground()) },
            text = {
                Column {
                    Text("daily limit (minutes)", color = launcherSecondary(), fontSize = 12.sp)
                    BasicTextField(
                        value = manualLimitMinutes.ifEmpty { app.dailyLimitMinutes.toString() },
                        onValueChange = { manualLimitMinutes = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = launcherOnBackground(), fontSize = 18.sp),
                        cursorBrush = SolidColor(launcherOnBackground()),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    Text("toggle block", color = launcherOnBackground(), modifier = Modifier.clickable {
                        viewModel.toggleBlocked(app)
                        runningLimitConfigApp = null
                    }.padding(vertical = 12.dp))
                }
            },
            confirmButton = {
                Text("save", color = launcherOnBackground(), modifier = Modifier.clickable {
                    val mins = manualLimitMinutes.toIntOrNull() ?: app.dailyLimitMinutes
                    viewModel.updateDailyLimit(app, mins)
                    runningLimitConfigApp = null
                }.padding(16.dp))
            },
            dismissButton = {
                Text("cancel", color = launcherSecondary(), modifier = Modifier.clickable { runningLimitConfigApp = null }.padding(16.dp))
            }
        )
    }

    if (isEditingScheduleRange) {
        var startH by remember { mutableStateOf(schedStartH.toString()) }
        var endH by remember { mutableStateOf(schedEndH.toString()) }
        AlertDialog(
            onDismissRequest = { isEditingScheduleRange = false },
            containerColor = launcherBackground(),
            title = { Text("edit schedule", color = launcherOnBackground()) },
            text = {
                Column {
                    Text("start hour", color = launcherSecondary())
                    BasicTextField(startH, { startH = it }, textStyle = androidx.compose.ui.text.TextStyle(color = launcherOnBackground()), cursorBrush = SolidColor(launcherOnBackground()))
                    Text("end hour", color = launcherSecondary(), modifier = Modifier.padding(top = 8.dp))
                    BasicTextField(endH, { endH = it }, textStyle = androidx.compose.ui.text.TextStyle(color = launcherOnBackground()), cursorBrush = SolidColor(launcherOnBackground()))
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                        val selected = schedDays.contains(day)
                        Text(
                            text = if (selected) "[$day]" else day,
                            color = launcherOnBackground(),
                            modifier = Modifier.clickable { viewModel.toggleScheduledDay(day) }.padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Text("save", color = launcherOnBackground(), modifier = Modifier.clickable {
                    viewModel.saveScheduleTime(startH.toIntOrNull() ?: schedStartH, schedStartM, endH.toIntOrNull() ?: schedEndH, schedEndM)
                    isEditingScheduleRange = false
                }.padding(16.dp))
            },
            dismissButton = {
                Text("cancel", color = launcherSecondary(), modifier = Modifier.clickable { isEditingScheduleRange = false }.padding(16.dp))
            }
        )
    }
}
