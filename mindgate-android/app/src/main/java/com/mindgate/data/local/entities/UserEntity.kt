package com.mindgate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val yearOfStudy: Int,
    val gender: String,
    val role: String = "student",
    val roleSelected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val assessmentCompleted: Boolean = false
)
