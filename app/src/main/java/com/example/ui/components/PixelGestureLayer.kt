package com.example.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

private data class SwipeGestureState(
    val start: Offset,
    val startedInBottomZone: Boolean,
    var consumedByChild: Boolean = false
)

private fun Modifier.trackSwipeUpOnRelease(
    swipeThresholdPx: Float,
    pointerPass: PointerEventPass,
    onSwipeUp: () -> Unit
): Modifier = pointerInput(onSwipeUp, swipeThresholdPx, pointerPass) {
    val startPositions = mutableMapOf<PointerId, Offset>()

    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(pointerPass)
            event.changes.forEach { change ->
                when {
                    change.changedToDownIgnoreConsumed() -> {
                        startPositions[change.id] = change.position
                    }

                    change.changedToUpIgnoreConsumed() -> {
                        val start = startPositions.remove(change.id) ?: return@forEach
                        val delta = change.position - start
                        val swipeUpDistance = -delta.y
                        if (
                            swipeUpDistance > swipeThresholdPx &&
                            swipeUpDistance > abs(delta.x) * 1.35f
                        ) {
                            onSwipeUp()
                        }
                    }

                    !change.pressed -> {
                        startPositions.remove(change.id)
                    }
                }
            }
        }
    }
}

/**
 * Swipe up on the home screen to open the app drawer. Evaluated on pointer-up
 * only (never consumes mid-drag) so pinned-app taps and short drags still work.
 */
@Composable
fun Modifier.pixelHomeDrawerSwipe(onSwipeUp: () -> Unit): Modifier {
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 52.dp.toPx() }
    return trackSwipeUpOnRelease(swipeThresholdPx, PointerEventPass.Initial, onSwipeUp)
}

/**
 * Pixel-style return home: only when the gesture **starts in the bottom zone**
 * (bottom [bottomZoneFraction] of the screen, at least [120.dp] tall), moves
 * clearly upward, and was **not consumed** by a scrollable child during the drag.
 */
@Composable
fun Modifier.pixelBottomSwipeHome(
    enabled: Boolean,
    bottomZoneFraction: Float = 0.25f,
    onSwipeHome: () -> Unit
): Modifier {
    if (!enabled) return this

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 52.dp.toPx() }
    val bottomZoneMinPx = with(density) { 120.dp.toPx() }

    return pointerInput(onSwipeHome, swipeThresholdPx, bottomZoneFraction, bottomZoneMinPx) {
        val gestures = mutableMapOf<PointerId, SwipeGestureState>()

        fun bottomZoneTopY(): Float {
            val zoneHeight = max(size.height * bottomZoneFraction, bottomZoneMinPx)
            return (size.height - zoneHeight).coerceAtLeast(0f)
        }

        awaitPointerEventScope {
            while (true) {
                // Main pass: scrollables consume drags before we decide on pointer-up.
                val event = awaitPointerEvent(PointerEventPass.Main)
                event.changes.forEach { change ->
                    when {
                        change.changedToDownIgnoreConsumed() -> {
                            gestures[change.id] = SwipeGestureState(
                                start = change.position,
                                startedInBottomZone = change.position.y >= bottomZoneTopY()
                            )
                        }

                        change.pressed -> {
                            gestures[change.id]?.let { gesture ->
                                if (change.isConsumed) {
                                    gesture.consumedByChild = true
                                }
                            }
                        }

                        change.changedToUpIgnoreConsumed() -> {
                            val gesture = gestures.remove(change.id) ?: return@forEach
                            if (!gesture.startedInBottomZone || gesture.consumedByChild) return@forEach

                            val delta = change.position - gesture.start
                            val swipeUpDistance = -delta.y
                            if (
                                swipeUpDistance > swipeThresholdPx &&
                                swipeUpDistance > abs(delta.x) * 1.35f
                            ) {
                                onSwipeHome()
                            }
                        }

                        !change.pressed -> {
                            gestures.remove(change.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.launcherSwipeBack(
    enabled: Boolean,
    onBack: () -> Unit
): Modifier {
    if (!enabled) return this

    val density = LocalDensity.current
    val thresholdPx = with(density) { 88.dp.toPx() }

    return pointerInput(onBack) {
        var totalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalDrag = 0f },
            onDragEnd = {
                if (abs(totalDrag) >= thresholdPx) {
                    onBack()
                }
                totalDrag = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
                totalDrag += dragAmount
                change.consume()
            }
        )
    }
}
