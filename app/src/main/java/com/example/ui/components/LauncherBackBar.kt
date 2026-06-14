package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.theme.launcherSecondary
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LauncherBackBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "< back"
) {
    Text(
        text = label,
        color = launcherSecondary(),
        fontSize = 14.sp,
        modifier = modifier
            .clickable(onClick = onBack)
            .padding(vertical = 16.dp)
    )
}
