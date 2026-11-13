package com.mindgate.presentation.ui.screens.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.local.SessionManager
import com.mindgate.data.repository.EligibilityResponse
import com.mindgate.data.repository.StudyRepository
import com.mindgate.domain.manager.ExtensionManager
import com.mindgate.domain.manager.ExtensionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedOverlayUiState(
    val eligibility: EligibilityResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val extensionGranted: Boolean = false,
    val expiresAt: Long = 0L,
    val isUnblockedManually: Boolean = false
)

@HiltViewModel
class BlockedOverlayViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val extensionManager: ExtensionManager,
    private val sessionManager: SessionManager,
    private val usageRepository: com.mindgate.data.repository.UsageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BlockedOverlayUiState())
    val state = _state.asStateFlow()

    fun loadEligibility() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val userId = sessionManager.getUserId()
            val result = studyRepository.getEligibility(userId)
            result.onSuccess { eligibility ->
                _state.update { it.copy(eligibility = eligibility, isLoading = false) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun observeLimitStatus(packageName: String) {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId <= 0) return@launch
            
            // Observe the limit for this package. If extensionUntil is set, we close.
            usageRepository.observeLimitsWithUsage(userId).collect { limits ->
                val limit = limits.find { it.packageName == packageName }
                if (limit != null) {
                    val hasExtension = limit.extensionUntil > System.currentTimeMillis()
                    val isUnderLimit = limit.usedMinutes < limit.effectiveLimitMinutes
                    
                    if (hasExtension || isUnderLimit) {
                        _state.update { it.copy(isUnblockedManually = true) }
                    }
                }
            }
        }
    }

    fun unlockViaStudy(packageName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = extensionManager.grantExtension(packageName, "study_duration")
            when (result) {
                is ExtensionResult.Granted -> {
                    _state.update { it.copy(extensionGranted = true, expiresAt = result.expiresAt, isLoading = false) }
                }
                else -> {
                    _state.update { it.copy(error = "Failed to unlock via study", isLoading = false) }
                }
            }
        }
    }
}
