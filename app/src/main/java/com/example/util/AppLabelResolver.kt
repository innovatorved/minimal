package com.example.util

import android.content.pm.PackageManager

object AppLabelResolver {

    fun originalLabel(pm: PackageManager, packageName: String, fallback: String): String {
        val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return fallback
        val component = launchIntent.component ?: return fallback
        return try {
            pm.getActivityInfo(component, 0).loadLabel(pm)?.toString() ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    fun isRenamed(pm: PackageManager, packageName: String, currentName: String): Boolean {
        val original = originalLabel(pm, packageName, currentName)
        return currentName != original
    }
}
