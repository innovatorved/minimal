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
import com.example.analytics.UsageInsightsSnapshot
import com.example.ui.LauncherViewModel
import com.example.ui.components.LauncherBackBar
import com.example.util.formatHourLabel
import com.example.util.formatMinutesAsHours

@Composable
fun InsightsScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val insights by viewModel.insights.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshInsights()
        viewModel.logInsightsView()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            "insights",
            color = Color.LightGray,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            "local only · nothing leaves your device",
            color = Color.DarkGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (insights == null) {
            Text("loading…", color = Color.Gray, fontSize = 14.sp)
        } else if (!insights!!.hasUsageAccess) {
            Text(
                "usage access required for screen time patterns",
                color = Color.LightGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                "grant usage access",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.clickable { viewModel.openUsageAccessSettings(context) }.padding(vertical = 14.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item { InsightsSectionTitle("today") }
                item {
                    val snap = insights!!
                    InsightRow("screen time", formatMinutesAsHours(snap.todayScreenMinutes))
                    InsightRow("goal", "${snap.goalPercent}% of ${formatMinutesAsHours(snap.goalMinutes)}")
                    InsightRow("opens", snap.todayOpens.toString())
                    InsightRow("blocked", snap.blockedToday.toString())
                    snap.peakHour?.let { InsightRow("peak hour", formatHourLabel(it)) }
                    snap.firstActivityHour?.let { InsightRow("first use", formatHourLabel(it)) }
                    snap.lastActivityHour?.let { InsightRow("last use", formatHourLabel(it)) }
                }

                item { InsightsSectionTitle("this week") }
                item {
                    val snap = insights!!
                    InsightRow("total", formatMinutesAsHours(snap.weekTotalMinutes))
                    InsightRow("daily avg", formatMinutesAsHours(snap.weekDailyAvgMinutes))
                    InsightRow("focus sessions", snap.focusSessionsThisWeek.toString())
                    snap.weekChangePercent?.let { change ->
                        val sign = if (change >= 0) "+" else ""
                        InsightRow("vs last week", "$sign$change%")
                    }
                    if (snap.underGoalStreak > 0) {
                        InsightRow("under goal streak", "${snap.underGoalStreak} days")
                    }
                }

                if (insights!!.topAppsToday.isNotEmpty()) {
                    item { InsightsSectionTitle("top apps today") }
                    items(insights!!.topAppsToday) { app ->
                        AppUsageRow(app.appName, app.durationMinutes, app.openCount)
                    }
                }

                if (insights!!.topAppsWeek.isNotEmpty()) {
                    item { InsightsSectionTitle("top apps (7 days)") }
                    items(insights!!.topAppsWeek) { app ->
                        AppUsageRow(app.appName, app.durationMinutes, app.openCount)
                    }
                }

                if (insights!!.lastSevenDays.isNotEmpty()) {
                    item { InsightsSectionTitle("last 7 days") }
                    items(insights!!.lastSevenDays) { day ->
                        val label = day.date.substring(5)
                        val goalMark = if (day.underDailyGoal) " · under goal" else ""
                        Text(
                            "$label  ${formatMinutesAsHours(day.totalScreenMinutes)}$goalMark",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }

                if (insights!!.launcherEventsToday.isNotEmpty()) {
                    item { InsightsSectionTitle("launcher habits today") }
                    item {
                        insights!!.launcherEventsToday.forEach { (type, count) ->
                            InsightRow(type.replace('_', ' '), count.toString())
                        }
                    }
                }

                item {
                    Text(
                        "refresh",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { viewModel.refreshInsights() }
                            .padding(vertical = 14.dp)
                    )
                    Text(
                        "delete all history",
                        color = Color.LightGray,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { showDeleteConfirm = true }
                            .padding(vertical = 14.dp)
                    )
                }
            }
        }

        LauncherBackBar(onBack = { viewModel.goHome() })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color.Black,
            title = { Text("delete all insights history?", color = Color.White) },
            text = {
                Text(
                    "removes usage, focus, and launcher event history from this device. app settings stay.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Text(
                    "delete",
                    color = Color.White,
                    modifier = Modifier.clickable {
                        viewModel.deleteAnalyticsHistory(context)
                        showDeleteConfirm = false
                    }.padding(16.dp)
                )
            },
            dismissButton = {
                Text(
                    "cancel",
                    color = Color.LightGray,
                    modifier = Modifier.clickable { showDeleteConfirm = false }.padding(16.dp)
                )
            }
        )
    }
}

@Composable
fun InsightsPreviewCard(
    insights: UsageInsightsSnapshot?,
    hasUsageAccess: Boolean,
    onOpenInsights: () -> Unit,
    onGrantUsageAccess: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        if (!hasUsageAccess) {
            Text(
                "grant usage access to see patterns",
                color = Color.DarkGray,
                fontSize = 11.sp,
                modifier = Modifier
                    .clickable(onClick = onGrantUsageAccess)
                    .padding(bottom = 8.dp)
            )
        } else if (insights != null) {
            val top = insights.topAppsToday.firstOrNull()
            Text(
                "today: ${formatMinutesAsHours(insights.todayScreenMinutes)} · ${insights.goalPercent}% of goal",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            if (top != null) {
                Text(
                    "top: ${top.appName.lowercase()} ${formatMinutesAsHours(top.durationMinutes)}",
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        Text(
            "view full insights →",
            color = Color.LightGray,
            fontSize = 16.sp,
            modifier = Modifier.clickable(onClick = onOpenInsights).padding(vertical = 14.dp)
        )
    }
}

@Composable
private fun InsightsSectionTitle(title: String) {
    Text(
        title,
        color = Color.Gray,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun InsightRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.LightGray, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun AppUsageRow(appName: String, minutes: Int, opens: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(appName.lowercase(), color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            "${formatMinutesAsHours(minutes)} · $opens opens",
            color = Color.LightGray,
            fontSize = 12.sp
        )
    }
}
