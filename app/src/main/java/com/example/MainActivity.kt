package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppConfigEntity
import com.example.ui.AppScreen
import com.example.ui.LauncherViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val viewModel: LauncherViewModel = viewModel()
                LauncherContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun LauncherContainer(viewModel: LauncherViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val context = LocalContext.current

    // Backpress handler
    BackHandler(enabled = currentScreen != AppScreen.Home) {
        viewModel.navigateBack()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            when (val screen = currentScreen) {
                is AppScreen.Home -> HomeScreen(viewModel)
                is AppScreen.AppDrawer -> AppDrawerScreen(viewModel)
                is AppScreen.Focus -> FocusScreen(viewModel)
                is AppScreen.Widgets -> WidgetsScreen(viewModel)
                is AppScreen.Settings -> SettingsScreen(viewModel)
                is AppScreen.MindfulLaunch -> MindfulLaunchScreen(
                    viewModel = viewModel,
                    packageName = screen.packageName,
                    appName = screen.appName,
                    limitMinutes = screen.limitMinutes
                )
                is AppScreen.BlockedScreen -> BlockedScreen(
                    viewModel = viewModel,
                    appName = screen.appName,
                    blockedUntil = screen.blockedUntil
                )
            }
        }
    }
}

// ==========================================
// SCREEN 1: HOME SCREEN
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val is24h by viewModel.is24HourFormat.collectAsState()
    val activeWidgets by viewModel.activeWidgets.collectAsState()

    // Focus Timer States
    val isFocusActive by viewModel.isFocusActive.collectAsState()
    val focusSecsLeft by viewModel.focusTimerSeconds.collectAsState()
    val focusSecsTotal by viewModel.focusTimerTotalSeconds.collectAsState()

    // General Screen Time stats
    val usageReport by viewModel.usageReport.collectAsState()
    val totalMinutes = usageReport.sumOf { it.durationMinutes }

    // Date/Time Clock states
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(is24h) {
        while (true) {
            val formatTime = if (is24h) "HH:mm" else "hh:mm a"
            val formatDate = "EEEE, MMMM dd"
            currentTime = SimpleDateFormat(formatTime, Locale.getDefault()).format(Date())
            currentDate = SimpleDateFormat(formatDate, Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    // Context dialog state for pinned apps
    var selectedAppForMenu by remember { mutableStateOf<AppConfigEntity?>(null) }
    var selectedAppForRename by remember { mutableStateOf<AppConfigEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    // Double tap detector to lock screen
    var lastTapTime by remember { mutableStateOf(0L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        Toast
                            .makeText(context, "Screen Locked (Simulated)", Toast.LENGTH_SHORT)
                            .show()
                    },
                    onTap = {
                        // Regular tap - can do nothing or clear menus
                    }
                )
            },
        horizontalAlignment = Alignment.Start
    ) {
        // Upper Third: Dynamic Widget stack (including Time + Date / Focus Ring)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Check if clock_date_ring or focus_ring is explicitly in activeWidgets list
            activeWidgets.forEach { widgetId ->
                when (widgetId) {
                    "clock", "focus ring" -> {
                        // Drawing centered large Clock Widget inside focus ring
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clickable {
                                    // Tapping the ring/time opens Screen 3 (Focus)
                                    viewModel.navigateTo(AppScreen.Focus)
                                }
                                .combinedClickable(
                                    onClick = { viewModel.navigateTo(AppScreen.Focus) },
                                    onLongClick = {
                                        // Long-press let's user assign a custom shortcut app
                                        viewModel.navigateTo(AppScreen.Settings)
                                        Toast
                                            .makeText(
                                                context,
                                                "Manage Custom Shortcuts in Settings",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Circular white progress stroke (~2dp)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Outer track
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.15f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                // Active sweep matching either focused session ticking or screen duration
                                val sweepProgress = when {
                                    isFocusActive -> {
                                        if (focusSecsTotal > 0) {
                                            focusSecsLeft.toFloat() / focusSecsTotal.toFloat()
                                        } else 1.0f
                                    }
                                    else -> {
                                        // screen time today vs standard 180 min goal
                                        (totalMinutes.toFloat() / 180f).coerceIn(0.01f, 1.0f)
                                    }
                                }
                                drawArc(
                                    color = Color.White,
                                    startAngle = -90f,
                                    sweepAngle = sweepProgress * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )
                            }

                            // Current time centered (large, bold)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentTime,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    textAlign = TextAlign.Center
                                )
                                if (isFocusActive) {
                                    val minutes = focusSecsLeft / 60
                                    val seconds = focusSecsLeft % 60
                                    Text(
                                        text = String.format("%02d:%02d LEFT", minutes, seconds),
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    "date" -> {
                        Text(
                            text = currentDate,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    "battery" -> {
                        Text(
                            text = "BATTERY STATUS: 82% CHARGED",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    "calendar (next event)" -> {
                        Text(
                            text = "NEXT EVENT: 2:00 PM DISCONNECT WORKSHOP",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }

        // Center / Lower half: Pinned Apps list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "PINNED",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (favoriteApps.isEmpty()) {
                Text(
                    text = "No apps pinned. Swipe up to add apps.",
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(favoriteApps.take(8)) { app ->
                        Text(
                            text = app.appName.lowercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        viewModel.tryLaunchApp(context, app.packageName, app.appName)
                                    },
                                    onLongClick = {
                                        selectedAppForMenu = app
                                    }
                                )
                                .padding(vertical = 14.dp)
                                .testTag("pinned_app_${app.packageName}")
                        )
                    }
                }
            }
        }

        // Bottom space: Swipe Up label / Link to Drawer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "all apps",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(AppScreen.AppDrawer) }
                    .padding(vertical = 16.dp)
                    .testTag("all_apps_link")
            )
        }
    }

    // Context Menu Dialog on Pinned list
    selectedAppForMenu?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForMenu = null },
            confirmButton = {
                Text(
                    text = "CLOSE",
                    color = Color.White,
                    modifier = Modifier
                        .clickable { selectedAppForMenu = null }
                        .padding(16.dp)
                )
            },
            title = {
                Text(
                    text = app.appName.uppercase(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "RENAME",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                renameInputText = app.appName
                                selectedAppForRename = app
                                selectedAppForMenu = null
                            }
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "MOVE UP",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.movePinnedApp(app, moveUp = true)
                                selectedAppForMenu = null
                            }
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "MOVE DOWN",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.movePinnedApp(app, moveUp = false)
                                selectedAppForMenu = null
                            }
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "REMOVE FROM HOME",
                        color = Color.LightGray,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.unpinApp(app)
                                selectedAppForMenu = null
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            },
            containerColor = Color.Black
        )
    }

    // Rename Dialog
    selectedAppForRename?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForRename = null },
            confirmButton = {
                Text(
                    text = "SAVE",
                    color = Color.White,
                    modifier = Modifier
                        .clickable {
                            if (renameInputText.trim().isNotEmpty()) {
                                viewModel.renameApp(app, renameInputText.trim())
                            }
                            selectedAppForRename = null
                        }
                        .padding(16.dp)
                )
            },
            dismissButton = {
                Text(
                    text = "CANCEL",
                    color = Color.LightGray,
                    modifier = Modifier
                        .clickable { selectedAppForRename = null }
                        .padding(16.dp)
                )
            },
            title = {
                Text(
                    text = "RENAME APP",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            text = {
                BasicTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(vertical = 12.dp)
                )
            },
            containerColor = Color.Black
        )
    }
}

