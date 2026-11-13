package com.mindgate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.local.SessionManager
import com.mindgate.data.remote.api.MindGateApiService
import com.mindgate.data.remote.dto.SelectRoleRequest
import com.mindgate.data.remote.util.NetworkResult
import com.mindgate.data.remote.util.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoleGateViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val api: MindGateApiService 
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun selectRole(role: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = safeApiCall { api.selectRole(SelectRoleRequest(role)) }
            
            if (result is NetworkResult.Success) {
                onSuccess()
            } else {
                _error.value = result.errorDisplayMessage()
            }
            _isLoading.value = false
        }
    }
}
