package com.mindgate.presentation.ui.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.local.PreferencesManager
import com.mindgate.data.repository.AssessmentRepository
import com.mindgate.data.repository.UserRepository
import com.mindgate.domain.model.AssessmentAnswers
import com.mindgate.domain.model.RiskResult
import com.mindgate.domain.usecase.SubmitAssessmentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssessmentUiState(
    val currentStep: Int = 1,
    val answers: AssessmentAnswers = AssessmentAnswers(),
    val isSubmitting: Boolean = false,
    val isAnalyzing: Boolean = false,
    val result: RiskResult? = null,
    val error: String? = null,
    val userId: Long = -1L
)

@HiltViewModel
class AssessmentViewModel @Inject constructor(
    private val submitUseCase: SubmitAssessmentUseCase,
    private val assessmentRepository: AssessmentRepository,
    private val userRepository: UserRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(AssessmentUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = prefs.currentUserId.first().takeIf { it > 0 }
                ?: userRepository.getFirstUser()?.id ?: -1L
            val (step, answers) = assessmentRepository.getSavedProgress()
            _state.value = _state.value.copy(
                userId = userId,
                currentStep = if (step > 0) step else 1,
                answers = answers
            )
        }
    }

    fun setAnswer(key: String, value: String) {
        val updated = _state.value.answers.withAnswer(key, value)
        _state.value = _state.value.copy(answers = updated)
        viewModelScope.launch {
            assessmentRepository.saveProgress(_state.value.currentStep, updated)
        }
    }

    fun nextStep() {
        if (_state.value.currentStep < 5) {
            val step = _state.value.currentStep + 1
            _state.value = _state.value.copy(currentStep = step)
            viewModelScope.launch {
                assessmentRepository.saveProgress(step, _state.value.answers)
            }
        }
    }

    fun prevStep() {
        if (_state.value.currentStep > 1) {
            _state.value = _state.value.copy(currentStep = _state.value.currentStep - 1)
        }
    }

    fun isStepValid(step: Int): Boolean {
        val required = AssessmentQuestions.forStep(step).filter { it.type != QuestionType.READONLY }
        return required.all { q ->
            val v = _state.value.answers.get(q.id)
            !v.isNullOrBlank()
        }
    }

    fun submit(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true, isAnalyzing = true)
            if (com.mindgate.BuildConfig.DEBUG) {
                android.util.Log.d("MINDGATE_ASSESS", "Building request with userId=${_state.value.userId}")
                android.util.Log.d("MINDGATE_ASSESS", "Answers=${_state.value.answers}")
            }
            delay(2000)
            val result = submitUseCase(_state.value.userId, _state.value.answers)
            result.onSuccess { risk ->
                _state.value = _state.value.copy(isSubmitting = false, isAnalyzing = false, result = risk)
                onComplete()
            }.onFailure {
                _state.value = _state.value.copy(
                    isSubmitting = false, isAnalyzing = false, error = it.message
                )
            }
        }
    }
}
