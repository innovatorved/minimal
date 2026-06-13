package com.example.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.rememberBatteryLevel
import com.example.util.rememberNextCalendarEvent

@Composable
fun DefaultHomeClock(currentTime: String, currentDate: String) {
    Text(
        text = currentTime,
        color = Color.White,
        fontSize = 46.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        textAlign = TextAlign.Center
    )
    Text(
        text = currentDate,
        color = Color.LightGray,
        fontSize = 14.sp,
        fontFamily = FontFamily.SansSerif,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun HomeWidget(
    widgetId: String,
    currentTime: String,
    currentDate: String,
    sweepProgress: Float,
    isFocusActive: Boolean,
    focusSecsLeft: Int
) {
    when (widgetId) {
        "clock" -> Text(
            text = currentTime,
            color = Color.White,
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        "date" -> Text(
            text = currentDate,
            color = Color.LightGray,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        "battery" -> Text(
            text = rememberBatteryLevel().lowercase(),
            color = Color.LightGray,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        "focus ring" -> TimeRing(
            currentTime = currentTime,
            currentDate = currentDate,
            sweepProgress = sweepProgress,
            isFocusActive = isFocusActive,
            focusSecsLeft = focusSecsLeft,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        "calendar (next event)" -> Text(
            text = rememberNextCalendarEvent().lowercase(),
            color = Color.LightGray,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
