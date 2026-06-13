package com.example.util

import android.content.Context
import android.provider.CalendarContract
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun rememberNextCalendarEvent(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    var event by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        event = withContext(Dispatchers.IO) { fetchNextEvent(context) }
    }
    return event
}

private fun fetchNextEvent(context: Context): String {
    if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
        != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        return "calendar access needed"
    }
    val now = System.currentTimeMillis()
    val end = now + 7L * 24 * 60 * 60 * 1000
    val projection = arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN
    )
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
        .appendPath(now.toString())
        .appendPath(end.toString())
        .build()
    return try {
        context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val title = cursor.getString(0) ?: "event"
                    val begin = cursor.getLong(1)
                    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(begin))
                    "$time $title"
                } else ""
            } ?: ""
    } catch (_: Exception) {
        "calendar unavailable"
    }
}
