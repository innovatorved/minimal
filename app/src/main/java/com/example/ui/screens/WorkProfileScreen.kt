package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.MinimalFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.LauncherViewModel
import com.example.ui.components.LauncherBackBar
import com.example.work.WorkProfileAuth

@Composable
fun WorkProfileScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val hasWorkProfile by viewModel.hasWorkProfile.collectAsState()
    val isPaused by viewModel.isWorkProfilePaused.collectAsState()
    val isUnlocked by viewModel.isWorkProfileUnlocked.collectAsState()
    val workApps by viewModel.workApps.collectAsState()
    val workSearch by viewModel.workSearchQuery.collectAsState()

    val filteredApps = workApps.filter {
        it.appName.contains(workSearch, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "work profile",
            color = Color.LightGray,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (!hasWorkProfile) {
            Text(
                text = "no work profile found on this device",
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            if (isPaused) {
                Text(
                    text = "work profile is paused",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "work apps are hidden until you resume",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Text(
                    text = "resume work profile",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { viewModel.resumeWorkProfile(context) }
                        .padding(vertical = 14.dp)
                )
            } else if (!isUnlocked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "work apps are locked",
                        color = Color.White,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "tap below to unlock with fingerprint or password",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                    Text(
                        text = "unlock work profile",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = MinimalFontFamily,
                        modifier = Modifier
                            .clickable {
                                val activity = context as? FragmentActivity
                                if (activity == null) {
                                    Toast.makeText(context, "cannot authenticate", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                WorkProfileAuth.authenticate(
                                    activity = activity,
                                    onSuccess = { viewModel.unlockWorkProfile() },
                                    onFailure = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            .padding(vertical = 18.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("unlocked", color = Color.LightGray, fontSize = 14.sp)
                    Text(
                        text = "lock",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { viewModel.lockWorkProfile() }
                    )
                }

                Text(
                    text = "pause work profile",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { viewModel.pauseWorkProfile(context) }
                        .padding(vertical = 14.dp)
                )
                Text(
                    text = "pauses all work apps (pixel work profile pause)",
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                BasicTextField(
                    value = workSearch,
                    onValueChange = { viewModel.updateWorkSearchQuery(it) },
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontFamily = MinimalFontFamily),
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    decorationBox = { inner ->
                        if (workSearch.isEmpty()) {
                            Text("search work apps", color = Color.DarkGray, fontSize = 18.sp)
                        }
                        inner()
                    }
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredApps) { app ->
                        Text(
                            text = app.appName.lowercase(),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = MinimalFontFamily,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.launchWorkApp(context, app.packageName, app.appName)
                                }
                                .padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }

        LauncherBackBar(onBack = {
            viewModel.lockWorkProfile()
            viewModel.goHome()
        })
    }
}
