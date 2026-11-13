package com.mindgate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.remote.dto.ChildUsageResponse
import com.mindgate.data.remote.util.NetworkResult
import com.mindgate.data.repository.ParentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mindgate.data.repository.FirestoreRepository
import com.mindgate.domain.model.ExtensionRequest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class ChildUsageDetailViewModel @Inject constructor(
    private val repository: ParentRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _studentId = MutableStateFlow<Long?>(null)

    private val _usageData = MutableStateFlow<ChildUsageResponse?>(null)
    val usageData = _usageData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val earnedExtensions: StateFlow<List<ExtensionRequest>> = _studentId
        .filterNotNull()
        .flatMapLatest { id ->
            // In the Firebase-only flow, we need to know the student's Firebase UID.
            // For now, we use the backend ID string, as established in UID Identity section of info.md
            firestoreRepository.observeEarnedExtensionsForStudent(id.toString())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadUsage(studentId: Long) {
        _studentId.value = studentId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.getChildUsage(studentId)
            if (result is NetworkResult.Success) {
                _usageData.value = result.data
            } else {
                _error.value = result.errorDisplayMessage()
            }
            _isLoading.value = false
        }
    }
}
