package com.mindgate.presentation.ui.screens.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.local.SessionManager
import com.mindgate.data.repository.AssessmentRepository
import com.mindgate.data.repository.CheckInRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckInUiState(
    val studentId: Long = 0,
    val showDailyReflection: Boolean = false,
    val reflectionPrompt: String = "",
    val reflectionTag: String = "",
    val isSubmitting: Boolean = false
)

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val checkInRepository: CheckInRepository,
    private val assessmentRepository: AssessmentRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(CheckInUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            val latestResult = assessmentRepository.getLatestResult(userId)
            
            val checkInDoneToday = sessionManager.checkInDone.first()

            val topFactor = latestResult?.topFactors?.firstOrNull() ?: "generic"
            val prompt = promptBank[topFactor] ?: promptBank["generic"]!!

            _state.value = CheckInUiState(
                studentId = userId,
                showDailyReflection = !checkInDoneToday,
                reflectionPrompt = prompt,
                reflectionTag = topFactor
            )
        }
    }

    private val promptBank = mapOf(
        "Late Night Usage" to "How can you wind down tonight without using your phone?",
        "Stress Level" to "What is one thing making you feel stressed today?",
        "Social Comparison" to "Did you find yourself comparing your life to others online today?",
        "FOMO" to "What's an activity you'd like to do more of offline?",
        "generic" to "How does gadget use affect your focus today?"
    )

    fun submit(mood: Int, reason: String, reflection: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true)
            val userId = _state.value.studentId
            checkInRepository.saveCheckIn(userId, mood, reason)
            if (reflection != null) {
                checkInRepository.saveReflection(userId, _state.value.reflectionTag, reflection)
            }
            onComplete()
        }
    }
}
