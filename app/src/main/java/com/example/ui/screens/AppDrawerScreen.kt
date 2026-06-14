package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.LauncherThemes
import com.example.ui.theme.MinimalFontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfigEntity
import com.example.ui.LauncherViewModel
import com.example.ui.navigation.AppScreen
import com.example.usage.UsageStatsRepository

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val themeColorId by viewModel.themeColorId.collectAsState()
    val allApps by viewModel.allAppConfigs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val usageReport by viewModel.usageReport.collectAsState()
    val maxOpenCount = usageReport.maxOfOrNull { it.openCount }?.coerceAtLeast(1) ?: 1
    val hasWorkProfile by viewModel.hasWorkProfile.collectAsState()

    var selectedAppForMenu by remember { mutableStateOf<AppConfigEntity?>(null) }
    var selectedAppForRename by remember { mutableStateOf<AppConfigEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    val filteredApps = allApps
        .filter { !UsageStatsRepository.isExcludedFromScreenTime(it.packageName) }
        .filter { it.appName.contains(searchQuery, ignoreCase = true) }

    val activeApps = filteredApps
        .filter { !it.isHidden }
        .sortedBy { it.appName.lowercase() }
    val archivedApps = filteredApps
        .filter { it.isHidden }
        .sortedBy { it.appName.lowercase() }

    key(themeColorId) {
        val themeColors = LauncherThemes.presetForId(themeColorId).colors
        val onBackground = themeColors.onBackground
        val muted = themeColors.muted
        val secondary = themeColors.secondary

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(themeColors.background)
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            Text(
                text = "scroll for apps",
                color = muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BasicTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                textStyle = TextStyle(color = onBackground, fontSize = 18.sp, fontFamily = MinimalFontFamily),
                singleLine = true,
                cursorBrush = SolidColor(onBackground),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("drawer_search_field"),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) {
                        Text("search", color = muted, fontSize = 18.sp)
                    }
                    inner()
                }
            )

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(activeApps, key = { "${it.packageName}_$themeColorId" }) { app ->
                    val opacity = viewModel.opacityForApp(app.packageName, maxOpenCount)
                    Text(
                        text = app.appName.lowercase(),
                        color = themeColors.onBackground.copy(alpha = opacity),
                        fontSize = 18.sp,
                        fontFamily = MinimalFontFamily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { viewModel.tryLaunchApp(context, app.packageName, app.appName) },
                                onLongClick = { selectedAppForMenu = app }
                            )
                            .padding(vertical = 16.dp)
                    )
                }

                if (archivedApps.isNotEmpty()) {
                    item {
                        Text(
                            text = "archived",
                            color = muted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(archivedApps, key = { "archived_${it.packageName}_$themeColorId" }) { app ->
                        val opacity = viewModel.opacityForApp(app.packageName, maxOpenCount) * 0.3f
                        Text(
                            text = "${app.appName.lowercase()} · archived",
                            color = muted.copy(alpha = opacity.coerceIn(0.25f, 0.45f)),
                            fontSize = 18.sp,
                            fontFamily = MinimalFontFamily,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { viewModel.tryLaunchApp(context, app.packageName, app.appName) },
                                    onLongClick = { selectedAppForMenu = app }
                                )
                                .padding(vertical = 16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hasWorkProfile) {
                    Text(
                        text = "work profile",
                        color = secondary,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(AppScreen.WorkProfile) }
                            .padding(vertical = 12.dp)
                    )
                }
                Text(
                    text = "settings",
                    color = secondary,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.Settings) }.padding(vertical = 12.dp)
                )
                Text(
                    text = "notifications",
                    color = secondary,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.Notifications) }.padding(vertical = 12.dp)
                )
                Text(
                    text = "< back to home",
                    color = secondary,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { viewModel.goHome() }.padding(vertical = 12.dp)
                )
            }
        }

        selectedAppForMenu?.let { app ->
            val menuActions = buildList {
                add("rename")
                if (!app.isHidden) {
                    add("add to home")
                    add("block")
                    add("archive")
                } else {
                    add("unarchive")
                }
            }
            AlertDialog(
                onDismissRequest = { selectedAppForMenu = null },
                containerColor = themeColors.dialogContainer,
                title = { Text(app.appName.lowercase(), color = onBackground) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        menuActions.forEach { action ->
                            Text(
                                text = action,
                                color = onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (action) {
                                            "rename" -> { renameInputText = app.appName; selectedAppForRename = app }
                                            "add to home" -> viewModel.pinApp(app)
                                            "block" -> viewModel.toggleBlocked(app)
                                            "archive" -> viewModel.archiveApp(app)
                                            "unarchive" -> viewModel.unarchiveApp(app)
                                        }
                                        selectedAppForMenu = null
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Text("close", color = onBackground, modifier = Modifier.clickable { selectedAppForMenu = null }.padding(16.dp))
                }
            )
        }

        selectedAppForRename?.let { app ->
            AlertDialog(
                onDismissRequest = { selectedAppForRename = null },
                containerColor = themeColors.dialogContainer,
                title = { Text("rename app", color = onBackground) },
                text = {
                    BasicTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        textStyle = TextStyle(color = onBackground, fontSize = 18.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(onBackground),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Text("save", color = onBackground, modifier = Modifier.clickable {
                        if (renameInputText.trim().isNotEmpty()) viewModel.renameApp(app, renameInputText.trim())
                        selectedAppForRename = null
                    }.padding(16.dp))
                },
                dismissButton = {
                    Text("cancel", color = secondary, modifier = Modifier.clickable { selectedAppForRename = null }.padding(16.dp))
                }
            )
        }
    }
}
