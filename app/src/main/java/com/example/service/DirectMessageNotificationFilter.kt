package com.example.service

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicReference

/**
 * Keeps home-screen alerts to direct/saved-contact style messages and drops promos / business blasts.
 */
object DirectMessageNotificationFilter {

    private val MESSAGING_PACKAGES = setOf(
        "com.whatsapp",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
        "com.facebook.orca",
        "com.telegram.messenger",
        "org.telegram.messenger"
    )

    private val BLOCKED_CATEGORIES = setOf(
        Notification.CATEGORY_PROMO,
        Notification.CATEGORY_RECOMMENDATION,
        Notification.CATEGORY_SERVICE,
        Notification.CATEGORY_STATUS
    )

    private val BLOCKED_CHANNEL_KEYWORDS = listOf(
        "promo",
        "promotion",
        "marketing",
        "advert",
        "offer",
        "deal",
        "silent",
        "other_notification",
        "other_notifications",
        "status",
        "update",
        "news",
        "subscription",
        "reminder_marketing"
    )

    private val ALLOWED_WHATSAPP_CHANNEL_KEYWORDS = listOf(
        "individual",
        "group_chat",
        "group",
        "message",
        "chat",
        "call",
        "missed_call",
        "voip"
    )

    private val PROMO_TEXT_KEYWORDS = listOf(
        "unsubscribe",
        "click here",
        "limited time",
        "% off",
        "percent off",
        "sale ends",
        "shop now",
        "buy now",
        "advertisement",
        "sponsored",
        "promotional",
        "opt out",
        "opt-out",
        "free gift",
        "claim now",
        "last chance",
        "exclusive offer",
        "business account",
        "verified business"
    )

    private val contactCache = AtomicReference<ContactCache?>(null)

    fun isDirectPersonalMessage(context: Context, sbn: StatusBarNotification): Boolean {
        if (sbn.packageName !in MESSAGING_PACKAGES) return true

        val notification = sbn.notification
        val category = notification.category
        if (category != null && category in BLOCKED_CATEGORIES) return false

        val channelId = notification.channelId?.lowercase().orEmpty()
        if (channelId.isNotEmpty()) {
            if (BLOCKED_CHANNEL_KEYWORDS.any { channelId.contains(it) }) return false
            if (sbn.packageName == "com.whatsapp") {
                val allowed = ALLOWED_WHATSAPP_CHANNEL_KEYWORDS.any { channelId.contains(it) }
                if (!allowed) return false
            }
        }

        val extras = notification.extras
        if (hasBusinessBroadcastIndicators(extras)) return false

        val title = readTitle(extras)
        val text = readText(extras)
        if (looksLikePromotion(title, text)) return false

        return isSavedContactOrDirectNumber(context, title)
    }

    fun isDirectPersonalStoredMessage(context: Context, title: String, text: String): Boolean {
        if (looksLikePromotion(title, text)) return false
        if (hasBusinessBroadcastIndicators(title, text)) return false
        return isSavedContactOrDirectNumber(context, title)
    }

    private fun hasBusinessBroadcastIndicators(extras: Bundle): Boolean {
        val sub = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.lowercase().orEmpty()
        val summary = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.lowercase().orEmpty()
        val info = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()?.lowercase().orEmpty()
        val combined = "$sub $summary $info"
        return combined.contains("business account") ||
            combined.contains("verified business") ||
            combined.contains("this business")
    }

    private fun hasBusinessBroadcastIndicators(title: String, text: String): Boolean {
        val combined = "${title.lowercase()} ${text.lowercase()}"
        return combined.contains("business account") || combined.contains("verified business")
    }

    private fun looksLikePromotion(title: String, text: String): Boolean {
        val combined = "${title.lowercase()} ${text.lowercase()}"
        return PROMO_TEXT_KEYWORDS.any { combined.contains(it) }
    }

    private fun isSavedContactOrDirectNumber(context: Context, title: String): Boolean {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return false
        if (looksLikePhoneNumber(normalizedTitle)) return true

        val contacts = loadContactNames(context)
        if (contacts.isEmpty()) {
            // No contacts permission: allow only if channel/title looks like a person (no brandy promo words)
            return !containsLikelyBrandName(normalizedTitle)
        }

        val titleKey = normalizeName(normalizedTitle)
        if (contacts.contains(titleKey)) return true

        return contacts.any { contact ->
            contact == titleKey ||
                titleKey.contains(contact) ||
                contact.contains(titleKey)
        }
    }

    private fun containsLikelyBrandName(title: String): Boolean {
        val lower = title.lowercase()
        return lower.contains(" ltd") ||
            lower.contains(" inc") ||
            lower.contains(" pvt") ||
            lower.contains(" store") ||
            lower.contains(" shop") ||
            lower.contains(" deals") ||
            lower.contains(" offers")
    }

    private fun looksLikePhoneNumber(value: String): Boolean {
        val compact = value.replace(Regex("[\\s()\\-]"), "")
        if (compact.startsWith("+") && compact.length >= 8 && compact.drop(1).all { it.isDigit() }) return true
        if (compact.length >= 7 && compact.all { it.isDigit() }) return true
        return false
    }

    private fun normalizeName(name: String): String =
        name.lowercase().replace(Regex("\\s+"), " ").trim()

    private fun readTitle(extras: Bundle): String =
        extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()

    private fun readText(extras: Bundle): String {
        var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        }
        return text
    }

    private fun loadContactNames(context: Context): Set<String> {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptySet()
        }

        val now = System.currentTimeMillis()
        val cached = contactCache.get()
        if (cached != null && now - cached.loadedAtMs < 30 * 60 * 1000) {
            return cached.names
        }

        val names = mutableSetOf<String>()
        val resolver = context.contentResolver
        val cursor: Cursor? = resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
            "${ContactsContract.Contacts.HAS_PHONE_NUMBER}=1",
            null,
            null
        )
        cursor?.use {
            val idx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            if (idx >= 0) {
                while (it.moveToNext()) {
                    val name = it.getString(idx)?.trim().orEmpty()
                    if (name.isNotEmpty()) names.add(normalizeName(name))
                }
            }
        }
        contactCache.set(ContactCache(names, now))
        return names
    }

    fun invalidateContactCache() {
        contactCache.set(null)
    }

    private data class ContactCache(
        val names: Set<String>,
        val loadedAtMs: Long
    )
}
