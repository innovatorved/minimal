package com.example.ui

import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed interface AppScreen {
    object Home : AppScreen
    object AppDrawer : AppScreen
    object Focus : AppScreen
    object Widgets : AppScreen
    object Settings : AppScreen
    data class MindfulLaunch(val packageName: String, val appName: String, val limitMinutes: Int, val isScheduled: Boolean = false) : AppScreen
    data class BlockedScreen(val appName: String, val blockedUntil: String) : AppScreen
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LauncherDatabase.getDatabase(application)
    private val repository = LauncherRepository(db.launcherDao())
    private val prefs = application.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)

    // Current Screen Flow
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Home)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Navigation History
    private val screenHistory = Stack<AppScreen>()

    // App Database Lists
    val allAppConfigs: StateFlow<List<AppConfigEntity>> = repository.allAppConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteApps: StateFlow<List<AppConfigEntity>> = repository.favoriteApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Usage logs
    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val usageReport: StateFlow<List<AppUsageEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getUsageForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusSessions: StateFlow<List<FocusSessionEntity>> = repository.allFocusSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query in App Drawer
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active Focus session timer (countdown)
    private val _focusTimerSeconds = MutableStateFlow(0)
    val focusTimerSeconds: StateFlow<Int> = _focusTimerSeconds.asStateFlow()

    private val _focusTimerTotalSeconds = MutableStateFlow(0)
    val focusTimerTotalSeconds: StateFlow<Int> = _focusTimerTotalSeconds.asStateFlow()

    private val _isFocusActive = MutableStateFlow(false)
    val isFocusActive: StateFlow<Boolean> = _isFocusActive.asStateFlow()

    private var focusJob: Job? = null

    // Settings flows
    private val _is24HourFormat = MutableStateFlow(prefs.getBoolean("is_24h", false))
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    private val _isWallpaperMatchingEnabled = MutableStateFlow(prefs.getBoolean("is_wallpaper_matching", false))
    val isWallpaperMatchingEnabled: StateFlow<Boolean> = _isWallpaperMatchingEnabled.asStateFlow()

    private val _isWebsiteBlockingEnabled = MutableStateFlow(prefs.getBoolean("is_website_blocking", false))
    val isWebsiteBlockingEnabled: StateFlow<Boolean> = _isWebsiteBlockingEnabled.asStateFlow()

    private val _isScheduledBlockingEnabled = MutableStateFlow(prefs.getBoolean("is_scheduled_blocking", false))
    val isScheduledBlockingEnabled: StateFlow<Boolean> = _isScheduledBlockingEnabled.asStateFlow()

    // Scheduled Blocking range
    private val _scheduledStartHour = MutableStateFlow(prefs.getInt("sched_start_hour", 22)) // 10 PM
    val scheduledStartHour: StateFlow<Int> = _scheduledStartHour.asStateFlow()

    private val _scheduledStartMinute = MutableStateFlow(prefs.getInt("sched_start_min", 0))
    val scheduledStartMinute: StateFlow<Int> = _scheduledStartMinute.asStateFlow()

    private val _scheduledEndHour = MutableStateFlow(prefs.getInt("sched_end_hour", 6))     // 6 AM
    val scheduledEndHour: StateFlow<Int> = _scheduledEndHour.asStateFlow()

    private val _scheduledEndMinute = MutableStateFlow(prefs.getInt("sched_end_min", 0))
    val scheduledEndMinute: StateFlow<Int> = _scheduledEndMinute.asStateFlow()

    private val _scheduledDays = MutableStateFlow(
        prefs.getStringSet("sched_days", setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) ?: setOf()
    )
    val scheduledDays: StateFlow<Set<String>> = _scheduledDays.asStateFlow()

    // Shortcut Settings
    private val _shortcutPackageName = MutableStateFlow(prefs.getString("shortcut_pkg", null))
    val shortcutPackageName: StateFlow<String?> = _shortcutPackageName.asStateFlow()

    private val _shortcutAppName = MutableStateFlow(prefs.getString("shortcut_name", null))
    val shortcutAppName: StateFlow<String?> = _shortcutAppName.asStateFlow()

    // Active Widgets Settings (comma-separated list stored in prefs, default is clock, date, focus ring)
    private val _activeWidgets = MutableStateFlow<List<String>>(
        prefs.getString("active_widgets", "clock,date,battery,focus ring")?.split(",")?.filter { it.isNotEmpty() } ?: listOf("clock", "date", "battery", "focus ring")
    )
    val activeWidgets: StateFlow<List<String>> = _activeWidgets.asStateFlow()

    // Renamed Apps background list
    private val _renamedApps = MutableStateFlow<List<AppConfigEntity>>(emptyList())
    val renamedApps: StateFlow<List<AppConfigEntity>> = _renamedApps.asStateFlow()

    init {
        viewModelScope.launch {
            bootstrapApps(getApplication())
            seedInitialStatsIfEmpty()
            if (_isWallpaperMatchingEnabled.value) {
                applyBlackLockscreenWallpaper(getApplication())
            }
            // Listen to allAppConfigs changes to safely calculate renamedApps on a background thread!
            allAppConfigs.collect { configs ->
                calculateRenamedApps(configs)
            }
        }
    }

    // Navigation Utils
    fun navigateTo(screen: AppScreen, clearHistory: Boolean = false) {
        if (clearHistory) {
            screenHistory.clear()
        } else {
            screenHistory.push(_currentScreen.value)
        }
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (screenHistory.isNotEmpty()) {
            _currentScreen.value = screenHistory.pop()
            return true
        }
        if (_currentScreen.value != AppScreen.Home) {
            _currentScreen.value = AppScreen.Home
            return true
        }
        return false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Toggle 24 Hour format
    fun toggle24HourFormat() {
        val newValue = !_is24HourFormat.value
        _is24HourFormat.value = newValue
        prefs.edit().putBoolean("is_24h", newValue).apply()
    }

    // Wallpaper actions
    fun toggleWallpaperMatching(context: Context) {
        val newValue = !_isWallpaperMatchingEnabled.value
        _isWallpaperMatchingEnabled.value = newValue
        prefs.edit().putBoolean("is_wallpaper_matching", newValue).apply()
        if (newValue) {
            applyBlackLockscreenWallpaper(context)
            Toast.makeText(context, "Solid black lock screen wallpaper set", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyBlackLockscreenWallpaper(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            viewModelScope.launch {
                try {
                    val wm = WallpaperManager.getInstance(context)
                    val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(android.graphics.Color.BLACK)
                    }
                    wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Website blocking actions
    fun toggleWebsiteBlocking() {
        val newValue = !_isWebsiteBlockingEnabled.value
        _isWebsiteBlockingEnabled.value = newValue
        prefs.edit().putBoolean("is_website_blocking", newValue).apply()
    }

    // Schedule actions
    fun toggleScheduledBlocking() {
        val newValue = !_isScheduledBlockingEnabled.value
        _isScheduledBlockingEnabled.value = newValue
        prefs.edit().putBoolean("is_scheduled_blocking", newValue).apply()
    }

    fun saveScheduleTime(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        _scheduledStartHour.value = startHour
        _scheduledStartMinute.value = startMinute
        _scheduledEndHour.value = endHour
        _scheduledEndMinute.value = endMinute
        prefs.edit()
            .putInt("sched_start_hour", startHour)
            .putInt("sched_start_min", startMinute)
            .putInt("sched_end_hour", endHour)
            .putInt("sched_end_min", endMinute)
            .apply()
    }

    fun toggleScheduledDay(day: String) {
        val current = _scheduledDays.value.toMutableSet()
        if (current.contains(day)) {
            current.remove(day)
        } else {
            current.add(day)
        }
        _scheduledDays.value = current
        prefs.edit().putStringSet("sched_days", current).apply()
    }

    // Custom Shortcut selection
    fun assignShortcut(appName: String, packageName: String) {
        _shortcutPackageName.value = packageName
        _shortcutAppName.value = appName
        prefs.edit()
            .putString("shortcut_pkg", packageName)
            .putString("shortcut_name", appName)
            .apply()
    }

    fun clearShortcut() {
        _shortcutPackageName.value = null
        _shortcutAppName.value = null
        prefs.edit()
            .remove("shortcut_pkg")
            .remove("shortcut_name")
            .apply()
    }

    // Widget configurations
    fun addWidget(widgetId: String) {
        val current = _activeWidgets.value.toMutableList()
        if (!current.contains(widgetId)) {
            current.add(widgetId)
            _activeWidgets.value = current
            prefs.edit().putString("active_widgets", current.joinToString(",")).apply()
        }
    }

    fun removeWidget(widgetId: String) {
        val current = _activeWidgets.value.toMutableList()
        current.remove(widgetId)
        _activeWidgets.value = current
        prefs.edit().putString("active_widgets", current.joinToString(",")).apply()
    }

    fun moveWidget(widgetId: String, moveUp: Boolean) {
        val current = _activeWidgets.value.toMutableList()
        val index = current.indexOf(widgetId)
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex in 0 until current.size) {
            Collections.swap(current, index, targetIndex)
            _activeWidgets.value = current
            prefs.edit().putString("active_widgets", current.joinToString(",")).apply()
        }
    }

    // Pinned Apps sorting & management
    fun pinApp(app: AppConfigEntity) {
        viewModelScope.launch {
            val currentPinned = favoriteApps.value
            val maxOrder = currentPinned.maxOfOrNull { it.pinnedOrder } ?: 0
            repository.updateAppConfig(app.copy(isFavorite = true, pinnedOrder = maxOrder + 1))
        }
    }

    fun unpinApp(app: AppConfigEntity) {
        viewModelScope.launch {
            repository.updateAppConfig(app.copy(isFavorite = false, pinnedOrder = 0))
        }
    }

    fun movePinnedApp(app: AppConfigEntity, moveUp: Boolean) {
        viewModelScope.launch {
            val currentPinned = favoriteApps.value.toMutableList()
            val index = currentPinned.indexOfFirst { it.packageName == app.packageName }
            if (index == -1) return@launch
            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex in 0 until currentPinned.size) {
                val other = currentPinned[targetIndex]
                val orderTemp = app.pinnedOrder
                repository.updateAppConfig(app.copy(pinnedOrder = other.pinnedOrder))
                repository.updateAppConfig(other.copy(pinnedOrder = orderTemp))
            }
        }
    }

    fun resetAppLabel(app: AppConfigEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            
            val simulatedDefaults = mapOf(
                "com.instagram.android" to "Instagram",
                "com.zhiliaoapp.musically" to "TikTok",
                "com.google.android.youtube" to "YouTube",
                "com.twitter.android" to "Twitter / X",
                "com.android.chrome" to "Chrome Browser",
                "com.whatsapp" to "WhatsApp Messenger",
                "com.spotify.music" to "Spotify",
                "com.duolingo" to "Duolingo Learning",
                "com.amazon.kindle" to "Kindle Reader",
                "com.netflix.mediaclient" to "Netflix",
                "org.telegram.messenger" to "Telegram"
            )

            val simulatedDefault = simulatedDefaults[app.packageName]
            val origName = if (simulatedDefault != null) {
                simulatedDefault
            } else {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                val component = launchIntent?.component
                if (component != null) {
                    try {
                        pm.getActivityInfo(component, 0).loadLabel(pm)?.toString() ?: app.appName
                    } catch (e: Exception) {
                        app.appName
                    }
                } else {
                    app.appName
                }
            }
            repository.updateAppConfig(app.copy(appName = origName))
        }
    }

    fun resetAppLabel(app: AppConfigEntity, defaultName: String) {
        viewModelScope.launch {
            repository.updateAppConfig(app.copy(appName = defaultName))
        }
    }

    fun renameApp(app: AppConfigEntity, newName: String) {
        viewModelScope.launch {
            repository.updateAppConfig(app.copy(appName = newName))
        }
    }

    fun toggleBlocked(app: AppConfigEntity) {
        viewModelScope.launch {
            repository.updateAppConfig(app.copy(isBlocked = !app.isBlocked))
        }
    }

    fun updateDailyLimit(app: AppConfigEntity, limitMinutes: Int) {
        viewModelScope.launch {
            repository.updateAppConfig(app.copy(dailyLimitMinutes = limitMinutes))
        }
    }

    // App Launch checking
    fun tryLaunchApp(context: Context, packageName: String, appName: String) {
        viewModelScope.launch {
            val config = allAppConfigs.value.firstOrNull { it.packageName == packageName }
            
            // Check if scheduled blocking is active and fits schedule
            val isScheduledBlocked = config?.isBlocked == true && checkIfInsideSchedule()
            
            if (isScheduledBlocked) {
                // Fully blocked - no bypass
                val untilString = String.format("%02d:%02d", _scheduledEndHour.value, _scheduledEndMinute.value)
                navigateTo(AppScreen.BlockedScreen(appName, untilString))
            } else if (config != null && config.dailyLimitMinutes > 0) {
                // Intercept with mindful delay
                navigateTo(AppScreen.MindfulLaunch(packageName, appName, config.dailyLimitMinutes))
            } else {
                launchAppDirect(context, packageName, appName)
            }
        }
    }

    fun launchAppDirect(context: Context, packageName: String, appName: String) {
        viewModelScope.launch {
            repository.recordAppLaunch(packageName, appName, getTodayDateString())
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open $appName", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Opened simulated $appName", Toast.LENGTH_SHORT).show()
            }
            // Navigate back to Home
            navigateTo(AppScreen.Home, clearHistory = true)
        }
    }

    // Scheduled Blocking Check
    private fun checkIfInsideSchedule(): Boolean {
        if (!_isScheduledBlockingEnabled.value) return false
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMin = cal.get(Calendar.MINUTE)
        
        // Check day
        val daysFormat = SimpleDateFormat("EEE", Locale.US)
        val todayName = daysFormat.format(cal.time) // e.g. "Mon"
        if (!_scheduledDays.value.contains(todayName)) return false

        val currentTotalMinutes = currentHour * 60 + currentMin
        val startTotalMinutes = _scheduledStartHour.value * 60 + _scheduledStartMinute.value
        val endTotalMinutes = _scheduledEndHour.value * 60 + _scheduledEndMinute.value

        return if (startTotalMinutes <= endTotalMinutes) {
            currentTotalMinutes in startTotalMinutes..endTotalMinutes
        } else {
            // Overnights (e.g. 10 PM to 6 AM)
            currentTotalMinutes >= startTotalMinutes || currentTotalMinutes <= endTotalMinutes
        }
    }

    // Focus session Actions
    fun startFocusSession(minutes: Int) {
        _focusTimerTotalSeconds.value = minutes * 60
        _focusTimerSeconds.value = minutes * 60
        _isFocusActive.value = true
        _currentScreen.value = AppScreen.Home // Focus ring on Home screen fills during focus session

        focusJob?.cancel()
        focusJob = viewModelScope.launch {
            while (_focusTimerSeconds.value > 0) {
                delay(1000)
                _focusTimerSeconds.value -= 1
            }
            completeFocusSession(minutes)
        }
    }

    fun cancelFocusSession() {
        focusJob?.cancel()
        _isFocusActive.value = false
        _focusTimerSeconds.value = 0
        navigateTo(AppScreen.Home, clearHistory = true)
    }

    private suspend fun completeFocusSession(minutes: Int) {
        _isFocusActive.value = false
        repository.insertFocusSession(
            FocusSessionEntity(
                date = getTodayDateString(),
                durationMinutes = minutes,
                isCompleted = true
            )
        )
        // System notification / toast
        _focusTimerSeconds.value = 0
        navigateTo(AppScreen.Home, clearHistory = true)
    }

    // App Database Bootstrap and Seeders
    private suspend fun bootstrapApps(context: Context) {
        val existingConfigs = db.launcherDao().getAllAppConfigs().first()

        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launchables = pm.queryIntentActivities(intent, 0) ?: emptyList()

        val realConfigs = launchables.mapNotNull { info ->
            val pName = info.activityInfo?.packageName ?: return@mapNotNull null
            val label = info.loadLabel(pm)?.toString() ?: ""
            val existing = existingConfigs.find { it.packageName == pName }
            AppConfigEntity(
                packageName = pName,
                appName = existing?.appName ?: label,
                isFavorite = existing?.isFavorite ?: false,
                isBlocked = existing?.isBlocked ?: false,
                dailyLimitMinutes = existing?.dailyLimitMinutes ?: 0,
                isSystemApp = true,
                isHidden = existing?.isHidden ?: false,
                pinnedOrder = existing?.pinnedOrder ?: 0
            )
        }

        val simulatedApps = listOf(
            AppConfigEntity("com.instagram.android", "Instagram", isFavorite = true, isBlocked = true, dailyLimitMinutes = 15, pinnedOrder = 1),
            AppConfigEntity("com.zhiliaoapp.musically", "TikTok", isFavorite = false, isBlocked = true, dailyLimitMinutes = 15),
            AppConfigEntity("com.google.android.youtube", "YouTube", isFavorite = true, isBlocked = false, dailyLimitMinutes = 30, pinnedOrder = 2),
            AppConfigEntity("com.twitter.android", "Twitter / X", isFavorite = false, isBlocked = true),
            AppConfigEntity("com.android.chrome", "Chrome Browser", isFavorite = true, isBlocked = false, pinnedOrder = 3),
            AppConfigEntity("com.whatsapp", "WhatsApp Messenger", isFavorite = true, isBlocked = false, pinnedOrder = 4),
            AppConfigEntity("com.spotify.music", "Spotify", isFavorite = false, isBlocked = false),
            AppConfigEntity("com.duolingo", "Duolingo Learning", isFavorite = false, isBlocked = false),
            AppConfigEntity("com.amazon.kindle", "Kindle Reader", isFavorite = false, isBlocked = false),
            AppConfigEntity("com.netflix.mediaclient", "Netflix", isFavorite = false, isBlocked = false),
            AppConfigEntity("org.telegram.messenger", "Telegram", isFavorite = false, isBlocked = false)
        )

        val combined = (realConfigs + simulatedApps).distinctBy { it.packageName }.map { config ->
            val existing = existingConfigs.find { it.packageName == config.packageName }
            if (existing != null) {
                config.copy(
                    appName = existing.appName,
                    isFavorite = existing.isFavorite,
                    isBlocked = existing.isBlocked,
                    dailyLimitMinutes = existing.dailyLimitMinutes,
                    isHidden = existing.isHidden,
                    pinnedOrder = existing.pinnedOrder
                )
            } else {
                config
            }
        }
        repository.insertAppConfigs(combined)
    }

    private suspend fun seedInitialStatsIfEmpty() {
        val dateToday = getTodayDateString()
        val todayUsage = db.launcherDao().getUsageForDate(dateToday).first()
        if (todayUsage.isEmpty()) {
            val yesterday = getYesterdayDateString()
            val usageSeed = listOf(
                AppUsageEntity(packageName = "com.instagram.android", appName = "Instagram", date = dateToday, openCount = 28, durationMinutes = 75, blockedTries = 4),
                AppUsageEntity(packageName = "com.zhiliaoapp.musically", appName = "TikTok", date = dateToday, openCount = 14, durationMinutes = 45, blockedTries = 9),
                AppUsageEntity(packageName = "com.android.chrome", appName = "Chrome Browser", date = dateToday, openCount = 8, durationMinutes = 20, blockedTries = 0),
                AppUsageEntity(packageName = "com.google.android.youtube", appName = "YouTube", date = dateToday, openCount = 6, durationMinutes = 35, blockedTries = 2),
                AppUsageEntity(packageName = "com.duolingo", appName = "Duolingo Learning", date = dateToday, openCount = 3, durationMinutes = 15, blockedTries = 0),
                AppUsageEntity(packageName = "com.amazon.kindle", appName = "Kindle Reader", date = dateToday, openCount = 2, durationMinutes = 30, blockedTries = 0)
            )
            for (seed in usageSeed) {
                db.launcherDao().insertUsage(seed)
            }
            repository.insertFocusSession(FocusSessionEntity(date = dateToday, durationMinutes = 25, isCompleted = true))
        }
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    private fun calculateRenamedApps(configs: List<AppConfigEntity>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            
            val simulatedDefaults = mapOf(
                "com.instagram.android" to "Instagram",
                "com.zhiliaoapp.musically" to "TikTok",
                "com.google.android.youtube" to "YouTube",
                "com.twitter.android" to "Twitter / X",
                "com.android.chrome" to "Chrome Browser",
                "com.whatsapp" to "WhatsApp Messenger",
                "com.spotify.music" to "Spotify",
                "com.duolingo" to "Duolingo Learning",
                "com.amazon.kindle" to "Kindle Reader",
                "com.netflix.mediaclient" to "Netflix",
                "org.telegram.messenger" to "Telegram"
            )

            val renamed = configs.filter { appEntity ->
                val simulatedDefault = simulatedDefaults[appEntity.packageName]
                if (simulatedDefault != null) {
                    appEntity.appName != simulatedDefault
                } else {
                    val launchIntent = pm.getLaunchIntentForPackage(appEntity.packageName)
                    val component = launchIntent?.component
                    if (component != null) {
                        try {
                            val origName = pm.getActivityInfo(component, 0).loadLabel(pm)?.toString() ?: appEntity.appName
                            appEntity.appName != origName
                        } catch (e: Exception) {
                            false
                        }
                    } else {
                        false
                    }
                }
            }
            _renamedApps.value = renamed
        }
    }
}
