package com.mindgate.presentation.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.remote.util.NetworkResult
import com.mindgate.domain.manager.ExtensionManager
import com.mindgate.domain.model.Challenge
import com.mindgate.domain.model.ChallengeGenerator
import com.mindgate.domain.model.ChallengeType
import com.mindgate.domain.model.ExtensionRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth

@HiltViewModel
class QuizPuzzleViewModel @Inject constructor(
    private val extensionManager: ExtensionManager,
    private val userRepository: com.mindgate.data.repository.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private lateinit var targetPackage: String
    private lateinit var appName: String
    private var currentChallenge: Challenge? = null
    private var attemptsLeft = 3
    private var challengeStartTime = 0L

    fun init(packageName: String, name: String) {
        targetPackage = packageName
        appName = name
        _uiState.value = QuizUiState.Calming
    }

    fun onCalmFinished() {
        loadChallenge()
    }

    private fun loadChallenge() {
        val type = ChallengeType.entries.random()
        val challenge = ChallengeGenerator.generate(type)
        currentChallenge = challenge
        attemptsLeft = 3
        challengeStartTime = System.currentTimeMillis()
        _uiState.value = QuizUiState.ChallengeActive(
            challenge = challenge,
            attemptsLeft = attemptsLeft,
            timerSeconds = when (type) {
                ChallengeType.MATH -> 60
                ChallengeType.TRIVIA -> 90
            }
        )
    }

    fun submitAnswer(answer: String) {
        val challenge = currentChallenge ?: return
        
        if (challenge.checkAnswer(answer)) {
            viewModelScope.launch {
                _uiState.value = QuizUiState.Loading
                
                when (val result = extensionManager.grantExtension(
                    targetPackage, challenge.type.name
                )) {
                    is com.mindgate.domain.manager.ExtensionResult.Granted -> {
                        _uiState.value = QuizUiState.Passed(
                            expiresAt = result.expiresAt,
                            remainingToday = result.remainingToday
                        )
                    }
                    is com.mindgate.domain.manager.ExtensionResult.PendingFirestore -> {
                        _uiState.value = QuizUiState.PendingApprovalFirestore(result.requestId)
                        listenForFirestoreApproval(result.requestId)
                    }
                    is com.mindgate.domain.manager.ExtensionResult.DailyCapReached -> {
                        _uiState.value = QuizUiState.DailyCapReached(cap = result.cap)
                    }
                    is com.mindgate.domain.manager.ExtensionResult.PolicyBlocked -> {
                        _uiState.value = QuizUiState.Failed(result.reason)
                    }
                    else -> {
                        _uiState.value = QuizUiState.QuizPassedWaitingSubmit(appName = appName)
                    }
                }
            }
        } else {
            attemptsLeft--
            if (attemptsLeft <= 0) {
                _uiState.value = QuizUiState.Failed(
                    "No attempts left. Take a break and come back! 🌿"
                )
            } else {
                val current = _uiState.value
                if (current is QuizUiState.ChallengeActive) {
                    _uiState.value = current.copy(
                        attemptsLeft = attemptsLeft,
                        lastAnswerWrong = true
                    )
                }
            }
        }
    }

    fun submitExtensionRequest() {
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            
            val userId = extensionManager.getUserId()
            
            // Requirement 1: Direct UID, no anonymous fallback
            val auth = FirebaseAuth.getInstance()
            val currentUid = auth.currentUser?.uid

            if (currentUid != null) {
                // Resolved Parent ID from parentChildLinks
                val parentId = extensionManager.getParentIdForStudent(currentUid)
                
                if (parentId != null) {
                    val studentName = userRepository.getUser(userId)?.name ?: "Student"
                    
                    val result = extensionManager.createFirestoreRequest(
                        studentUid = currentUid,
                        parentUid = parentId,
                        appPackage = targetPackage,
                        studentName = studentName
                    )
                    
                    if (result.isSuccess) {
                        val docId = result.getOrNull() ?: ""
                        _uiState.value = QuizUiState.PendingApprovalFirestore(docId)
                        listenForFirestoreApproval(docId)
                        extensionManager.notifyParentOfRequest(appName)
                    } else {
                        _uiState.value = QuizUiState.Failed("Failed to submit request to Firestore.")
                    }
                } else {
                    _uiState.value = QuizUiState.Failed("No linked parent found.")
                }
            } else {
                _uiState.value = QuizUiState.Failed("Firebase auth failed.")
            }
        }
    }

    private fun listenForFirestoreApproval(requestId: String) {
        viewModelScope.launch {
            extensionManager.observeFirestoreRequest(requestId).collect { request ->
                if (request == null) return@collect
                
                when (request.status) {
                    "approved" -> {
                        val respondedAtMs = request.respondedAt?.toDate()?.time ?: System.currentTimeMillis()
                        
                        viewModelScope.launch {
                            val applied = extensionManager.applyFirestoreApproval(targetPackage, 15, respondedAtMs, request.id)
                            if (applied) {
                                extensionManager.markFirestoreRequestConsumed(request.id)
                                
                                val grant = extensionManager.getActiveExtension(targetPackage)
                                if (grant != null) {
                                    _uiState.value = QuizUiState.Passed(
                                        expiresAt = grant.expiresAt,
                                        remainingToday = 0
                                    )
                                }
                            }
                        }
                    }
                    "denied" -> {
                        _uiState.value = QuizUiState.Failed("Parent denied the extension request.")
                    }
                }
            }
        }
    }

    fun onTimerExpired() {
        _uiState.value = QuizUiState.Failed("Time's up! Try again after a break. ⏱")
    }
}
