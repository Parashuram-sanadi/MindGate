package com.mindgate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "behavior_signals")
data class BehaviorSignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val type: String, // protection_disabled, state_mismatch, fast_solve_rejected, repeated_denial, clock_skew
    val timestamp: Long = System.currentTimeMillis(),
    val payload: String? = null,
    val isSynced: Boolean = false
)
