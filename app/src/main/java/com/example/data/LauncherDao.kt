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
}
