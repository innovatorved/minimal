package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.MinimalFontFamily
import com.example.ui.theme.launcherOnBackground
import com.example.ui.theme.launcherSecondary
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimeRing(
    currentTime: String,
    currentDate: String,
    sweepProgress: Float,
    isFocusActive: Boolean,
    focusSecsLeft: Int,
    modifier: Modifier = Modifier
) {
    val ringColor = launcherOnBackground()
    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = ringColor.copy(alpha = 0.15f),
                style = Stroke(width = 2.dp.toPx())
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = sweepProgress.coerceIn(0.01f, 1f) * 360f,
                useCenter = false,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTime,
                color = ringColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MinimalFontFamily,
                textAlign = TextAlign.Center
            )
            if (isFocusActive) {
                val minutes = focusSecsLeft / 60
                val seconds = focusSecsLeft % 60
                Text(
                    text = String.format("%02d:%02d left", minutes, seconds),
                    color = launcherSecondary(),
                    fontSize = 11.sp,
                    fontFamily = MinimalFontFamily,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = currentDate,
                    color = launcherSecondary(),
                    fontSize = 13.sp,
                    fontFamily = MinimalFontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
