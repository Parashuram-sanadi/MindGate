package com.mindgate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessment_results")
data class AssessmentResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val riskLevel: String,
    val riskScore: Double,
    val confidence: Double,
    val recommendationsJson: String,
    val primaryRecommendation: String,
    val interventionLevel: String,
    val topFactorsJson: String? = null,
    val answersJson: String,
    val downgraded: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isLatest: Boolean = true
)
