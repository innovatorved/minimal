package com.example.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import com.example.data.AppUsageEntity
import com.example.data.LauncherDao
import com.example.util.todayDateString
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class UsageStatsRepository(
    private val context: Context,
    private val dao: LauncherDao
) {
    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings() {
        val packageIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(packageIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall back to the generic usage-access list on devices that ignore package URI.
        }
        try {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "could not open usage access settings", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun syncTodayUsage(date: String = todayDateString()) {
        syncUsageForDate(date)
    }

    suspend fun syncUsageForDate(date: String) {
        if (!hasUsageAccess() || usageStatsManager === null) return

        val (start, end) = dayBoundsMillis(date)
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        ) ?: return

        val appNames = dao.getAllAppConfigs().first().associate { it.packageName to it.appName }

        for (stat in stats) {
            if (stat.totalTimeInForeground <= 0L) continue
            val minutes = TimeUnit.MILLISECONDS.toMinutes(stat.totalTimeInForeground).toInt()
            if (minutes <= 0) continue
            val appName = appNames[stat.packageName] ?: stat.packageName
            val existing = dao.getUsageForPackageAndDate(stat.packageName, date)
            val openCount = countLaunchEvents(stat.packageName, start, end)
            if (existing != null) {
                dao.updateUsage(
                    existing.copy(
                        appName = appName,
                        durationMinutes = minutes,
                        openCount = maxOf(existing.openCount, openCount)
                    )
                )
            } else {
                dao.insertUsage(
                    AppUsageEntity(
                        packageName = stat.packageName,
                        appName = appName,
                        date = date,
                        openCount = openCount,
                        durationMinutes = minutes
                    )
                )
            }
        }
    }

    suspend fun backfillRecentDays(days: Int) {
        if (!hasUsageAccess()) return
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        for (i in 0 until days) {
            val date = formatter.format(cal.time)
            syncUsageForDate(date)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
    }

    fun getForegroundPackage(): String? {
        if (!hasUsageAccess() || usageStatsManager === null) return null
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.SECONDS.toMillis(5)
        val events = usageStatsManager.queryEvents(start, end) ?: return null
        var lastPkg: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    fun getTotalScreenTimeMinutes(date: String = todayDateString()): Int {
        if (!hasUsageAccess() || usageStatsManager === null) return 0
        val (start, end) = dayBoundsMillis(date)
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        ) ?: return 0
        return stats.sumOf { TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground).toInt() }
    }

    fun getPeakHourForDate(date: String): Int? {
        if (!hasUsageAccess() || usageStatsManager === null) return null
        val (start, end) = dayBoundsMillis(date)
        val events = usageStatsManager.queryEvents(start, end) ?: return null
        val hourBuckets = IntArray(24)
        val event = UsageEvents.Event()
        var sessionStart: Long? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (sessionStart === null) sessionStart = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    sessionStart?.let { started ->
                        addSessionToBuckets(hourBuckets, started, event.timeStamp)
                    }
                    sessionStart = null
                }
            }
        }
        sessionStart?.let { started ->
            addSessionToBuckets(hourBuckets, started, end)
        }
        val max = hourBuckets.maxOrNull() ?: 0
        if (max <= 0) return null
        return hourBuckets.indexOf(max)
    }

    fun getFirstLastActivityHour(date: String): Pair<Int?, Int?> {
        if (!hasUsageAccess() || usageStatsManager === null) return null to null
        val (start, end) = dayBoundsMillis(date)
        val events = usageStatsManager.queryEvents(start, end) ?: return null to null
        val event = UsageEvents.Event()
        var firstHour: Int? = null
        var lastHour: Int? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                val hour = hourOf(event.timeStamp)
                if (firstHour === null) firstHour = hour
                lastHour = hour
            }
        }
        return firstHour to lastHour
    }

    private fun addSessionToBuckets(buckets: IntArray, startMs: Long, endMs: Long) {
        if (endMs <= startMs) return
        var cursor = startMs
        while (cursor < endMs) {
            val hour = hourOf(cursor)
            val hourEnd = nextHourStart(cursor)
            val sliceEnd = minOf(endMs, hourEnd)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(sliceEnd - cursor).toInt()
            if (minutes > 0) buckets[hour] += minutes
            cursor = sliceEnd
        }
    }

    private fun hourOf(timestampMs: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestampMs
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    private fun nextHourStart(timestampMs: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestampMs
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.HOUR_OF_DAY, 1)
        return cal.timeInMillis
    }

    private fun countLaunchEvents(packageName: String, start: Long, end: Long): Int {
        if (usageStatsManager === null) return 0
        val events = usageStatsManager.queryEvents(start, end) ?: return 0
        var count = 0
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == packageName &&
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                count++
            }
        }
        return count
    }

    private fun dayBoundsMillis(date: String): Pair<Long, Long> {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = formatter.parse(date) ?: Date()
        val cal = Calendar.getInstance()
        cal.time = parsed
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endExclusive = cal.timeInMillis
        val end = if (date == todayDateString()) {
            minOf(System.currentTimeMillis(), endExclusive)
        } else {
            endExclusive
        }
        return start to end
    }

    fun opacityForOpenCount(openCount: Int, maxOpenCount: Int): Float {
        if (maxOpenCount <= 0) return 1f
        val ratio = openCount.toFloat() / maxOpenCount.toFloat()
        return (0.4f + ratio * 0.6f).coerceIn(0.4f, 1f)
    }
}
