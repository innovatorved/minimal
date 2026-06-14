package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LauncherThemes
import com.example.ui.theme.MinimalFontFamily
import com.example.ui.theme.launcherOnBackground
import com.example.ui.theme.launcherSecondary

@Composable
fun HomeQuickShortcuts(
    onGoogleClick: () -> Unit,
    onPaymentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = launcherOnBackground()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_google_g),
            contentDescription = "open google",
            tint = iconColor,
            modifier = Modifier
                .clickable(onClick = onGoogleClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .size(22.dp)
        )
        Text(
            text = "₹",
            color = iconColor,
            fontSize = 22.sp,
            fontFamily = MinimalFontFamily,
            modifier = Modifier
                .clickable(onClick = onPaymentClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ThemeColorPickerGrouped(
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = LauncherThemes.presets
    val resolvedSelectedId = LauncherThemes.presetForId(selectedThemeId).id
    val onBackground = launcherOnBackground()
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            val isSelected = preset.id == resolvedSelectedId
            val checkColor = if (preset.swatch.luminance() > 0.55f) Color(0xFF1A1C1E) else Color.White
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(preset.swatch, shape = CircleShape)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 2.dp,
                                color = onBackground,
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onThemeSelected(preset.id) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text("✓", color = checkColor, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SelectedThemeLabel(selectedThemeId: String, modifier: Modifier = Modifier) {
    val label = LauncherThemes.presetForId(selectedThemeId).label
    Text(
        text = label,
        color = launcherSecondary(),
        fontSize = 12.sp,
        modifier = modifier.padding(top = 2.dp, bottom = 4.dp)
    )
}
