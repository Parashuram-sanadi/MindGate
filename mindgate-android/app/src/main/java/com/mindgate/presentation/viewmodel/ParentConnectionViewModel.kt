package com.mindgate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.remote.util.NetworkResult
import com.mindgate.data.repository.ParentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ParentConnectionViewModel @Inject constructor(
    private val repository: ParentRepository,
    private val sessionManager: com.mindgate.data.local.SessionManager
) : ViewModel() {

    private val _linkCode = MutableStateFlow<String?>(null)
    val linkCode = _linkCode.asStateFlow()

    private val _timeLeft = MutableStateFlow<String?>(null)
    val timeLeft = _timeLeft.asStateFlow()

    private val _isExpired = MutableStateFlow(false)
    val isExpired = _isExpired.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var timerJob: Job? = null

    private val _isLinked = MutableStateFlow(false)
    val isLinked = _isLinked.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var pollJob: Job? = null

    fun generateCode() {
        viewModelScope.launch {
            android.util.Log.d("LinkFlow", "generateCode() called. Resetting state.")
            _isLoading.value = true
            _error.value = null
            _isExpired.value = false // Reset expiry state immediately
            _linkCode.value = null   // Clear old code to show loading spinner
            
            try {
                val result = repository.createLinkCode()
                android.util.Log.d("LinkFlow", "createLinkCode result: $result")
                if (result is NetworkResult.Success) {
                    android.util.Log.i("LinkFlow", "Successfully generated new code: ${result.data.code}")
                    _linkCode.value = result.data.code
                    startTimer(result.data.expiresAt, result.data.ttlSeconds)
                    startPolling()
                } else {
                    val msg = result.errorDisplayMessage()
                    android.util.Log.e("LinkFlow", "Failed to generate code: $msg")
                    _error.value = msg
                    // If it failed and we don't have a code, stay on error
                    // If we had one, maybe it was a retry that failed
                }
            } catch (e: Exception) {
                android.util.Log.e("LinkFlow", "Exception in generateCode", e)
                _error.value = "Connection failed: ${e.message}. Check your WiFi and Server URL."
            } finally {
                _isLoading.value = false
                android.util.Log.d("LinkFlow", "generateCode() finished.")
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        // Use GlobalScope or a custom application scope here because 
        // the user might navigate away before the parent completes the link.
        // viewModelScope is too short-lived for this critical session sync.
        pollJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                val statusResult = repository.checkConnectionStatus()
                if (statusResult is NetworkResult.Success && statusResult.data.isLinked) {
                    sessionManager.setLinked(true)
                    sessionManager.saveParentUid(statusResult.data.linkedParentFirebaseUid)
                    _isLinked.value = true
                    break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        pollJob?.cancel()
    }

    private fun startTimer(expiresAtIso: String, ttlSeconds: Int? = null) {
        timerJob?.cancel()
        _isExpired.value = false
        timerJob = viewModelScope.launch {
            try {
                val now = Instant.now()
                val expiry = if (ttlSeconds != null) {
                    // Use TTL to avoid clock drift issues entirely
                    now.plusSeconds(ttlSeconds.toLong())
                } else {
                    // Fallback to absolute time parsing
                    try {
                        java.time.OffsetDateTime.parse(expiresAtIso).toInstant()
                    } catch (e: Exception) {
                        try {
                            java.time.ZonedDateTime.parse(expiresAtIso).toInstant()
                        } catch (e2: Exception) {
                            val clean = if (expiresAtIso.contains(" ")) expiresAtIso.replace(" ", "T") else expiresAtIso
                            java.time.LocalDateTime.parse(clean.substringBefore("+")).toInstant(java.time.ZoneOffset.UTC)
                        }
                    }
                }

                while (true) {
                    val currentTime = Instant.now()
                    val duration = Duration.between(currentTime, expiry)
                    if (duration.isNegative || duration.isZero) {
                        _timeLeft.value = "00:00"
                        _isExpired.value = true
                        break
                    }
                    val minutes = duration.toMinutes()
                    val seconds = duration.minusMinutes(minutes).seconds
                    _timeLeft.value = java.util.Locale.US.let { String.format(it, "%02d:%02d", minutes, seconds) }
                    delay(1000)
                }
            } catch (e: Exception) {
                android.util.Log.e("TIMER", "Failed to parse expiry: $expiresAtIso", e)
                _timeLeft.value = null
            }
        }
    }

    fun acceptCode(code: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            val result = repository.acceptLinkCode(code)
            if (result is NetworkResult.Success) {
                onSuccess()
            } else {
                _error.value = result.errorDisplayMessage()
            }
        }
    }
}