// ==========================================
// SCREEN 2: APP DRAWER (ALL APPS)
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val allApps by viewModel.allAppConfigs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Determine the max open count for frequency opacity grading
    val usageReport by viewModel.usageReport.collectAsState()
    val maxOpenCount = usageReport.maxOfOrNull { it.openCount }?.coerceAtLeast(1) ?: 1

    val filteredApps = allApps.filter { app ->
        !app.isHidden && app.appName.contains(searchQuery, ignoreCase = true)
    }.sortedBy { it.appName.lowercase() }

    var selectedAppForDrawerMenu by remember { mutableStateOf<AppConfigEntity?>(null) }
    var selectedAppForRename by remember { mutableStateOf<AppConfigEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Transparent, borderless search field at top
        BasicTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal
            ),
            singleLine = true,
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("drawer_search_field"),
            decorationBox = { innerTextField ->
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "search",
                        color = Color.DarkGray,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal
                    )
                }
                innerTextField()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Large list of apps, vertical padding on each option, opacity gradient
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(filteredApps) { app ->
                // Opacity grading: frequently opened -> full white, rarely opened -> down to 40%
                val usageObj = usageReport.find { it.packageName == app.packageName }
                val count = usageObj?.openCount ?: 0
                val opacity = 0.40f + 0.60f * (count.toFloat() / maxOpenCount.toFloat()).coerceIn(0f, 1f)

                Text(
                    text = app.appName.lowercase(),
                    color = Color.White.copy(alpha = opacity),
                    fontSize = 19.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                viewModel.tryLaunchApp(context, app.packageName, app.appName)
                            },
                            onLongClick = {
                                selectedAppForDrawerMenu = app
                            }
                        )
                        .padding(vertical = 14.dp)
                        .testTag("drawer_app_${app.packageName}")
                )
            }
        }

        // Link to Screen 5: Settings at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "settings",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(AppScreen.Settings) }
                    .padding(vertical = 16.dp)
                    .testTag("drawer_settings_link")
            )
        }
    }

    // Context Menu Drawer Dialog
    selectedAppForDrawerMenu?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForDrawerMenu = null },
            confirmButton = {
                Text(
                    text = "CLOSE",
                    color = Color.White,
                    modifier = Modifier
                        .clickable { selectedAppForDrawerMenu = null }
                        .padding(16.dp)
                )
            },
            title = {
                Text(
                    text = app.appName.uppercase(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "RENAME",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                renameInputText = app.appName
                                selectedAppForRename = app
                                selectedAppForDrawerMenu = null
                            }
                            .padding(vertical = 8.dp)
                    )
                    val pinLabel = if (app.isFavorite) "UNPIN FROM HOME" else "PIN TO HOME"
                    Text(
                        text = pinLabel,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (app.isFavorite) {
                                    viewModel.unpinApp(app)
                                } else {
                                    viewModel.pinApp(app)
                                }
                                selectedAppForDrawerMenu = null
                            }
                            .padding(vertical = 8.dp)
                    )
                    val blockLabel = if (app.isBlocked) "UNBLOCK APP" else "BLOCK APP"
                    Text(
                        text = blockLabel,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.toggleBlocked(app)
                                selectedAppForDrawerMenu = null
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            },
            containerColor = Color.Black
        )
    }

    // Rename Dialog (Drawer)
    selectedAppForRename?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForRename = null },
            confirmButton = {
                Text(
                    text = "SAVE",
                    color = Color.White,
                    modifier = Modifier
                        .clickable {
                            if (renameInputText.trim().isNotEmpty()) {
                                viewModel.renameApp(app, renameInputText.trim())
                            }
                            selectedAppForRename = null
                        }
                        .padding(16.dp)
                )
            },
            dismissButton = {
                Text(
                    text = "CANCEL",
                    color = Color.LightGray,
                    modifier = Modifier
                        .clickable { selectedAppForRename = null }
                        .padding(16.dp)
                )
            },
            title = {
                Text(
                    text = "RENAME APP",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            text = {
                BasicTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(vertical = 12.dp)
                )
            },
            containerColor = Color.Black
        )
    }
}

