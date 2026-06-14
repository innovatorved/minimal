package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LauncherViewModel
import com.example.ui.theme.launcherBackground
import com.example.ui.theme.launcherDialogContainer
import com.example.ui.theme.launcherMuted
import com.example.ui.theme.launcherOnBackground
import com.example.ui.theme.launcherSecondary

@Composable
fun MindfulLaunchScreen(
    viewModel: LauncherViewModel,
    packageName: String,
    appName: String,
    limitMinutes: Int
) {
    val context = LocalContext.current
    var selectedTimeLimit by remember { mutableStateOf(limitMinutes.coerceAtLeast(5)) }

    Column(
        modifier = Modifier.fillMaxSize().background(launcherBackground()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "open ${appName.lowercase()} for how long?",
            color = launcherOnBackground(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 32.dp)) {
            listOf(5, 10, 15, 30).forEach { mins ->
                Text(
                    text = if (selectedTimeLimit == mins) "[$mins min]" else "$mins min",
                    color = launcherOnBackground(),
                    fontSize = 18.sp,
                    fontWeight = if (selectedTimeLimit == mins) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { selectedTimeLimit = mins }.padding(vertical = 8.dp)
                )
            }
        }
        Text("reminder after $selectedTimeLimit minutes", color = launcherSecondary(), fontSize = 14.sp, modifier = Modifier.padding(bottom = 48.dp))
        Text(
            text = "open app",
            color = launcherBackground(),
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(launcherOnBackground())
                .clickable { viewModel.startMindfulSession(context, packageName, appName, selectedTimeLimit) }
                .padding(vertical = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "cancel",
            color = launcherOnBackground(),
            fontSize = 16.sp,
            modifier = Modifier.clickable { viewModel.goHome() }.padding(vertical = 16.dp)
        )
    }
}

@Composable
fun BlockedScreen(viewModel: LauncherViewModel, appName: String, blockedUntil: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(launcherBackground()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${appName.lowercase()} is blocked until $blockedUntil",
            color = launcherOnBackground(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        Text(
            text = "back to home",
            color = launcherBackground(),
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth().background(launcherOnBackground()).clickable { viewModel.goHome() }.padding(vertical = 16.dp),
            textAlign = TextAlign.Center
        )
    }
}
