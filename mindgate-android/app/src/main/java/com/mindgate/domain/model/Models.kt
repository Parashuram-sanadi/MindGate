package com.mindgate.domain.model

data class UserModel(
    val id: Long,
    val name: String,
    val email: String,
    val yearOfStudy: Int,
    val gender: String,
    val role: String = "student",
    val roleSelected: Boolean = false,
    val assessmentCompleted: Boolean
)

data class RiskResult(
    val id: Long = 0,
    val riskLevel: String,
    val riskScore: Double,
    val confidence: Double,
    val primaryRecommendation: String,
    val recommendations: List<String>,
    val interventionLevel: String,
    val topFactors: List<String> = emptyList(),
    val downgraded: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val fromCache: Boolean = false,
    val apiWarning: String? = null
)

data class AppLimitModel(
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val isEnabled: Boolean,
    val suggestedLimit: Int,
    val usedMinutes: Int = 0,
    val isBlocked: Boolean = false,
    val effectiveLimitMinutes: Int = 0,
    val isCustomSchedule: Boolean = false,
    val extensionUntil: Long = 0L
)

data class AssessmentAnswers(
    val answers: Map<String, String> = emptyMap()
) {
    fun get(key: String): String? = answers[key]
    fun withAnswer(key: String, value: String): AssessmentAnswers =
        copy(answers = answers + (key to value))
}
