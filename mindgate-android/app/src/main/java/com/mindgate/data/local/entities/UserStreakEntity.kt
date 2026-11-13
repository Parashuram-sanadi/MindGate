package com.mindgate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_streak")
data class UserStreakEntity(
    @PrimaryKey val userId: Long,
    val currentStreak: Int = 0,
    val lastCheckDate: String = ""
)
