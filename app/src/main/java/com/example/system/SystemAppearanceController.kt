package com.example.system

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.BuildConfig

sealed class AppearanceResult {
    data object Success : AppearanceResult()
    data class PermissionRequired(val message: String, val adbCommand: String? = null) : AppearanceResult()
    data class Error(val message: String) : AppearanceResult()
}

class SystemAppearanceController(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasWriteSecureSettings(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_SECURE_SETTINGS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun isWallpaperMatchingEnabled(): Boolean =
        prefs.getBoolean(KEY_WALLPAPER_MATCHING, true)

    fun isGrayscaleEnabled(): Boolean =
        prefs.getBoolean(KEY_GRAYSCALE, false)

    fun isDarkModeEnabled(): Boolean =
        prefs.getBoolean(KEY_DARK_MODE, false)

    fun setWallpaperMatching(enabled: Boolean): AppearanceResult {
        prefs.edit().putBoolean(KEY_WALLPAPER_MATCHING, enabled).apply()
        return if (enabled) ensureBlackWallpapers() else AppearanceResult.Success
    }

    /** Applies plain black home wallpaper + minimalist clock lock wallpaper matching the launcher. */
    fun ensureBlackWallpapers(): AppearanceResult {
        prefs.edit().putBoolean(KEY_WALLPAPER_MATCHING, true).apply()
        return applyMatchingWallpapers()
    }

    fun applyMatchingWallpapers(): AppearanceResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return AppearanceResult.Error("Wallpaper requires Android 7.0+")
        }
        return try {
            val wm = WallpaperManager.getInstance(context)
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels.coerceAtLeast(1)
            val height = metrics.heightPixels.coerceAtLeast(1)

            val blackBitmap = createBlackWallpaper(width, height)

            wm.setBitmap(blackBitmap, null, true, WallpaperManager.FLAG_LOCK)
            wm.setBitmap(blackBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            AppearanceResult.Success
        } catch (e: SecurityException) {
            AppearanceResult.Error("Wallpaper permission denied — open settings > wallpaper and allow this app")
        } catch (e: Exception) {
            AppearanceResult.Error(e.message ?: "Failed to set wallpaper")
        }
    }

    fun applyBlackWallpaper(): AppearanceResult = applyMatchingWallpapers()

    fun openLockScreenSettings() {
        val intents = listOf(
            Intent("android.settings.LOCK_SCREEN_SETTINGS"),
            Intent(Settings.ACTION_DISPLAY_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // try next
            }
        }
    }

    fun isSystemDarkSchemaEnabled(): Boolean =
        prefs.getBoolean(KEY_SYSTEM_DARK_SCHEMA, false)

    /**
     * Enables system-wide dark + desaturated look (grayscale + night mode).
     * Saves the phone's current values before first enable so [disableSystemDarkSchema] can restore them.
     */
    fun setSystemDarkSchema(enabled: Boolean): AppearanceResult {
        if (!hasWriteSecureSettings()) {
            return AppearanceResult.PermissionRequired(
                message = "ADB permission required for system-wide dark schema",
                adbCommand = ADB_GRANT_COMMAND
            )
        }
        return if (enabled) enableSystemDarkSchema() else disableSystemDarkSchema()
    }

    private fun enableSystemDarkSchema(): AppearanceResult {
        if (!prefs.getBoolean(KEY_SYSTEM_BACKUP_SAVED, false)) {
            saveSystemStateBackup()
        }
        val grayscale = setGrayscale(true)
        if (grayscale !is AppearanceResult.Success) return grayscale
        val dark = setDarkMode(true)
        if (dark !is AppearanceResult.Success) return dark
        prefs.edit().putBoolean(KEY_SYSTEM_DARK_SCHEMA, true).apply()
        return AppearanceResult.Success
    }

    private fun disableSystemDarkSchema(): AppearanceResult {
        restoreSystemStateBackup()
        prefs.edit()
            .putBoolean(KEY_SYSTEM_DARK_SCHEMA, false)
            .putBoolean(KEY_GRAYSCALE, false)
            .putBoolean(KEY_DARK_MODE, false)
            .apply()
        return AppearanceResult.Success
    }

    private fun saveSystemStateBackup() {
        val resolver = context.contentResolver
        prefs.edit()
            .putInt(KEY_BACKUP_NIGHT_MODE, Settings.Secure.getInt(resolver, "ui_night_mode", UI_NIGHT_MODE_NO))
            .putInt(
                KEY_BACKUP_DALTONIZER_ENABLED,
                Settings.Secure.getInt(resolver, "accessibility_display_daltonizer_enabled", 0)
            )
            .putInt(
                KEY_BACKUP_DALTONIZER,
                Settings.Secure.getInt(resolver, "accessibility_display_daltonizer", 0)
            )
            .putBoolean(KEY_SYSTEM_BACKUP_SAVED, true)
            .apply()
    }

    private fun restoreSystemStateBackup() {
        if (!hasWriteSecureSettings()) return
        val resolver = context.contentResolver
        try {
            Settings.Secure.putInt(
                resolver,
                "ui_night_mode",
                prefs.getInt(KEY_BACKUP_NIGHT_MODE, UI_NIGHT_MODE_NO)
            )
            Settings.Secure.putInt(
                resolver,
                "accessibility_display_daltonizer_enabled",
                prefs.getInt(KEY_BACKUP_DALTONIZER_ENABLED, 0)
            )
            Settings.Secure.putInt(
                resolver,
                "accessibility_display_daltonizer",
                prefs.getInt(KEY_BACKUP_DALTONIZER, 0)
            )
        } catch (_: Exception) {
            setGrayscale(false)
            setDarkMode(false)
        }
    }

    fun adbGrantCommand(): String = ADB_GRANT_COMMAND

    fun setGrayscale(enabled: Boolean): AppearanceResult {
        if (!hasWriteSecureSettings()) {
            return AppearanceResult.PermissionRequired(
                message = "Grant WRITE_SECURE_SETTINGS via ADB to enable system-wide grayscale",
                adbCommand = "adb shell pm grant ${BuildConfig.APPLICATION_ID} android.permission.WRITE_SECURE_SETTINGS"
            )
        }
        return try {
            val resolver = context.contentResolver
            if (enabled) {
                Settings.Secure.putInt(resolver, "accessibility_display_daltonizer_enabled", 1)
                Settings.Secure.putInt(resolver, "accessibility_display_daltonizer", 0)
            } else {
                Settings.Secure.putInt(resolver, "accessibility_display_daltonizer_enabled", 0)
            }
            prefs.edit().putBoolean(KEY_GRAYSCALE, enabled).apply()
            AppearanceResult.Success
        } catch (e: Exception) {
            AppearanceResult.Error(e.message ?: "Failed to set grayscale")
        }
    }

    fun setDarkMode(enabled: Boolean): AppearanceResult {
        if (!hasWriteSecureSettings()) {
            return AppearanceResult.PermissionRequired(
                message = "Grant WRITE_SECURE_SETTINGS via ADB to enable system dark mode",
                adbCommand = "adb shell pm grant ${BuildConfig.APPLICATION_ID} android.permission.WRITE_SECURE_SETTINGS"
            )
        }
        return try {
            val resolver = context.contentResolver
            Settings.Secure.putInt(
                resolver,
                "ui_night_mode",
                if (enabled) UI_NIGHT_MODE_YES else UI_NIGHT_MODE_NO
            )
            prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
            AppearanceResult.Success
        } catch (e: Exception) {
            AppearanceResult.Error(e.message ?: "Failed to set dark mode")
        }
    }

    fun reapplySavedSettings() {
        if (isWallpaperMatchingEnabled()) ensureBlackWallpapers()
        if (hasWriteSecureSettings() && isSystemDarkSchemaEnabled()) {
            setGrayscale(true)
            setDarkMode(true)
        }
    }

    fun openAccessibilitySettings() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openDisplaySettings() {
        context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun createBlackWallpaper(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            eraseColor(AndroidColor.BLACK)
        }
    }

    companion object {
        private const val PREFS_NAME = "system_appearance"
        private const val KEY_WALLPAPER_MATCHING = "wallpaper_matching"
        private const val KEY_GRAYSCALE = "grayscale"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SYSTEM_DARK_SCHEMA = "system_dark_schema"
        private const val KEY_SYSTEM_BACKUP_SAVED = "system_backup_saved"
        private const val KEY_BACKUP_NIGHT_MODE = "backup_night_mode"
        private const val KEY_BACKUP_DALTONIZER_ENABLED = "backup_daltonizer_enabled"
        private const val KEY_BACKUP_DALTONIZER = "backup_daltonizer"
        private const val UI_NIGHT_MODE_YES = 2
        private const val UI_NIGHT_MODE_NO = 1
        private val ADB_GRANT_COMMAND =
            "adb shell pm grant ${BuildConfig.APPLICATION_ID} android.permission.WRITE_SECURE_SETTINGS"
    }
}
