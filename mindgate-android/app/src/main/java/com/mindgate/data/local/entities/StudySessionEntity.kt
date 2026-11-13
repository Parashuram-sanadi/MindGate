package com.mindgate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val source: String, // manual_toggle, inferred
    val dateKey: String, // YYYY-MM-DD
    val isSynced: Boolean = false
)

@Entity(tableName = "limit_warnings")
data class LimitWarningEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val appPackage: String,
    val dateKey: String, // YYYY-MM-DD
    val thresholdPercent: Int
)
