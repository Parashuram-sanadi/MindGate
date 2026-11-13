package com.mindgate.presentation.ui.quiz

import com.mindgate.domain.model.Challenge

sealed class QuizUiState {
    object Loading : QuizUiState()
    object Calming : QuizUiState() // 4.8 Calming step
    data class ChallengeActive(
        val challenge: Challenge,
        val attemptsLeft: Int,
        val timerSeconds: Int,
        val lastAnswerWrong: Boolean = false
    ) : QuizUiState()
    data class Passed(val expiresAt: Long, val remainingToday: Int) : QuizUiState()
    data class QuizPassedWaitingSubmit(val appName: String) : QuizUiState()
    data class PendingApproval(val requestId: Long) : QuizUiState()
    data class PendingApprovalFirestore(val requestId: String) : QuizUiState()
    data class Failed(val reason: String) : QuizUiState()
    data class DailyCapReached(val cap: Int) : QuizUiState()
}
