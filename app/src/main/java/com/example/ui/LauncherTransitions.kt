package com.example.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import com.example.ui.navigation.AppScreen
import com.example.ui.navigation.NavigationDirection

private val drawerTween = tween<IntOffset>(durationMillis = 300)

fun AnimatedContentTransitionScope<AppScreen>.launcherScreenTransition(
    navigationDirection: NavigationDirection
): ContentTransform {
    val openingDrawer =
        initialState == AppScreen.Home && targetState == AppScreen.AppDrawer
    val closingDrawer =
        initialState == AppScreen.AppDrawer && targetState == AppScreen.Home
    val returningHome =
        targetState == AppScreen.Home &&
            initialState != AppScreen.Home &&
            initialState != AppScreen.AppDrawer

    return when {
        openingDrawer -> {
            slideInVertically(drawerTween) { fullHeight -> fullHeight } +
                fadeIn(tween(220)) togetherWith
                fadeOut(tween(180))
        }
        closingDrawer || returningHome -> {
            fadeIn(tween(200)) togetherWith
                slideOutVertically(drawerTween) { fullHeight -> fullHeight } +
                fadeOut(tween(220))
        }
        navigationDirection == NavigationDirection.Forward -> {
            slideInHorizontally(tween(260)) { fullWidth -> fullWidth } + fadeIn(tween(220)) togetherWith
                slideOutHorizontally(tween(260)) { fullWidth -> -fullWidth / 5 } + fadeOut(tween(180))
        }
        else -> {
            slideInHorizontally(tween(260)) { fullWidth -> -fullWidth / 5 } + fadeIn(tween(220)) togetherWith
                slideOutHorizontally(tween(260)) { fullWidth -> fullWidth } + fadeOut(tween(180))
        }
    }
}
