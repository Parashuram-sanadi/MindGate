package com.mindgate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parent_child_relations")
data class ParentChildRelationEntity(
    @PrimaryKey val studentId: Long,
    val name: String,
    val status: String,
    val studentFirebaseUid: String? = null,
    val lastSyncAt: Long = System.currentTimeMillis()
)
