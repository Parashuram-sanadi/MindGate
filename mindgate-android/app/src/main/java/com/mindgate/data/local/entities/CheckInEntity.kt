package com.mindgate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val mood: Int, // 1-5
    val reason: String, // boredom, habit, need, avoiding, other
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "reflections")
data class ReflectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val promptTag: String, // SHAP factor or generic
    val response: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
