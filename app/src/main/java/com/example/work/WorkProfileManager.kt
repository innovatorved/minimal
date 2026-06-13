package com.example.work

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager

class WorkProfileManager(private val context: Context) {

    private val userManager: UserManager =
        context.getSystemService(Context.USER_SERVICE) as UserManager
    private val launcherApps: LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    fun getWorkUserHandle(): UserHandle? {
        val current = Process.myUserHandle()
        return userManager.userProfiles.firstOrNull { profile ->
            profile != current && isManagedProfileUser(profile)
        }
    }

    private fun isManagedProfileUser(user: UserHandle): Boolean {
        return try {
            val method = UserManager::class.java.getMethod("isManagedProfile", UserHandle::class.java)
            method.invoke(userManager, user) as Boolean
        } catch (_: Exception) {
            val others = userManager.userProfiles.filter { it != Process.myUserHandle() }
            others.size == 1 && others.first() == user
        }
    }

    fun hasWorkProfile(): Boolean = getWorkUserHandle() != null

    fun isWorkProfilePaused(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val workUser = getWorkUserHandle() ?: return false
        return userManager.isQuietModeEnabled(workUser)
    }

    fun setWorkProfilePaused(paused: Boolean, onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onResult(false)
            return
        }
        val workUser = getWorkUserHandle() ?: run {
            onResult(false)
            return
        }
        val success = userManager.requestQuietModeEnabled(paused, workUser)
        onResult(success)
    }

    fun getWorkApps(): List<WorkApp> {
        val workUser = getWorkUserHandle() ?: return emptyList()
        return launcherApps.getActivityList(null, workUser)
            .mapNotNull { info ->
                val label = info.label?.toString()?.trim().orEmpty()
                if (label.isEmpty()) return@mapNotNull null
                WorkApp(
                    packageName = info.componentName.packageName,
                    appName = label,
                    componentName = info.componentName.flattenToString()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    fun launchWorkApp(packageName: String): Boolean {
        val workUser = getWorkUserHandle() ?: return false
        if (isWorkProfilePaused()) return false
        val activity = launcherApps.getActivityList(packageName, workUser).firstOrNull() ?: return false
        return try {
            launcherApps.startMainActivity(activity.componentName, workUser, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }
}
