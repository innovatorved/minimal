package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LauncherViewModel
import com.example.ui.components.LauncherBackBar
import com.example.ui.theme.launcherBackground
import com.example.ui.theme.launcherDialogContainer
import com.example.ui.theme.launcherMuted
import com.example.ui.theme.launcherOnBackground
import com.example.ui.theme.launcherSecondary

private val widgetDescriptions = mapOf(
    "clock" to "centered time display",
    "date" to "current day and date",
    "display name" to "custom label — edit text in settings → display",
    "battery" to "battery level text",
    "focus ring" to "progress ring around clock",
    "calendar (next event)" to "next upcoming calendar event"
)

@Composable
fun WidgetsScreen(viewModel: LauncherViewModel) {
    val activeWidgets by viewModel.activeWidgets.collectAsState()
    val inactiveWidgets = widgetDescriptions.keys.filter { it !in activeWidgets }

    Column(
        modifier = Modifier.fillMaxSize().background(launcherBackground()).padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text("widgets", color = launcherSecondary(), fontSize = 11.sp, letterSpacing = 2.sp)
        Text(
            "manage widgets on home screen",
            color = launcherOnBackground(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Text(
            "on by default — remove or reorder below",
            color = launcherMuted(),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (activeWidgets.isNotEmpty()) {
                item {
                    Text("on home screen", color = launcherSecondary(), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                items(activeWidgets, key = { it }) { widgetId ->
                    ActiveWidgetRow(
                        widgetId = widgetId,
                        description = widgetDescriptions[widgetId].orEmpty(),
                        canMoveUp = activeWidgets.indexOf(widgetId) > 0,
                        canMoveDown = activeWidgets.indexOf(widgetId) < activeWidgets.lastIndex,
                        onMoveUp = { viewModel.moveWidget(widgetId, moveUp = true) },
                        onMoveDown = { viewModel.moveWidget(widgetId, moveUp = false) },
                        onRemove = { viewModel.removeWidget(widgetId) }
                    )
                }
            } else {
                item {
                    Text(
                        "no widgets on home — add one below",
                        color = launcherMuted(),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            if (inactiveWidgets.isNotEmpty()) {
                item {
                    Text(
                        "add widget",
                        color = launcherSecondary(),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(inactiveWidgets, key = { it }) { widgetId ->
                    InactiveWidgetRow(
                        widgetId = widgetId,
                        description = widgetDescriptions[widgetId].orEmpty(),
                        onAdd = { viewModel.addWidget(widgetId) }
                    )
                }
            }
        }
        LauncherBackBar(onBack = { viewModel.goHome() })
    }
}

@Composable
private fun ActiveWidgetRow(
    widgetId: String,
    description: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(widgetId, color = launcherOnBackground(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("[remove]", color = launcherSecondary(), modifier = Modifier.clickable(onClick = onRemove))
        }
        Text(description, color = launcherSecondary(), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
            Text(
                text = "move up",
                color = if (canMoveUp) launcherOnBackground() else launcherMuted(),
                fontSize = 12.sp,
                modifier = Modifier.clickable(enabled = canMoveUp, onClick = onMoveUp)
            )
            Text(
                text = "move down",
                color = if (canMoveDown) launcherOnBackground() else launcherMuted(),
                fontSize = 12.sp,
                modifier = Modifier.clickable(enabled = canMoveDown, onClick = onMoveDown)
            )
        }
    }
}

@Composable
private fun InactiveWidgetRow(
    widgetId: String,
    description: String,
    onAdd: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(widgetId, color = launcherOnBackground(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("[add]", color = launcherOnBackground(), modifier = Modifier.clickable(onClick = onAdd))
        }
        Text(description, color = launcherSecondary(), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
