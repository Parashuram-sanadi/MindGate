package com.mindgate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extension_grants")
data class ExtensionGrant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val packageName: String,      // e.g. "com.instagram.android"
    val grantedAt: Long,          // System.currentTimeMillis()
    val expiresAt: Long,          // grantedAt + 15 * 60 * 1000
    val extensionMinutes: Int,    // always 15
    val date: String,             // "yyyy-MM-dd" for daily cap counting
    val quizType: String,         // "MATH" | "TRIVIA" | "parent_approved"
    val externalId: String? = null // For deduplicating Firestore requests
)
