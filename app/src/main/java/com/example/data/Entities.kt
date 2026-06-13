package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isFavorite: Boolean = false,
    val isBlocked: Boolean = false,
    val dailyLimitMinutes: Int = 0, // 0 for no limit
    val isSystemApp: Boolean = false,
    val isHidden: Boolean = false,
    val pinnedOrder: Int = 0
)

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val date: String, // YYYY-MM-DD
    val openCount: Int = 0,
    val durationMinutes: Int = 0,
    val blockedTries: Int = 0
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val durationMinutes: Int,
    val isCompleted: Boolean
)

@Entity(tableName = "daily_rollup")
data class DailyRollupEntity(
    @PrimaryKey val date: String,
    val totalScreenMinutes: Int = 0,
    val totalOpens: Int = 0,
    val topAppPackage: String? = null,
    val topAppName: String? = null,
    val topAppMinutes: Int = 0,
    val blockedAttempts: Int = 0,
    val focusSessionsCompleted: Int = 0,
    val mutedNotificationCount: Int = 0,
    val peakHour: Int? = null,
    val firstActivityHour: Int? = null,
    val lastActivityHour: Int? = null,
    val underDailyGoal: Boolean = false
)

@Entity(tableName = "launcher_events")
data class LauncherEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String? = null
)
