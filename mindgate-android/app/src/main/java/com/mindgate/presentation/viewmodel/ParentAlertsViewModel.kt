package com.mindgate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.remote.dto.ParentAlertResponse
import com.mindgate.data.remote.util.NetworkResult
import com.mindgate.data.remote.util.safeApiCall
import com.mindgate.data.remote.api.ParentApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentAlertsViewModel @Inject constructor(
    private val api: ParentApiService
) : ViewModel() {

    private val _alerts = MutableStateFlow<List<ParentAlertResponse>>(emptyList())
    val alerts = _alerts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = safeApiCall { api.listAlerts() }
            if (result is NetworkResult.Success) {
                _alerts.value = result.data
            } else {
                _error.value = result.errorDisplayMessage()
            }
            _isLoading.value = false
        }
    }
}
