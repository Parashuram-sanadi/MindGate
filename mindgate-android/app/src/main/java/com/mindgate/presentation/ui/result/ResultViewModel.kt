package com.mindgate.presentation.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.local.PreferencesManager
import com.mindgate.data.repository.AssessmentRepository
import com.mindgate.domain.model.RiskResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val assessmentRepository: AssessmentRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _result = MutableStateFlow<RiskResult?>(null)
    val result = _result.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = prefs.currentUserId.first()
            _result.value = assessmentRepository.getLatestResult(userId)
        }
    }
}
