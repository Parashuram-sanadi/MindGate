package com.mindgate.presentation.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mindgate.util.isValidEmail
import com.mindgate.BuildConfig
import com.mindgate.data.repository.GoogleAuthResult
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showRoleSelection: Boolean = false,
    val googleIdToken: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    val sessionManager: com.mindgate.data.local.SessionManager,
    private val parentServiceManager: com.mindgate.parent.manager.ParentServiceManager
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    fun signInWithGoogle(idToken: String, onSuccess: (Long, String, Boolean) -> Unit) {
        viewModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            userRepository.googleAuth(idToken)
                .onSuccess { result ->
                    when (result) {
                        is GoogleAuthResult.Success -> {
                            val id = result.userId
                            val user = userRepository.getUser(id)
                            val role = user?.role ?: "student"
                            if (role == "parent") parentServiceManager.startService()
                            onSuccess(id, role, user?.assessmentCompleted != true)
                            _state.value = AuthUiState()
                        }
                        is GoogleAuthResult.RoleRequired -> {
                            _state.value = AuthUiState(showRoleSelection = true, googleIdToken = idToken)
                        }
                    }
                }
                .onFailure { _state.value = AuthUiState(error = it.message) }
        }
    }

    fun completeGoogleAuth(role: String, onSuccess: (Long, String, Boolean) -> Unit) {
        val idToken = _state.value.googleIdToken ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            userRepository.googleAuthComplete(idToken, role)
                .onSuccess { response ->
                    val user = userRepository.getUser(response.userId)
                    if (response.role == "parent") parentServiceManager.startService()
                    onSuccess(response.userId, response.role, user?.assessmentCompleted != true)
                    _state.value = AuthUiState()
                }
                .onFailure { _state.value = _state.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun cancelRoleSelection() {
        _state.value = AuthUiState()
    }

    fun register(
        name: String,
        email: String,
        password: String,
        year: Int,
        gender: String,
        role: String = "student",
        onSuccess: (Long, String, Boolean) -> Unit
    ) {
        val err = validate(name, email, password) ?: run {
            viewModelScope.launch {
                _state.value = AuthUiState(isLoading = true)
                userRepository.register(name, email, password, year, gender, role)
                    .onSuccess { response ->
                        if (BuildConfig.DEBUG) Log.d("MINDGATE_AUTH", "Registered user id=${response.userId}, role=${response.role}")
                        if (response.role == "parent") parentServiceManager.startService()
                        onSuccess(response.userId, response.role, true) // New users always need assessment
                        _state.value = AuthUiState() 
                    }
                    .onFailure { _state.value = AuthUiState(error = it.message) }
            }
            return
        }
        _state.value = AuthUiState(error = err)
    }

    fun login(email: String, password: String, onSuccess: (Long, String, Boolean) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(error = "Fill all fields")
            return
        }
        viewModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            userRepository.login(email, password)
                .onSuccess { response ->
                    if (BuildConfig.DEBUG) Log.d("MINDGATE_AUTH", "Login success, userId=${response.userId}")
                    // Fetch full model only if we need to check assessment status
                    val user = userRepository.getUser(response.userId)
                    if (response.role == "parent") parentServiceManager.startService()
                    onSuccess(response.userId, response.role, user?.assessmentCompleted != true)
                    _state.value = AuthUiState()
                }
                .onFailure { _state.value = AuthUiState(error = it.message) }
        }
    }

    fun setError(message: String) {
        _state.value = _state.value.copy(error = message, isLoading = false)
    }

    private fun validate(name: String, email: String, password: String): String? = when {
        name.isBlank() -> "Name is required"
        !isValidEmail(email) -> "Invalid email format"
        password.length < 6 -> "Password must be at least 6 characters"
        else -> null
    }
}
