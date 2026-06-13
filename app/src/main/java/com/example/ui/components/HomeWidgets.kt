package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MinimalFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
        fontFamily = MinimalFontFamily,
        textAlign = TextAlign.Center
    )
    Text(
        text = currentDate,
        color = Color.LightGray,
        fontSize = 14.sp,
        fontFamily = MinimalFontFamily,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun HomeWidget(
    widgetId: String,
    currentTime: String,
    currentDate: String,
    homeDisplayName: String,
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
            fontFamily = MinimalFontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        "date" -> Text(
            text = currentDate,
            color = Color.LightGray,
            fontSize = 14.sp,
            fontFamily = MinimalFontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        "display name" -> {
            if (homeDisplayName.isNotBlank()) {
                Text(
                    text = homeDisplayName,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontFamily = MinimalFontFamily,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
        "battery" -> Text(
            text = rememberBatteryLevel().lowercase(),
            color = Color.LightGray,
            fontSize = 14.sp,
            fontFamily = MinimalFontFamily,
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
        "calendar (next event)" -> {
            val eventText = rememberNextCalendarEvent()
            if (eventText.isNotBlank()) {
                Text(
                    text = eventText.lowercase(),
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontFamily = MinimalFontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
