package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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
    val allApps by viewModel.allAppConfigs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val usageReport by viewModel.usageReport.collectAsState()
    val maxOpenCount = usageReport.maxOfOrNull { it.openCount }?.coerceAtLeast(1) ?: 1
    val listState = rememberLazyListState()

    val filteredApps = allApps
        .filter { !UsageStatsRepository.isExcludedFromScreenTime(it.packageName) }
        .filter { it.appName.contains(searchQuery, ignoreCase = true) }
        .sortedBy { it.appName.lowercase() }
    val hasWorkProfile by viewModel.hasWorkProfile.collectAsState()

    var selectedAppForMenu by remember { mutableStateOf<AppConfigEntity?>(null) }
    var selectedAppForRename by remember { mutableStateOf<AppConfigEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        Text(
            text = "scroll for apps",
            color = Color.DarkGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        BasicTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontFamily = MinimalFontFamily),
            singleLine = true,
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("drawer_search_field"),
            decorationBox = { inner ->
                if (searchQuery.isEmpty()) {
                    Text("search", color = Color.DarkGray, fontSize = 18.sp)
                }
                inner()
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                val opacity = viewModel.opacityForApp(app.packageName, maxOpenCount)
                Text(
                    text = app.appName.lowercase(),
                    color = Color.White.copy(alpha = opacity),
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

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasWorkProfile) {
                Text(
                    text = "work profile",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { viewModel.navigateTo(AppScreen.WorkProfile) }
                        .padding(vertical = 12.dp)
                )
            }
            Text(
                text = "settings",
                color = Color.LightGray,
                fontSize = 14.sp,
                modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.Settings) }.padding(vertical = 12.dp)
            )
            Text(
                text = "notifications",
                color = Color.LightGray,
                fontSize = 14.sp,
                modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.Notifications) }.padding(vertical = 12.dp)
            )
            Text(
                text = "< back to home",
                color = Color.LightGray,
                fontSize = 14.sp,
                modifier = Modifier.clickable { viewModel.goHome() }.padding(vertical = 12.dp)
            )
        }
    }

    selectedAppForMenu?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForMenu = null },
            containerColor = Color.Black,
            title = { Text(app.appName.lowercase(), color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("rename", "add to home", "block").forEach { action ->
                        Text(
                            text = action,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (action) {
                                        "rename" -> { renameInputText = app.appName; selectedAppForRename = app }
                                        "add to home" -> viewModel.pinApp(app)
                                        "block" -> viewModel.toggleBlocked(app)
                                    }
                                    selectedAppForMenu = null
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Text("close", color = Color.White, modifier = Modifier.clickable { selectedAppForMenu = null }.padding(16.dp))
            }
        )
    }

    selectedAppForRename?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForRename = null },
            containerColor = Color.Black,
            title = { Text("rename app", color = Color.White) },
            text = {
                BasicTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Text("save", color = Color.White, modifier = Modifier.clickable {
                    if (renameInputText.trim().isNotEmpty()) viewModel.renameApp(app, renameInputText.trim())
                    selectedAppForRename = null
                }.padding(16.dp))
            },
            dismissButton = {
                Text("cancel", color = Color.LightGray, modifier = Modifier.clickable { selectedAppForRename = null }.padding(16.dp))
            }
        )
    }
}
