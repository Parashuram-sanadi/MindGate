package com.mindgate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val userId: Long,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val studyModeEnabled: Boolean = false,
    val studyModeStart: String = "09:00",
    val studyModeEnd: String = "17:00",
    val limitsActive: Boolean = false
)
