package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfigEntity
import com.example.ui.theme.launcherDialogContainer
import com.example.ui.theme.launcherOnBackground
import com.example.ui.theme.launcherSecondary

@Composable
fun AppPickerDialog(
    title: String,
    apps: List<AppConfigEntity>,
    onSelect: (AppConfigEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = launcherDialogContainer(),
        title = { Text(title, color = launcherOnBackground()) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    Text(
                        text = app.appName.lowercase(),
                        color = launcherOnBackground(),
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(app) }
                            .padding(vertical = 10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Text(
                text = "close",
                color = launcherSecondary(),
                modifier = Modifier.clickable(onClick = onDismiss).padding(16.dp)
            )
        }
    )
}
