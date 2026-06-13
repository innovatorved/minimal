package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherDao {
    // --- App Config Queries ---
    @Query("SELECT * FROM app_config ORDER BY appName ASC")
    fun getAllAppConfigs(): Flow<List<AppConfigEntity>>

    @Query("SELECT * FROM app_config WHERE isFavorite = 1 ORDER BY pinnedOrder ASC, appName ASC")
    fun getFavoriteApps(): Flow<List<AppConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppConfigs(configs: List<AppConfigEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppConfig(config: AppConfigEntity)

    @Update
    suspend fun updateAppConfig(config: AppConfigEntity)

    // --- App Usage Queries ---
    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY (durationMinutes * 60 + openCount) DESC")
    fun getUsageForDate(date: String): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE packageName = :packageName AND date = :date LIMIT 1")
    suspend fun getUsageForPackageAndDate(packageName: String, date: String): AppUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: AppUsageEntity)

    @Update
    suspend fun updateUsage(usage: AppUsageEntity)

    // Helper to increment launch or log duration
    @Transaction
    suspend fun recordAppLaunch(packageName: String, appName: String, date: String) {
        val existing = getUsageForPackageAndDate(packageName, date)
        if (existing != null) {
            updateUsage(existing.copy(openCount = existing.openCount + 1))
        } else {
            insertUsage(AppUsageEntity(packageName = packageName, appName = appName, date = date, openCount = 1))
        }
    }

    @Transaction
    suspend fun recordBlockedAttempt(packageName: String, appName: String, date: String) {
        val existing = getUsageForPackageAndDate(packageName, date)
        if (existing != null) {
            updateUsage(existing.copy(blockedTries = existing.blockedTries + 1))
        } else {
            insertUsage(AppUsageEntity(packageName = packageName, appName = appName, date = date, blockedTries = 1))
        }
    }

    @Transaction
    suspend fun addAppMinutes(packageName: String, appName: String, date: String, minutes: Int) {
        val existing = getUsageForPackageAndDate(packageName, date)
        if (existing != null) {
            updateUsage(existing.copy(durationMinutes = existing.durationMinutes + minutes))
        } else {
            insertUsage(AppUsageEntity(packageName = packageName, appName = appName, date = date, durationMinutes = minutes))
        }
    }

    // --- Focus Session Queries ---
    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSessionEntity)

    // --- Muted Notifications ---
    @Query("SELECT * FROM muted_notifications ORDER BY timestamp DESC")
    fun getAllMutedNotifications(): Flow<List<MutedNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMutedNotification(notification: MutedNotificationEntity)

    @Query("UPDATE muted_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: Int)

    @Query("UPDATE muted_notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    // --- Daily Rollup ---
    @Query("SELECT * FROM daily_rollup WHERE date = :date LIMIT 1")
    suspend fun getRollupForDate(date: String): DailyRollupEntity?

    @Query("SELECT * FROM daily_rollup WHERE date >= :startDate ORDER BY date DESC")
    suspend fun getRollupsSince(startDate: String): List<DailyRollupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRollup(rollup: DailyRollupEntity)

    @Query("DELETE FROM daily_rollup")
    suspend fun clearAllRollups()

    @Query("SELECT SUM(durationMinutes) FROM app_usage WHERE date >= :startDate AND date <= :endDate")
    suspend fun sumScreenMinutesBetween(startDate: String, endDate: String): Int?

    @Query("SELECT SUM(openCount) FROM app_usage WHERE date = :date")
    suspend fun sumOpensForDate(date: String): Int?

    @Query("SELECT SUM(blockedTries) FROM app_usage WHERE date = :date")
    suspend fun sumBlockedForDate(date: String): Int?

    @Query("""
        SELECT packageName, appName, SUM(durationMinutes) as durationMinutes, SUM(openCount) as openCount
        FROM app_usage
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY packageName
        ORDER BY durationMinutes DESC
        LIMIT :limit
    """)
    suspend fun topAppsBetween(startDate: String, endDate: String, limit: Int): List<TopAppRow>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE date >= :startDate AND date <= :endDate AND isCompleted = 1")
    suspend fun countCompletedFocusSessionsBetween(startDate: String, endDate: String): Int

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE date = :date AND isCompleted = 1")
    suspend fun countCompletedFocusSessionsForDate(date: String): Int

    @Query("SELECT COUNT(*) FROM muted_notifications WHERE timestamp >= :startMs AND timestamp < :endMs")
    suspend fun countMutedNotificationsBetween(startMs: Long, endMs: Long): Int

    // --- Launcher Events ---
    @Insert
    suspend fun insertLauncherEvent(event: LauncherEventEntity)

    @Query("SELECT type, COUNT(*) as count FROM launcher_events WHERE timestamp >= :startMs AND timestamp < :endMs GROUP BY type")
    suspend fun countLauncherEventsBetween(startMs: Long, endMs: Long): List<EventCountRow>

    @Query("DELETE FROM launcher_events")
    suspend fun clearLauncherEvents()

    @Query("DELETE FROM app_usage")
    suspend fun clearAllUsage()

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAllFocusSessions()
}

data class TopAppRow(
    val packageName: String,
    val appName: String,
    val durationMinutes: Int,
    val openCount: Int
)

data class EventCountRow(
    val type: String,
    val count: Int
)
