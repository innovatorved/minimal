package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "muted_notifications")
data class MutedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
