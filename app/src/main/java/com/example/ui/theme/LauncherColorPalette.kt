package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

data class LauncherColors(
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val onSurface: Color,
    val secondary: Color,
    val muted: Color,
    val dialogContainer: Color = background,
    val isLightBackground: Boolean = false
) {
    fun toMaterialColorScheme() = if (isLightBackground) {
        lightColorScheme(
            primary = onBackground,
            secondary = secondary,
            tertiary = muted,
            background = background,
            surface = surface,
            onPrimary = background,
            onSecondary = onBackground,
            onBackground = onBackground,
            onSurface = onSurface
        )
    } else {
        darkColorScheme(
            primary = onBackground,
            secondary = secondary,
            tertiary = muted,
            background = background,
            surface = surface,
            onPrimary = background,
            onSecondary = onBackground,
            onBackground = onBackground,
            onSurface = onSurface
        )
    }
}

data class LauncherThemePreset(
    val id: String,
    val label: String,
    val swatch: Color,
    val colors: LauncherColors
)

private data class PixelBackColor(
    val id: String,
    val label: String,
    val hex: Long
)

object LauncherThemes {
    val defaultId = "obsidian"

    private val obsidianText = Color(0xFF1A1C1E)
    private val porcelainText = Color(0xFFF3EFE9)

    private val pixelColors = listOf(
        PixelBackColor("black", "black", 0xFF000000),
        PixelBackColor("obsidian", "obsidian", 0xFF1A1C1E),
        PixelBackColor("limoncello", "limoncello", 0xFFE2E7A9),
        PixelBackColor("mint", "mint", 0xFFBCE6CD),
        PixelBackColor("powder_blue", "powder blue", 0xFFBACDD8),
        PixelBackColor("sorta_sunny", "sorta sunny", 0xFFFCE1AC),
        PixelBackColor("clearly_white", "clearly white", 0xFFFFFFFF)
    )

    val presets: List<LauncherThemePreset> = pixelColors.map { it.toPreset() }

    fun presetForId(id: String): LauncherThemePreset {
        val legacy = legacyIdMap[id]
        if (legacy !== null) {
            return presets.firstOrNull { it.id == legacy } ?: presets.first()
        }
        return presets.firstOrNull { it.id == id } ?: presets.first()
    }

    fun backgroundArgbForThemeId(id: String): Int = presetForId(id).swatch.value.toInt()

    private fun PixelBackColor.toPreset(): LauncherThemePreset {
        val background = Color(hex)
        val isLight = background.luminance() > 0.55f
        val onBackground = if (isLight) obsidianText else porcelainText
        val secondary = lerp(background, onBackground, 0.70f)
        val muted = lerp(background, onBackground, 0.45f)
        return LauncherThemePreset(
            id = id,
            label = label,
            swatch = background,
            colors = LauncherColors(
                background = background,
                surface = background,
                onBackground = onBackground,
                onSurface = onBackground,
                secondary = secondary,
                muted = muted,
                isLightBackground = isLight
            )
        )
    }

    private val legacyIdMap = mapOf(
        "hazel" to "obsidian",
        "lemongrass" to "limoncello",
        "cloudy_white" to "clearly_white",
        "not_pink" to "sorta_sunny",
        "purple_ish" to "clearly_white",
        "charcoal" to "obsidian",
        "porcelain" to "clearly_white",
        "pixel_sage" to "obsidian",
        "sorta_sage" to "obsidian",
        "pixel_coral" to "sorta_sunny",
        "kinda_coral" to "sorta_sunny",
        "pixel_seafoam" to "mint",
        "sorta_seafoam" to "mint",
        "pixel_bay" to "obsidian",
        "bay" to "obsidian",
        "pixel_hazel" to "obsidian",
        "pixel_rose" to "sorta_sunny",
        "rose_quartz" to "sorta_sunny",
        "pixel_wintergreen" to "mint",
        "wintergreen" to "mint",
        "google_blue" to "obsidian",
        "really_blue" to "obsidian",
        "google_green" to "mint",
        "google_red" to "sorta_sunny",
        "coral" to "sorta_sunny",
        "navy" to "obsidian",
        "indigo" to "obsidian",
        "purple" to "clearly_white",
        "teal" to "mint",
        "brown" to "obsidian",
        "snow" to "clearly_white",
        "sea" to "mint",
        "sage" to "obsidian",
        "oh_so_orange" to "sorta_sunny",
        "panda_orange" to "sorta_sunny",
        "pixel3_mint" to "mint",
        "just_black_button" to "clearly_white",
        "clearly_white_button" to "sorta_sunny",
        "moonstone" to "obsidian",
        "jade" to "obsidian",
        "peony" to "sorta_sunny"
    )
}

val LocalLauncherColors = compositionLocalOf { LauncherThemes.presetForId(LauncherThemes.defaultId).colors }

@Composable
fun launcherBackground(): Color = LocalLauncherColors.current.background

@Composable
fun launcherSurface(): Color = LocalLauncherColors.current.surface

@Composable
fun launcherOnBackground(): Color = LocalLauncherColors.current.onBackground

@Composable
fun launcherOnSurface(): Color = LocalLauncherColors.current.onSurface

@Composable
fun launcherSecondary(): Color = LocalLauncherColors.current.secondary

@Composable
fun launcherMuted(): Color = LocalLauncherColors.current.muted

@Composable
fun launcherDialogContainer(): Color = LocalLauncherColors.current.dialogContainer

@Composable
fun launcherIsLightBackground(): Boolean = LocalLauncherColors.current.isLightBackground
