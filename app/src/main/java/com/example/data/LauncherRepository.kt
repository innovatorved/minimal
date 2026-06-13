package com.example.data

import kotlinx.coroutines.flow.Flow

class LauncherRepository(private val launcherDao: LauncherDao) {

    val allAppConfigs: Flow<List<AppConfigEntity>> = launcherDao.getAllAppConfigs()
    val favoriteApps: Flow<List<AppConfigEntity>> = launcherDao.getFavoriteApps()
    val allFocusSessions: Flow<List<FocusSessionEntity>> = launcherDao.getAllFocusSessions()

    fun getUsageForDate(date: String): Flow<List<AppUsageEntity>> {
        return launcherDao.getUsageForDate(date)
    }

    suspend fun insertAppConfigs(configs: List<AppConfigEntity>) {
        launcherDao.insertAppConfigs(configs)
    }

    suspend fun insertAppConfig(config: AppConfigEntity) {
        launcherDao.insertAppConfig(config)
    }

    suspend fun updateAppConfig(config: AppConfigEntity) {
        launcherDao.updateAppConfig(config)
    }

    suspend fun deleteAppConfig(packageName: String) {
        launcherDao.deleteAppConfig(packageName)
    }

    suspend fun recordAppLaunch(packageName: String, appName: String, date: String) {
        launcherDao.recordAppLaunch(packageName, appName, date)
    }

    suspend fun recordBlockedAttempt(packageName: String, appName: String, date: String) {
        launcherDao.recordBlockedAttempt(packageName, appName, date)
    }

    suspend fun addAppMinutes(packageName: String, appName: String, date: String, minutes: Int) {
        launcherDao.addAppMinutes(packageName, appName, date, minutes)
    }

    suspend fun insertFocusSession(session: FocusSessionEntity) {
        launcherDao.insertFocusSession(session)
    }

    val mutedNotifications: Flow<List<MutedNotificationEntity>> =
        launcherDao.getAllMutedNotifications()

    suspend fun markNotificationRead(id: Int) {
        launcherDao.markNotificationRead(id)
    }

    suspend fun markAllNotificationsRead() {
        launcherDao.markAllNotificationsRead()
    }
}
