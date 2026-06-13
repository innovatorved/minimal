package com.example.analytics

import com.example.data.AppUsageEntity
import com.example.data.DailyRollupEntity

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val durationMinutes: Int,
    val openCount: Int
)

data class UsageInsightsSnapshot(
    val hasUsageAccess: Boolean,
    val todayScreenMinutes: Int,
    val goalMinutes: Int,
    val goalPercent: Int,
    val todayOpens: Int,
    val blockedToday: Int,
    val focusSessionsThisWeek: Int,
    val weekTotalMinutes: Int,
    val weekDailyAvgMinutes: Int,
    val weekChangePercent: Int?,
    val underGoalStreak: Int,
    val peakHour: Int?,
    val firstActivityHour: Int?,
    val lastActivityHour: Int?,
    val topAppsToday: List<AppUsageSummary>,
    val topAppsWeek: List<AppUsageSummary>,
    val lastSevenDays: List<DailyRollupEntity>,
    val launcherEventsToday: Map<String, Int>
)
