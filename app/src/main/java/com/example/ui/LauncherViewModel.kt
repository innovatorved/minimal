package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.analytics.LauncherEventType
import com.example.analytics.UsageAnalyticsRepository
import com.example.analytics.UsageInsightsSnapshot
import com.example.data.*
import com.example.service.SessionMonitorService
import com.example.system.AppearanceResult
import com.example.system.SystemAppearanceController
import com.example.ui.navigation.AppScreen
import com.example.ui.navigation.LauncherNavigator
import com.example.ui.navigation.NavigationDirection
import com.example.usage.UsageStatsRepository
import com.example.util.AppLabelResolver
import com.example.util.LauncherUtils
import com.example.util.todayDateString
import com.example.work.WorkApp
import com.example.work.WorkProfileManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val WEBSITE_BLOCKING_V2_NOTE =
            "Website blocking requires v2 setup (AccessibilityService or local VPN). Toggle saved for future release."
    }

    private val db = LauncherDatabase.getDatabase(application)
    private val repository = LauncherRepository(db.launcherDao())
    private val prefs = application.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)
    private val usageStatsRepository = UsageStatsRepository(application, db.launcherDao())
    private val analyticsRepository = UsageAnalyticsRepository(application, db.launcherDao(), usageStatsRepository)
    private val appearanceController = SystemAppearanceController(application)
    private val workProfileManager = WorkProfileManager(application)

    private val navigator = LauncherNavigator(
        if (prefs.getBoolean("onboarding_complete", false)) AppScreen.Home else AppScreen.Onboarding
    )

    val currentScreen: StateFlow<AppScreen> = navigator.currentScreen
    val navigationDirection: StateFlow<NavigationDirection> = navigator.navigationDirection

    val allAppConfigs: StateFlow<List<AppConfigEntity>> = repository.allAppConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteApps: StateFlow<List<AppConfigEntity>> = repository.favoriteApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedDate = MutableStateFlow(todayDateString())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val usageReport: StateFlow<List<AppUsageEntity>> = selectedDate
        .flatMapLatest { date -> repository.getUsageForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mutedNotifications: StateFlow<List<MutedNotificationEntity>> = repository.mutedNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _focusTimerSeconds = MutableStateFlow(0)
    val focusTimerSeconds: StateFlow<Int> = _focusTimerSeconds.asStateFlow()

    private val _isFocusActive = MutableStateFlow(false)
    val isFocusActive: StateFlow<Boolean> = _isFocusActive.asStateFlow()

    private val _showBreathingPause = MutableStateFlow(false)
    val showBreathingPause: StateFlow<Boolean> = _showBreathingPause.asStateFlow()

    private val _totalScreenTimeMinutes = MutableStateFlow(0)
    val totalScreenTimeMinutes: StateFlow<Int> = _totalScreenTimeMinutes.asStateFlow()

    private var focusJob: Job? = null
    private var usageSyncJob: Job? = null

    private val _is24HourFormat = MutableStateFlow(prefs.getBoolean("is_24h", false))
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    private val _dailyGoalMinutes = MutableStateFlow(prefs.getInt("daily_goal_minutes", 180))
    val dailyGoalMinutes: StateFlow<Int> = _dailyGoalMinutes.asStateFlow()

    private val _isWallpaperMatchingEnabled = MutableStateFlow(appearanceController.isWallpaperMatchingEnabled())
    val isWallpaperMatchingEnabled: StateFlow<Boolean> = _isWallpaperMatchingEnabled.asStateFlow()

    private val _isSystemDarkSchemaEnabled = MutableStateFlow(appearanceController.isSystemDarkSchemaEnabled())
    val isSystemDarkSchemaEnabled: StateFlow<Boolean> = _isSystemDarkSchemaEnabled.asStateFlow()

    private val _isWebsiteBlockingEnabled = MutableStateFlow(prefs.getBoolean("is_website_blocking", false))
    val isWebsiteBlockingEnabled: StateFlow<Boolean> = _isWebsiteBlockingEnabled.asStateFlow()

    private val _isScheduledBlockingEnabled = MutableStateFlow(prefs.getBoolean("is_scheduled_blocking", false))
    val isScheduledBlockingEnabled: StateFlow<Boolean> = _isScheduledBlockingEnabled.asStateFlow()

    private val _scheduledStartHour = MutableStateFlow(prefs.getInt("sched_start_hour", 22))
    val scheduledStartHour: StateFlow<Int> = _scheduledStartHour.asStateFlow()

    private val _scheduledStartMinute = MutableStateFlow(prefs.getInt("sched_start_min", 0))
    val scheduledStartMinute: StateFlow<Int> = _scheduledStartMinute.asStateFlow()

    private val _scheduledEndHour = MutableStateFlow(prefs.getInt("sched_end_hour", 6))
    val scheduledEndHour: StateFlow<Int> = _scheduledEndHour.asStateFlow()

    private val _scheduledEndMinute = MutableStateFlow(prefs.getInt("sched_end_min", 0))
    val scheduledEndMinute: StateFlow<Int> = _scheduledEndMinute.asStateFlow()

    private val _scheduledDays = MutableStateFlow(
        prefs.getStringSet("sched_days", setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) ?: setOf()
    )
    val scheduledDays: StateFlow<Set<String>> = _scheduledDays.asStateFlow()

    private val _shortcutPackageName = MutableStateFlow(prefs.getString("shortcut_pkg", null))
    val shortcutPackageName: StateFlow<String?> = _shortcutPackageName.asStateFlow()

    private val _shortcutAppName = MutableStateFlow(prefs.getString("shortcut_name", null))
    val shortcutAppName: StateFlow<String?> = _shortcutAppName.asStateFlow()

    private val _activeWidgets = MutableStateFlow<List<String>>(
        prefs.getString("active_widgets", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    )
    val activeWidgets: StateFlow<List<String>> = _activeWidgets.asStateFlow()

    private val _renamedApps = MutableStateFlow<List<AppConfigEntity>>(emptyList())
    val renamedApps: StateFlow<List<AppConfigEntity>> = _renamedApps.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(usageStatsRepository.hasUsageAccess())
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    val hasSecureSettings: Boolean get() = appearanceController.hasWriteSecureSettings()

    val adbGrantCommand: String get() = appearanceController.adbGrantCommand()

    private val _hasWorkProfile = MutableStateFlow(workProfileManager.hasWorkProfile())
    val hasWorkProfile: StateFlow<Boolean> = _hasWorkProfile.asStateFlow()

    private val _isWorkProfilePaused = MutableStateFlow(workProfileManager.isWorkProfilePaused())
    val isWorkProfilePaused: StateFlow<Boolean> = _isWorkProfilePaused.asStateFlow()

    private val _isWorkProfileUnlocked = MutableStateFlow(false)
    val isWorkProfileUnlocked: StateFlow<Boolean> = _isWorkProfileUnlocked.asStateFlow()

    private val _workApps = MutableStateFlow<List<WorkApp>>(emptyList())
    val workApps: StateFlow<List<WorkApp>> = _workApps.asStateFlow()

    private val _workSearchQuery = MutableStateFlow("")
    val workSearchQuery: StateFlow<String> = _workSearchQuery.asStateFlow()

    private val _insights = MutableStateFlow<UsageInsightsSnapshot?>(null)
    val insights: StateFlow<UsageInsightsSnapshot?> = _insights.asStateFlow()

    init {
        val legacyWidgets = prefs.getString("active_widgets", "") ?: ""
        if (legacyWidgets.contains("clock") || legacyWidgets.contains("focus ring")) {
            prefs.edit().putString("active_widgets", "").apply()
            _activeWidgets.value = emptyList()
        }
        viewModelScope.launch {
            bootstrapApps(getApplication())
            allAppConfigs.collect { configs -> calculateRenamedApps(configs) }
        }
        startUsageSync()
        appearanceController.ensureBlackWallpapers()
        _isWallpaperMatchingEnabled.value = true
        refreshWorkProfileState()
        viewModelScope.launch { runAnalyticsBackfillIfNeeded() }
    }

    fun refreshInsights() {
        viewModelScope.launch {
            _insights.value = analyticsRepository.loadInsights(_dailyGoalMinutes.value)
            _totalScreenTimeMinutes.value = _insights.value?.todayScreenMinutes
                ?: usageStatsRepository.getTotalScreenTimeMinutes()
        }
    }

    fun logInsightsView() {
        viewModelScope.launch {
            analyticsRepository.logEvent(LauncherEventType.INSIGHTS_VIEW)
        }
    }

    fun deleteAnalyticsHistory(context: Context) {
        viewModelScope.launch {
            analyticsRepository.clearAllAnalyticsHistory()
            refreshInsights()
            Toast.makeText(context, "insights history deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun runAnalyticsBackfillIfNeeded() {
        if (!usageStatsRepository.hasUsageAccess()) return
        if (!prefs.getBoolean("analytics_backfill_done", false)) {
            usageStatsRepository.backfillRecentDays(30)
            analyticsRepository.rebuildRecentRollups(30, _dailyGoalMinutes.value)
            prefs.edit().putBoolean("analytics_backfill_done", true).apply()
        }
        refreshInsights()
    }

    private fun logLauncherEvent(type: String, metadata: String? = null) {
        viewModelScope.launch { analyticsRepository.logEvent(type, metadata) }
    }

    fun onResume() {
        _hasUsageAccess.value = usageStatsRepository.hasUsageAccess()
        appearanceController.ensureBlackWallpapers()
        _isWallpaperMatchingEnabled.value = true
        refreshWorkProfileState()
        viewModelScope.launch {
            if (usageStatsRepository.hasUsageAccess()) {
                usageStatsRepository.syncTodayUsage(todayDateString())
                analyticsRepository.rebuildRollupForDate(todayDateString(), _dailyGoalMinutes.value)
                _totalScreenTimeMinutes.value = usageStatsRepository.getTotalScreenTimeMinutes()
                _insights.value = analyticsRepository.loadInsights(_dailyGoalMinutes.value)
            }
        }
    }

    fun triggerBreathingPause() {
        viewModelScope.launch {
            _showBreathingPause.value = true
            delay(600)
            delay(600)
            _showBreathingPause.value = false
        }
    }

    private fun startUsageSync() {
        usageSyncJob?.cancel()
        usageSyncJob = viewModelScope.launch {
            while (true) {
                if (usageStatsRepository.hasUsageAccess()) {
                    val today = todayDateString()
                    usageStatsRepository.syncTodayUsage(today)
                    analyticsRepository.rebuildRollupForDate(today, _dailyGoalMinutes.value)
                    _totalScreenTimeMinutes.value = usageStatsRepository.getTotalScreenTimeMinutes()
                    _insights.value = analyticsRepository.loadInsights(_dailyGoalMinutes.value)
                }
                delay(60_000)
            }
        }
    }

    fun completeOnboarding(context: Context) {
        prefs.edit().putBoolean("onboarding_complete", true).apply()
        appearanceController.ensureBlackWallpapers()
        _isWallpaperMatchingEnabled.value = true
        if (!LauncherUtils.isDefaultLauncher(context)) {
            LauncherUtils.requestDefaultLauncher(context)
        }
        navigator.setScreen(AppScreen.Home)
    }

    fun navigateTo(screen: AppScreen, clearHistory: Boolean = false) {
        navigator.navigateTo(screen, clearHistory) { lockWorkProfile() }
    }

    fun goHome() {
        navigator.goHome { lockWorkProfile() }
    }

    fun openDrawer() {
        navigator.openDrawer()
        logLauncherEvent(LauncherEventType.DRAWER_OPEN)
    }

    fun onSwipeUp() {
        navigator.onSwipeUp { lockWorkProfile() }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshWorkProfileState() {
        _hasWorkProfile.value = workProfileManager.hasWorkProfile()
        _isWorkProfilePaused.value = workProfileManager.isWorkProfilePaused()
        if (_hasWorkProfile.value && !_isWorkProfilePaused.value) {
            _workApps.value = workProfileManager.getWorkApps()
        } else {
            _workApps.value = emptyList()
        }
        if (_isWorkProfilePaused.value) {
            lockWorkProfile()
        }
    }

    fun updateWorkSearchQuery(query: String) {
        _workSearchQuery.value = query
    }

    fun unlockWorkProfile() {
        _isWorkProfileUnlocked.value = true
        _workApps.value = workProfileManager.getWorkApps()
    }

    fun lockWorkProfile() {
        _isWorkProfileUnlocked.value = false
        _workSearchQuery.value = ""
    }

    fun pauseWorkProfile(context: Context) {
        workProfileManager.setWorkProfilePaused(true) { success ->
            if (success) {
                refreshWorkProfileState()
                lockWorkProfile()
                Toast.makeText(context, "work profile paused", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "could not pause work profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun resumeWorkProfile(context: Context) {
        workProfileManager.setWorkProfilePaused(false) { success ->
            refreshWorkProfileState()
            if (success) {
                Toast.makeText(context, "work profile resumed", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "could not resume work profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchWorkApp(context: Context, packageName: String, appName: String) {
        if (!_isWorkProfileUnlocked.value) {
            Toast.makeText(context, "unlock work profile first", Toast.LENGTH_SHORT).show()
            return
        }
        if (_isWorkProfilePaused.value) {
            Toast.makeText(context, "work profile is paused — resume first", Toast.LENGTH_SHORT).show()
            return
        }
        val launched = workProfileManager.launchWorkApp(packageName)
        if (!launched) {
            Toast.makeText(context, "could not open $appName", Toast.LENGTH_SHORT).show()
        } else {
            lockWorkProfile()
            goHome()
        }
    }

    fun toggle24HourFormat() {
        val newValue = !_is24HourFormat.value
        _is24HourFormat.value = newValue
        prefs.edit().putBoolean("is_24h", newValue).apply()
        if (_isWallpaperMatchingEnabled.value) {
            appearanceController.ensureBlackWallpapers()
        }
    }

    fun setDailyGoalMinutes(minutes: Int) {
        _dailyGoalMinutes.value = minutes
        prefs.edit().putInt("daily_goal_minutes", minutes).apply()
    }

    fun toggleWallpaperMatching(context: Context) {
        val newValue = !_isWallpaperMatchingEnabled.value
        val result = appearanceController.setWallpaperMatching(newValue)
        _isWallpaperMatchingEnabled.value = newValue
        handleAppearanceResult(context, result)
    }

    fun toggleSystemDarkSchema(context: Context) {
        val newValue = !_isSystemDarkSchemaEnabled.value
        val result = appearanceController.setSystemDarkSchema(newValue)
        if (result is AppearanceResult.Success) {
            _isSystemDarkSchemaEnabled.value = newValue
            val message = if (newValue) {
                "Dark schema on — entire phone is desaturated + dark"
            } else {
                "Dark schema off — phone colors restored"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        handleAppearanceResult(context, result)
    }

    fun restoreAppearanceDefaults(context: Context) {
        val result = appearanceController.setSystemDarkSchema(false)
        if (result is AppearanceResult.Success) {
            _isSystemDarkSchemaEnabled.value = false
        }
        handleAppearanceResult(context, result)
        Toast.makeText(context, "Phone appearance restored", Toast.LENGTH_SHORT).show()
    }

    private fun handleAppearanceResult(context: Context, result: AppearanceResult) {
        when (result) {
            is AppearanceResult.Success -> {}
            is AppearanceResult.PermissionRequired -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                navigateTo(AppScreen.SystemSetup)
            }
            is AppearanceResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleWebsiteBlocking(context: Context) {
        val newValue = !_isWebsiteBlockingEnabled.value
        _isWebsiteBlockingEnabled.value = newValue
        prefs.edit().putBoolean("is_website_blocking", newValue).apply()
        if (newValue) {
            Toast.makeText(context, WEBSITE_BLOCKING_V2_NOTE, Toast.LENGTH_LONG).show()
        }
    }

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
        if (current.contains(day)) current.remove(day) else current.add(day)
        _scheduledDays.value = current
        prefs.edit().putStringSet("sched_days", current).apply()
    }

    fun assignShortcut(appName: String, packageName: String) {
        _shortcutPackageName.value = packageName
        _shortcutAppName.value = appName
        prefs.edit().putString("shortcut_pkg", packageName).putString("shortcut_name", appName).apply()
        goHome()
    }

    fun clearShortcut() {
        _shortcutPackageName.value = null
        _shortcutAppName.value = null
        prefs.edit().remove("shortcut_pkg").remove("shortcut_name").apply()
    }

    fun launchShortcut(context: Context) {
        val pkg = _shortcutPackageName.value ?: return navigateTo(AppScreen.ShortcutPicker)
        val name = _shortcutAppName.value ?: pkg
        launchAppDirect(context, pkg, name)
    }

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
        if (targetIndex in current.indices) {
            Collections.swap(current, index, targetIndex)
            _activeWidgets.value = current
            prefs.edit().putString("active_widgets", current.joinToString(",")).apply()
        }
    }

    fun pinApp(app: AppConfigEntity) {
        viewModelScope.launch {
            val maxOrder = favoriteApps.value.maxOfOrNull { it.pinnedOrder } ?: 0
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
            if (targetIndex in currentPinned.indices) {
                val other = currentPinned[targetIndex]
                repository.updateAppConfig(app.copy(pinnedOrder = other.pinnedOrder))
                repository.updateAppConfig(other.copy(pinnedOrder = app.pinnedOrder))
            }
        }
    }

    fun resetAppLabel(app: AppConfigEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val origName = AppLabelResolver.originalLabel(pm, app.packageName, app.appName)
            repository.updateAppConfig(app.copy(appName = origName))
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

    fun markNotificationRead(id: Int) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    fun tryLaunchApp(context: Context, packageName: String, appName: String) {
        viewModelScope.launch {
            val config = allAppConfigs.value.firstOrNull { it.packageName == packageName }
            val isScheduledBlocked = config?.isBlocked == true &&
                _isScheduledBlockingEnabled.value &&
                checkIfInsideSchedule()

            if (isScheduledBlocked) {
                repository.recordBlockedAttempt(packageName, appName, todayDateString())
                val untilString = formatBlockedUntilTime()
                navigateTo(AppScreen.BlockedScreen(appName, untilString))
            } else if (config != null && config.dailyLimitMinutes > 0) {
                logLauncherEvent(LauncherEventType.MINDFUL_LAUNCH, packageName)
                navigateTo(AppScreen.MindfulLaunch(packageName, appName, config.dailyLimitMinutes))
            } else {
                launchAppDirect(context, packageName, appName)
            }
        }
    }

    fun startMindfulSession(context: Context, packageName: String, appName: String, durationMinutes: Int) {
        viewModelScope.launch {
            SessionMonitorService.startSession(context, packageName, appName, durationMinutes)
            launchAppDirect(context, packageName, appName)
        }
    }

    fun launchAppDirect(context: Context, packageName: String, appName: String) {
        viewModelScope.launch {
            repository.recordAppLaunch(packageName, appName, todayDateString())
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Could not open $appName", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "App not installed: $appName", Toast.LENGTH_SHORT).show()
            }
            goHome()
        }
    }

    fun lockScreen(context: Context): Boolean {
        return if (LauncherUtils.isDeviceAdminActive(context)) {
            val locked = LauncherUtils.lockScreen(context)
            if (locked) {
                logLauncherEvent(LauncherEventType.LOCK_DOUBLE_TAP)
                Toast.makeText(context, "phone locked", Toast.LENGTH_SHORT).show()
            }
            locked
        } else {
            Toast.makeText(
                context,
                "device admin required — enable in settings or use adb command",
                Toast.LENGTH_LONG
            ).show()
            LauncherUtils.requestDeviceAdmin(context)
            false
        }
    }

    fun applyLockWallpaperNow(context: Context) {
        val result = appearanceController.ensureBlackWallpapers()
        _isWallpaperMatchingEnabled.value = true
        if (result is AppearanceResult.Success) {
            Toast.makeText(context, "black lock screen applied — pixel clock only", Toast.LENGTH_SHORT).show()
        } else {
            handleAppearanceResult(context, result)
        }
    }

    fun openLockScreenSettings(context: Context) {
        appearanceController.openLockScreenSettings()
        Toast.makeText(
            context,
            "lock screen is plain black — only pixel's clock is shown",
            Toast.LENGTH_LONG
        ).show()
    }

    fun openUsageAccessSettings(context: Context) {
        usageStatsRepository.openUsageAccessSettings()
    }

    fun opacityForApp(packageName: String, maxOpenCount: Int): Float {
        val usage = usageReport.value.firstOrNull { it.packageName == packageName }
        return usageStatsRepository.opacityForOpenCount(usage?.openCount ?: 0, maxOpenCount)
    }

    private fun formatBlockedUntilTime(): String {
        val hour = _scheduledEndHour.value
        val minute = _scheduledEndMinute.value
        return if (_is24HourFormat.value) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val amPm = if (hour < 12) "AM" else "PM"
            val h12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            String.format("%d:%02d %s", h12, minute, amPm)
        }
    }

    private fun checkIfInsideSchedule(): Boolean {
        val cal = Calendar.getInstance()
        val daysFormat = SimpleDateFormat("EEE", Locale.US)
        if (!_scheduledDays.value.contains(daysFormat.format(cal.time))) return false
        val currentTotal = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startTotal = _scheduledStartHour.value * 60 + _scheduledStartMinute.value
        val endTotal = _scheduledEndHour.value * 60 + _scheduledEndMinute.value
        return if (startTotal <= endTotal) {
            currentTotal in startTotal..endTotal
        } else {
            currentTotal >= startTotal || currentTotal <= endTotal
        }
    }

    fun startFocusSession(minutes: Int) {
        _focusTimerSeconds.value = minutes * 60
        _isFocusActive.value = true
        logLauncherEvent(LauncherEventType.FOCUS_START, minutes.toString())
        navigator.setScreen(AppScreen.Home)
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
        goHome()
    }

    private suspend fun completeFocusSession(minutes: Int) {
        _isFocusActive.value = false
        repository.insertFocusSession(
            FocusSessionEntity(date = todayDateString(), durationMinutes = minutes, isCompleted = true)
        )
        _focusTimerSeconds.value = 0
        goHome()
    }

    private suspend fun bootstrapApps(context: Context) {
        val existingConfigs = db.launcherDao().getAllAppConfigs().first()
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launchables = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val configs = launchables.mapNotNull { info ->
            val pName = info.activityInfo?.packageName ?: return@mapNotNull null
            val label = info.loadLabel(pm)?.toString() ?: ""
            val existing = existingConfigs.find { it.packageName == pName }
            AppConfigEntity(
                packageName = pName,
                appName = existing?.appName ?: label,
                isFavorite = existing?.isFavorite ?: false,
                isBlocked = existing?.isBlocked ?: false,
                dailyLimitMinutes = existing?.dailyLimitMinutes ?: 0,
                isSystemApp = (info.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                pinnedOrder = existing?.pinnedOrder ?: 0
            )
        }.distinctBy { it.packageName }

        repository.insertAppConfigs(configs)
    }

    private fun calculateRenamedApps(configs: List<AppConfigEntity>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val renamed = configs.filter { appEntity ->
                AppLabelResolver.isRenamed(pm, appEntity.packageName, appEntity.appName)
            }
            _renamedApps.value = renamed
        }
    }
}
