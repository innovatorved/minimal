package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.example.data.AppConfigEntity
import com.example.data.ShortNotificationDisplay

object ActiveNotificationsProvider {

    private val MESSAGING_PACKAGES = setOf(
        "com.whatsapp",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
        "com.facebook.orca",
        "com.telegram.messenger",
        "org.telegram.messenger",
        "com.Slack",
        "com.microsoft.teams",
        "com.google.android.gm",
        "com.android.email"
    )

    private val COMMON_IMPORTANT_PACKAGES = MESSAGING_PACKAGES + setOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.phone",
        "com.google.android.calendar",
        "com.android.calendar"
    )

    private val SKIP_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.minimalist.launcher"
    )

    fun isImportantApp(packageName: String, appConfigs: List<AppConfigEntity>): Boolean {
        if (packageName in SKIP_PACKAGES) return false
        val config = appConfigs.find { it.packageName == packageName }
        if (config != null) {
            if (config.isFavorite || config.dailyLimitMinutes > 0 || config.isBlocked) return true
        }
        return packageName in COMMON_IMPORTANT_PACKAGES
    }

    fun filterPriorityNotifications(
        context: Context,
        notifications: List<StatusBarNotification>,
        appConfigs: List<AppConfigEntity>,
        maxCount: Int = 3
    ): List<ShortNotificationDisplay> {
        return notifications
            .asSequence()
            .filter { sbn -> isImportantApp(sbn.packageName, appConfigs) }
            .filter { sbn -> isWorthShowing(context, sbn) }
            .filter { sbn -> DirectMessageNotificationFilter.isDirectPersonalMessage(context, sbn) }
            .filter { sbn -> hasDisplayableContent(sbn) }
            .sortedByDescending { it.postTime }
            .take(maxCount)
            .mapNotNull { sbn -> toShortDisplay(sbn, appConfigs, context) }
            .toList()
    }

    private fun isWorthShowing(context: Context, sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false

        val minImportance = if (sbn.packageName in MESSAGING_PACKAGES) {
            NotificationManager.IMPORTANCE_DEFAULT
        } else {
            NotificationManager.IMPORTANCE_HIGH
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = notification.channelId
            if (channelId != null) {
                val nm = context.getSystemService(NotificationManager::class.java) ?: return true
                val channel = nm.getNotificationChannel(channelId)
                if (channel != null) {
                    return channel.importance >= minImportance
                }
            }
        }

        @Suppress("DEPRECATION")
        val legacyMin = if (sbn.packageName in MESSAGING_PACKAGES) {
            Notification.PRIORITY_DEFAULT
        } else {
            Notification.PRIORITY_HIGH
        }
        @Suppress("DEPRECATION")
        return notification.priority >= legacyMin
    }

    private fun hasDisplayableContent(sbn: StatusBarNotification): Boolean {
        val (title, text) = extractTitleAndText(sbn.notification.extras)
        return title.isNotEmpty() || text.isNotEmpty()
    }

    private fun toShortDisplay(
        sbn: StatusBarNotification,
        appConfigs: List<AppConfigEntity>,
        context: Context
    ): ShortNotificationDisplay? {
        val (title, text) = extractTitleAndText(sbn.notification.extras)
        val summary = formatSummary(title, text) ?: return null

        val config = appConfigs.find { it.packageName == sbn.packageName }
        val appName = config?.appName?.takeIf { it.isNotEmpty() }
            ?: runCatching {
                context.packageManager.getApplicationLabel(
                    context.packageManager.getApplicationInfo(sbn.packageName, 0)
                ).toString()
            }.getOrNull()
            ?: sbn.packageName.substringAfterLast('.')

        return ShortNotificationDisplay(
            packageName = sbn.packageName,
            appName = appName,
            summary = summary
        )
    }

    fun formatStoredSummary(title: String, text: String, maxLen: Int = 48): String? =
        formatSummary(title.trim(), text.trim(), maxLen)

    private fun extractTitleAndText(extras: Bundle): Pair<String, String> {
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()

        if (text.isEmpty()) {
            text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        }
        if (text.isEmpty()) {
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (!lines.isNullOrEmpty()) {
                text = lines.last().toString().trim()
            }
        }
        if (text.isEmpty()) {
            text = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        }

        return title to text
    }

    private fun formatSummary(title: String, text: String, maxLen: Int = 48): String? {
        val raw = when {
            title.isNotEmpty() && text.isNotEmpty() -> "$title: $text"
            title.isNotEmpty() -> title
            text.isNotEmpty() -> text
            else -> return null
        }
        val normalized = raw.replace('\n', ' ').trim()
        if (normalized.isEmpty()) return null
        return if (normalized.length <= maxLen) {
            normalized.lowercase()
        } else {
            normalized.take(maxLen - 1).trimEnd().lowercase() + "…"
        }
    }
}
