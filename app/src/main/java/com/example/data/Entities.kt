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
