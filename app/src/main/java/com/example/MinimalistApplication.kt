package com.example

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.system.SystemAppearanceController

class MinimalistApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        SystemAppearanceController(this).reapplySavedSettings()
    }
}
