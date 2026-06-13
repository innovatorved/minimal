package com.example.util

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.example.BuildConfig
import com.example.deviceadmin.LauncherDeviceAdminReceiver

object LauncherUtils {

    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun requestDefaultLauncher(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                }
            }
        }
        context.startActivity(
            Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, LauncherDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(admin)
    }

    fun requestDeviceAdmin(context: Context): Boolean {
        if (isDeviceAdminActive(context)) {
            Toast.makeText(context, "device admin already enabled", Toast.LENGTH_SHORT).show()
            return true
        }
        val admin = ComponentName(context, LauncherDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Required for double-tap screen lock"
            )
        }
        val activity = context.findActivity()
        try {
            if (activity !== null) {
                activity.startActivity(intent)
            } else {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "could not open device admin setup", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    fun deviceAdminAdbCommand(): String =
        "adb shell dpm set-active-admin ${BuildConfig.APPLICATION_ID}/com.example.deviceadmin.LauncherDeviceAdminReceiver"

    fun lockScreen(context: Context): Boolean {
        if (!isDeviceAdminActive(context)) return false
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        dpm.lockNow()
        return true
    }
}
