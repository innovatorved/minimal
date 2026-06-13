package com.example.analytics

import android.content.Context
import com.example.data.DailyRollupEntity
import com.example.data.LauncherDao
import com.example.data.LauncherEventEntity
import com.example.usage.UsageStatsRepository
import com.example.util.dateStringDaysAgo
import com.example.util.pastDateStrings
import com.example.util.todayDateString
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.first

class UsageAnalyticsRepository(
    private val context: Context,
    private val dao: LauncherDao,
    private val usageStatsRepository: UsageStatsRepository
) {

    suspend fun logEvent(type: String, metadata: String? = null) {
        dao.insertLauncherEvent(
            LauncherEventEntity(type = type, metadata = metadata)
        )
    }

    suspend fun rebuildRollupForDate(date: String, dailyGoalMinutes: Int) {
        if (!usageStatsRepository.hasUsageAccess()) return

        usageStatsRepository.syncUsageForDate(date)

        val usageRows = dao.getUsageForDate(date).first()
        val totalScreenMinutes = usageRows.sumOf { it.durationMinutes }
        val totalOpens = dao.sumOpensForDate(date) ?: 0
        val blockedAttempts = dao.sumBlockedForDate(date) ?: 0
        val top = usageRows.maxByOrNull { it.durationMinutes }
        val focusCount = dao.countCompletedFocusSessionsForDate(date)
        val (dayStart, dayEnd) = dayBoundsMillis(date)
        val mutedCount = dao.countMutedNotificationsBetween(dayStart, dayEnd)
        val peakHour = usageStatsRepository.getPeakHourForDate(date)
        val (firstHour, lastHour) = usageStatsRepository.getFirstLastActivityHour(date)

        dao.insertRollup(
            DailyRollupEntity(
                date = date,
                totalScreenMinutes = totalScreenMinutes,
                totalOpens = totalOpens,
                topAppPackage = top?.packageName,
                topAppName = top?.appName,
                topAppMinutes = top?.durationMinutes ?: 0,
                blockedAttempts = blockedAttempts,
                focusSessionsCompleted = focusCount,
                mutedNotificationCount = mutedCount,
                peakHour = peakHour,
                firstActivityHour = firstHour,
                lastActivityHour = lastHour,
                underDailyGoal = dailyGoalMinutes > 0 && totalScreenMinutes <= dailyGoalMinutes
            )
        )
    }

    suspend fun rebuildRecentRollups(days: Int, dailyGoalMinutes: Int) {
        pastDateStrings(days).forEach { date ->
            rebuildRollupForDate(date, dailyGoalMinutes)
        }
    }

    suspend fun loadInsights(dailyGoalMinutes: Int): UsageInsightsSnapshot {
        val today = todayDateString()
        val weekStart = dateStringDaysAgo(6)
        val priorWeekStart = dateStringDaysAgo(13)
        val priorWeekEnd = dateStringDaysAgo(7)

        val hasAccess = usageStatsRepository.hasUsageAccess()
        if (hasAccess) {
            rebuildRollupForDate(today, dailyGoalMinutes)
        }

        val todayRollup = dao.getRollupForDate(today)
        val lastSeven = dao.getRollupsSince(weekStart).take(7)
        val weekTotal = dao.sumScreenMinutesBetween(weekStart, today) ?: 0
        val priorWeekTotal = dao.sumScreenMinutesBetween(priorWeekStart, priorWeekEnd) ?: 0
        val weekChange = if (priorWeekTotal > 0) {
            ((weekTotal - priorWeekTotal) * 100) / priorWeekTotal
        } else {
            null
        }

        val topToday = dao.topAppsBetween(today, today, 5).map { row ->
            AppUsageSummary(row.packageName, row.appName, row.durationMinutes, row.openCount)
        }
        val topWeek = dao.topAppsBetween(weekStart, today, 5).map { row ->
            AppUsageSummary(row.packageName, row.appName, row.durationMinutes, row.openCount)
        }

        val focusWeek = dao.countCompletedFocusSessionsBetween(weekStart, today)
        val (dayStart, dayEnd) = dayBoundsMillis(today)
        val eventsToday = dao.countLauncherEventsBetween(dayStart, dayEnd)
            .associate { it.type to it.count }

        val todayMinutes = todayRollup?.totalScreenMinutes
            ?: usageStatsRepository.getTotalScreenTimeMinutes(today)
        val goalPercent = if (dailyGoalMinutes > 0) {
            ((todayMinutes * 100) / dailyGoalMinutes).coerceAtMost(999)
        } else {
            0
        }

        return UsageInsightsSnapshot(
            hasUsageAccess = hasAccess,
            todayScreenMinutes = todayMinutes,
            goalMinutes = dailyGoalMinutes,
            goalPercent = goalPercent,
            todayOpens = todayRollup?.totalOpens ?: 0,
            blockedToday = todayRollup?.blockedAttempts ?: 0,
            focusSessionsThisWeek = focusWeek,
            weekTotalMinutes = weekTotal,
            weekDailyAvgMinutes = if (lastSeven.isNotEmpty()) weekTotal / lastSeven.size else 0,
            weekChangePercent = weekChange,
            underGoalStreak = computeUnderGoalStreak(dailyGoalMinutes),
            peakHour = todayRollup?.peakHour,
            firstActivityHour = todayRollup?.firstActivityHour,
            lastActivityHour = todayRollup?.lastActivityHour,
            topAppsToday = topToday,
            topAppsWeek = topWeek,
            lastSevenDays = lastSeven,
            launcherEventsToday = eventsToday
        )
    }

    suspend fun clearAllAnalyticsHistory() {
        dao.clearAllUsage()
        dao.clearAllRollups()
        dao.clearLauncherEvents()
        dao.clearAllFocusSessions()
    }

    private suspend fun computeUnderGoalStreak(dailyGoalMinutes: Int): Int {
        if (dailyGoalMinutes <= 0) return 0
        var streak = 0
        val cal = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (i in 0 until 30) {
            val date = formatter.format(cal.time)
            val rollup = dao.getRollupForDate(date)
            val under = rollup?.underDailyGoal == true ||
                (rollup === null && usageStatsRepository.getTotalScreenTimeMinutes(date) <= dailyGoalMinutes)
            if (under) {
                streak++
            } else if (i > 0) {
                break
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun dayBoundsMillis(date: String): Pair<Long, Long> {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = formatter.parse(date) ?: return 0L to 0L
        val cal = Calendar.getInstance()
        cal.time = parsed
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return start to cal.timeInMillis
    }
}
