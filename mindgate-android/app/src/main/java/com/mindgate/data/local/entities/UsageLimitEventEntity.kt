package com.mindgate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_limit_events")
data class UsageLimitEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackage: String?,
    val limitSeconds: Int,
    val actualUsageSeconds: Int,
    val timestamp: Long,
    val eventDate: String, // YYYY-MM-DD
    val synced: Boolean = false
)
