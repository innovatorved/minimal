package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MinimalColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = Color(0xFFB0B0B0),
    tertiary = Color(0xFFB0B0B0),
    background = Color.Black,
    surface = Color.Black,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MinimalColorScheme,
        typography = Typography,
        content = content
    )
}
