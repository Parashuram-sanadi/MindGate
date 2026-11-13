package com.mindgate.data.local

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper kept for backward compatibility — delegates session keys to [SessionManager].
 */
@Singleton
class PreferencesManager @Inject constructor(
    private val session: SessionManager
) {
    val onboardingDone = session.onboardingDone
    val currentUserId = session.currentUserId
    val assessmentStep = session.assessmentStep
    val assessmentAnswersJson = session.assessmentAnswersJson
    val checkInDone = session.checkInDone

    suspend fun setOnboardingDone(done: Boolean) = session.setOnboardingDone(done)

    suspend fun setCurrentUserId(id: Long) = session.saveUserId(id)

    suspend fun setCheckInDone(done: Boolean) = session.setCheckInDone(done)

    suspend fun saveAssessmentProgress(step: Int, answersJson: String) =
        session.saveAssessmentProgress(step, answersJson)

    suspend fun clearAssessmentProgress() = session.clearAssessmentProgress()

    suspend fun setLastQuizType(type: String) = session.setLastQuizType(type)

    suspend fun getLastQuizType(): String? = session.getLastQuizType()
}
