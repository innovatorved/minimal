package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun MyApplicationTheme(
    themeId: String = LauncherThemes.defaultId,
    content: @Composable () -> Unit
) {
    val preset = LauncherThemes.presetForId(themeId)
    CompositionLocalProvider(LocalLauncherColors provides preset.colors) {
        MaterialTheme(
            colorScheme = preset.colors.toMaterialColorScheme(),
            typography = Typography,
            content = content
        )
    }
}