// ==========================================
// SCREEN 3: FOCUS (APP BLOCKING & TIME LIMITS)
// ==========================================
@Composable
fun FocusScreen(viewModel: LauncherViewModel) {
    val allApps by viewModel.allAppConfigs.collectAsState()
    val usageReport by viewModel.usageReport.collectAsState()
    val isWebsiteBlocking by viewModel.isWebsiteBlockingEnabled.collectAsState()
    val isScheduledBlocking by viewModel.isScheduledBlockingEnabled.collectAsState()

    // Scheduled Blocking range
    val schedStartH by viewModel.scheduledStartHour.collectAsState()
    val schedStartM by viewModel.scheduledStartMinute.collectAsState()
    val schedEndH by viewModel.scheduledEndHour.collectAsState()
    val schedEndM by viewModel.scheduledEndMinute.collectAsState()
    val schedDays by viewModel.scheduledDays.collectAsState()

    // Calculating Screen Time Today
    val totalMinutesToday = usageReport.sumOf { it.durationMinutes }
    val hours = totalMinutesToday / 60
    val minutes = totalMinutesToday % 60

    // Filter apps that have a limit or are blocked
    val limitedOrBlockedApps = allApps.filter { it.isBlocked || it.dailyLimitMinutes > 0 }

    // Dialog trigger states
    var showAddAppSelector by remember { mutableStateOf(false) }
    var runningLimitConfigApp by remember { mutableStateOf<AppConfigEntity?>(null) }
    var manualLimitMinutes by remember { mutableStateOf("") }

    // Navigation and schedule edits
    var isEditingScheduleRange by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Header
        Text(
            text = "focus control",
            color = Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "today: ${hours}h ${minutes}m",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }

        // Section 1: Focus quick timer triggers
        Text(
            text = "START FOCUS SESSION",
            color = Color.Gray,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            listOf(15, 30, 45).forEach { mins ->
                Text(
                    text = "[${mins} mins]",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .clickable {
                            viewModel.startFocusSession(mins)
                        }
                        .padding(vertical = 12.dp)
                        .testTag("start_focus_${mins}")
                )
            }
        }

        // Section 2: Blocked/Limited list
        Text(
            text = "RESTRICTED APPS & LIMITS",
            color = Color.Gray,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(limitedOrBlockedApps) { app ->
                val displayLimit = if (app.isBlocked) {
                    "blocked"
                } else {
                    "${app.dailyLimitMinutes} min limit"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { runningLimitConfigApp = app }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.appName.lowercase(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = displayLimit,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            item {
                // Add app row at bottom
                Text(
                    text = "+ add limited/blocked app",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddAppSelector = true }
                        .padding(vertical = 14.dp)
                        .testTag("add_focus_app_row")
                )
            }
        }

        // Section 3: Scheduled blocking & website controls (plain text switch)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.toggleScheduledBlocking() }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "scheduled blocking",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = if (isScheduledBlocking) "[on]" else "[off]",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("sched_blocking_switch")
            )
        }

        if (isScheduledBlocking) {
            Text(
                text = "schedules: configured for ${schedStartH}h${String.format("%02d", schedStartM)} to ${schedEndH}h${String.format("%02d", schedEndM)} on ${schedDays.joinToString(", ")} (click to edit)",
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditingScheduleRange = true }
                    .padding(vertical = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.toggleWebsiteBlocking() }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "block websites in blocked apps",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = if (isWebsiteBlocking) "[on]" else "[off]",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold
            )
        }

        // Back link
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "< back",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .clickable { viewModel.navigateBack() }
                    .padding(vertical = 16.dp)
            )
        }
    }

    // Modal to add a new app to Focus limits
    if (showAddAppSelector) {
        val nonFocusApps = allApps.filter { !it.isBlocked && it.dailyLimitMinutes == 0 }
        AlertDialog(
            onDismissRequest = { showAddAppSelector = false },
            confirmButton = {},
            dismissButton = {
                Text(
                    text = "CLOSE",
                    color = Color.White,
                    modifier = Modifier
                        .clickable { showAddAppSelector = false }
                        .padding(16.dp)
                )
            },
            title = {
                Text(
                    text = "SELECT APP TO RESTRICT",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(nonFocusApps) { app ->
                        Text(
                            text = app.appName.lowercase(),
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleBlocked(app)
                                    showAddAppSelector = false
                                }
                                .padding(vertical = 14.dp)
                        )
                    }
                }
            },
            containerColor = Color.Black
        )
    }

    // Modal to configure limit/blocking of screen time
    runningLimitConfigApp?.let { app ->
        AlertDialog(
            onDismissRequest = { runningLimitConfigApp = null },
            confirmButton = {
                Text(
                    text = "SAVE",
                    color = Color.White,
                    modifier = Modifier
                        .clickable {
                            val limit = manualLimitMinutes.toIntOrNull() ?: 0
                            if (limit > 0) {
                                viewModel.updateDailyLimit(app, limit)
                                // If was fully blocked, unblock explicitly to let limit handle it
                                if (app.isBlocked) viewModel.toggleBlocked(app)
                            } else {
                                viewModel.updateDailyLimit(app, 0)
                            }
                            runningLimitConfigApp = null
                            manualLimitMinutes = ""
                        }
                        .padding(16.dp)
                )
            },
            dismissButton = {
                Text(
                    text = "REMOVE RESTRICTION",
                    color = Color.LightGray,
                    modifier = Modifier
                        .clickable {
                            viewModel.updateDailyLimit(app, 0)
                            if (app.isBlocked) viewModel.toggleBlocked(app)
                            runningLimitConfigApp = null
                            manualLimitMinutes = ""
                        }
                        .padding(16.dp)
                )
            },
            title = {
                Text(
                    text = "MANAGE RESTRICTIONS: ${app.appName.uppercase()}",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Toggling blocked status keeps this app fully locked inside blocking schedules. Setting a daily time limit prompts a mindful launch timer instead.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.toggleBlocked(app)
                                runningLimitConfigApp = null
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("fully blocked in schedules", color = Color.White)
                        Text(
                            text = if (app.isBlocked) "[on]" else "[off]",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "daily time limit (minutes):",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    BasicTextField(
                        value = manualLimitMinutes,
                        onValueChange = { manualLimitMinutes = it.filter { char -> char.isDigit() } },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(vertical = 8.dp),
                        decorationBox = { innerTextField ->
                            if (manualLimitMinutes.isEmpty()) {
                                Text(
                                    text = if (app.dailyLimitMinutes > 0) app.dailyLimitMinutes.toString() else "0 for none",
                                    color = Color.DarkGray,
                                    fontSize = 18.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            },
            containerColor = Color.Black
        )
    }

    // Edit Schedule Range dialog
    if (isEditingScheduleRange) {
        var startHText by remember { mutableStateOf(schedStartH.toString()) }
        var startMText by remember { mutableStateOf(schedStartM.toString()) }
        var endHText by remember { mutableStateOf(schedEndH.toString()) }
        var endMText by remember { mutableStateOf(schedEndM.toString()) }

        AlertDialog(
            onDismissRequest = { isEditingScheduleRange = false },
            confirmButton = {
                Text(
                    text = "SAVE",
                    color = Color.White,
                    modifier = Modifier
                        .clickable {
                            val sh = startHText.toIntOrNull() ?: 22
                            val sm = startMText.toIntOrNull() ?: 0
                            val eh = endHText.toIntOrNull() ?: 6
                            val em = endMText.toIntOrNull() ?: 0
                            viewModel.saveScheduleTime(sh, sm, eh, em)
                            isEditingScheduleRange = false
                        }
                        .padding(16.dp)
                )
            },
            title = {
                Text(
                    text = "EDIT BLOCKING RANGE",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("START TIME (HOUR / MINUTE):", color = Color.Gray, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BasicTextField(
                            value = startHText,
                            onValueChange = { startHText = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black)
                                .padding(8.dp)
                        )
                        BasicTextField(
                            value = startMText,
                            onValueChange = { startMText = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black)
                                .padding(8.dp)
                        )
                    }

                    Text("END TIME (HOUR / MINUTE):", color = Color.Gray, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BasicTextField(
                            value = endHText,
                            onValueChange = { endHText = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black)
                                .padding(8.dp)
                        )
                        BasicTextField(
                            value = endMText,
                            onValueChange = { endMText = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black)
                                .padding(8.dp)
                        )
                    }

                    Text("TRIGGER DAYS:", color = Color.Gray, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                            val isActive = schedDays.contains(day)
                            Text(
                                text = day.take(1),
                                color = if (isActive) Color.White else Color.DarkGray,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .clickable { viewModel.toggleScheduledDay(day) }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            },
            containerColor = Color.Black
        )
    }
}

// ==========================================
// SCREEN 4: WIDGETS CONFIGURATION SCREEN
// ==========================================
@Composable
fun WidgetsScreen(viewModel: LauncherViewModel) {
    val activeWidgets by viewModel.activeWidgets.collectAsState()
    val availableWidgets = listOf(
        Pair("clock", "centered simple display, standard representation"),
        Pair("date", "current day, week name in elegant display"),
        Pair("battery", "minimal text battery summary on screen state"),
        Pair("focus ring", "clockwise filled circle outline surrounding clock"),
        Pair("calendar (next event)", "displays the next upcoming work or rest timeline item")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "widgets config",
            color = Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "manage widgets on home screen",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Flat listing of widgets
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(availableWidgets) { (widgetId, description) ->
                val isPlaced = activeWidgets.contains(widgetId)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = widgetId.lowercase(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPlaced) "[remove]" else "[add]",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    if (isPlaced) {
                                        viewModel.removeWidget(widgetId)
                                    } else {
                                        viewModel.addWidget(widgetId)
                                    }
                                }
                                .padding(vertical = 6.dp)
                                .testTag("widget_toggle_${widgetId}")
                        )
                    }
                    Text(
                        text = description,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (isPlaced) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = ":: move up",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { viewModel.moveWidget(widgetId, moveUp = true) }
                                    .padding(vertical = 4.dp)
                            )
                            Text(
                                text = ":: move down",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { viewModel.moveWidget(widgetId, moveUp = false) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Back link
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "< back to settings",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .clickable { viewModel.navigateBack() }
                    .padding(vertical = 16.dp)
            )
        }
    }
}

// ==========================================
// SCREEN 5: SETTINGS
// ==========================================
@Composable
fun SettingsScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val is24h by viewModel.is24HourFormat.collectAsState()
    val isWallpaperMatching by viewModel.isWallpaperMatchingEnabled.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val allApps by viewModel.allAppConfigs.collectAsState()

    val renamedApps by viewModel.renamedApps.collectAsState()

    var selectedAppForShortcutAssign by remember { mutableStateOf(false) }
    val shortcutAppName by viewModel.shortcutAppName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "settings",
            color = Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Section: DISPLAY
            item {
                Text(
                    text = "DISPLAY",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggle24HourFormat() }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("time format .......... 24h", color = Color.White, fontSize = 16.sp)
                    Text(
                        text = if (is24h) "[on]" else "[off]",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleWallpaperMatching(context) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("matching pure dark wallpaper", color = Color.White, fontSize = 16.sp)
                    Text(
                        text = if (isWallpaperMatching) "[on]" else "[off]",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Section: APPS
            item {
                Text(
                    text = "APPS",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )

                Text(
                    text = "widgets configuration",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(AppScreen.Widgets) }
                        .padding(vertical = 14.dp)
                        .testTag("widgets_config_link")
                )

                if (renamedApps.isNotEmpty()) {
                    Text(
                        text = "RENAMED APPS (CLICK TO RESET):",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                    renamedApps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.resetAppLabel(app)
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(app.appName.lowercase(), color = Color.White)
                            Text("[reset]", color = Color.LightGray)
                        }
                    }
                }
            }

            // Section: SHORTCUTS
            item {
                Text(
                    text = "SHORTCUTS",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAppForShortcutAssign = true }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("time area longpress shortcut", color = Color.White, fontSize = 16.sp)
                    Text(
                        text = shortcutAppName?.lowercase() ?: "[not assigned]",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (shortcutAppName != null) {
                    Text(
                        text = "remove active shortcut",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.clearShortcut() }
                            .padding(vertical = 10.dp)
                    )
                }
            }

            // Section: ABOUT
            item {
                Text(
                    text = "ABOUT",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )

                Text(
                    text = "version .......... 1.0.0",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                Text(
                    text = "privacy policy & support link",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable {
                            Toast
                                .makeText(
                                    context,
                                    "Redirect to: privacy.html (Simulated)",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                        .padding(vertical = 10.dp)
                )
            }
        }

        // Back link
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "< back to home",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(AppScreen.Home) }
                    .padding(vertical = 16.dp)
            )
        }
    }

    // Modal to Assign Shortcut App
    if (selectedAppForShortcutAssign) {
        AlertDialog(
            onDismissRequest = { selectedAppForShortcutAssign = false },
            confirmButton = {},
            dismissButton = {
                Text(
                    text = "CLOSE",
                    color = Color.White,
                    modifier = Modifier
                        .clickable { selectedAppForShortcutAssign = false }
                        .padding(16.dp)
                )
            },
            title = {
                Text(
                    text = "CHOOSE SHORTCUT APP",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(allApps) { app ->
                        Text(
                            text = app.appName.lowercase(),
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.assignShortcut(app.appName, app.packageName)
                                    selectedAppForShortcutAssign = false
                                }
                                .padding(vertical = 14.dp)
                        )
                    }
                }
            },
            containerColor = Color.Black
        )
    }
}

// ==========================================
// MINDFUL LAUNCH DELAY INTERSTITIAL
// ==========================================
@Composable
fun MindfulLaunchScreen(
    viewModel: LauncherViewModel,
    packageName: String,
    appName: String,
    limitMinutes: Int
) {
    val context = LocalContext.current
    var selectedTimeLimit by remember { mutableStateOf(5) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "open ${appName.lowercase()} for how long?",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Select Duration triggers
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            listOf(5, 10, 15, 30).forEach { mins ->
                val isSelected = selectedTimeLimit == mins
                Text(
                    text = if (isSelected) "[$mins min]" else " $mins min ",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { selectedTimeLimit = mins }
                        .padding(vertical = 8.dp)
                )
            }
        }

        Text(
            text = "Reminders will trigger after $selectedTimeLimit minutes.",
            color = Color.LightGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    viewModel.launchAppDirect(context, packageName, appName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("delay_confirm_open")
            ) {
                Text("open app", color = Color.Black, fontSize = 16.sp)
            }

            Button(
                onClick = {
                    viewModel.navigateTo(AppScreen.Home, clearHistory = true)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, Color.White),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("cancel & remain mindful", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

// ==========================================
// FULLY BLOCKED INTERSTITIAL (PLAIN BLACK)
// ==========================================
@Composable
fun BlockedScreen(
    viewModel: LauncherViewModel,
    appName: String,
    blockedUntil: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${appName.lowercase()} is blocked until $blockedUntil",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Disconnect fully to focus on what matters.",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Button(
            onClick = {
                viewModel.navigateTo(AppScreen.Home, clearHistory = true)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("blocked_back_home")
        ) {
            Text("back to home", color = Color.Black, fontSize = 16.sp)
        }
    }
}
