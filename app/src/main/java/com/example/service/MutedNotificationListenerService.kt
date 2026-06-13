package com.example.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.AppConfigEntity
import com.example.data.LauncherDatabase
import com.example.data.MutedNotificationEntity
import com.example.data.ShortNotificationDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class MutedNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        syncActiveCache(activeNotifications?.toList().orEmpty())
    }

    override fun onListenerDisconnected() {
        instance = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn === null) return
        cachedNotifications[sbn.key] = sbn
        persistMutedNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn === null) return
        cachedNotifications.remove(sbn.key)
    }

    private fun persistMutedNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        if (title.isEmpty() && text.isEmpty()) return

        scope.launch {
            val dao = LauncherDatabase.getDatabase(applicationContext).launcherDao()
            dao.insertMutedNotification(
                MutedNotificationEntity(
                    packageName = sbn.packageName,
                    title = title,
                    text = text,
                    timestamp = sbn.postTime
                )
            )
        }
    }

    companion object {
        @Volatile
        private var instance: MutedNotificationListenerService? = null

        private val cachedNotifications = ConcurrentHashMap<String, StatusBarNotification>()

        fun isEnabled(context: Context): Boolean {
            val enabled = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return enabled.contains(context.packageName)
        }

        fun openSettings(context: Context) {
            context.startActivity(
                android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        fun getPriorityNotifications(
            context: Context,
            appConfigs: List<AppConfigEntity>,
            maxCount: Int = 3
        ): List<ShortNotificationDisplay> {
            if (!isEnabled(context)) return emptyList()

            ensureListenerConnected(context)

            val active = collectActiveNotifications()
            val fromActive = ActiveNotificationsProvider.filterPriorityNotifications(
                context = context,
                notifications = active,
                appConfigs = appConfigs,
                maxCount = maxCount
            )
            if (fromActive.isNotEmpty()) return fromActive

            return fallbackFromRecentStored(context, appConfigs, maxCount)
        }

        private fun ensureListenerConnected(context: Context) {
            if (instance != null) return
            runCatching {
                requestRebind(ComponentName(context, MutedNotificationListenerService::class.java))
            }
        }

        private fun collectActiveNotifications(): List<StatusBarNotification> {
            val live = instance?.activeNotifications?.toList().orEmpty()
            if (live.isNotEmpty()) {
                syncActiveCache(live)
                return live
            }
            return cachedNotifications.values.toList()
        }

        private fun syncActiveCache(notifications: List<StatusBarNotification>) {
            cachedNotifications.clear()
            notifications.forEach { cachedNotifications[it.key] = it }
        }

        private fun fallbackFromRecentStored(
            context: Context,
            appConfigs: List<AppConfigEntity>,
            maxCount: Int
        ): List<ShortNotificationDisplay> {
            val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            return runCatching {
                val dao = LauncherDatabase.getDatabase(context).launcherDao()
                val recent = kotlinx.coroutines.runBlocking {
                    dao.getRecentMutedNotifications(cutoff, maxCount * 4)
                }
                recent
                    .filter { ActiveNotificationsProvider.isImportantApp(it.packageName, appConfigs) }
                    .filter {
                        DirectMessageNotificationFilter.isDirectPersonalStoredMessage(
                            context,
                            it.title,
                            it.text
                        )
                    }
                    .distinctBy { it.packageName }
                    .take(maxCount)
                    .mapNotNull { entity ->
                        val summary = ActiveNotificationsProvider.formatStoredSummary(entity.title, entity.text)
                            ?: return@mapNotNull null
                        val config = appConfigs.find { it.packageName == entity.packageName }
                        val appName = config?.appName?.takeIf { it.isNotEmpty() }
                            ?: entity.packageName.substringAfterLast('.')
                        ShortNotificationDisplay(
                            packageName = entity.packageName,
                            appName = appName,
                            summary = summary
                        )
                    }
            }.getOrDefault(emptyList())
        }
    }
}
