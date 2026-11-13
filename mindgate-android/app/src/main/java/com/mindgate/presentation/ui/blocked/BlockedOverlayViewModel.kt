package com.mindgate.presentation.ui.blocked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.local.SessionManager
import com.mindgate.data.remote.util.NetworkResult
import com.mindgate.data.repository.FirestoreRepository
import com.mindgate.domain.manager.ExtensionManager
import com.mindgate.domain.model.ExtensionRequest
import com.mindgate.util.MindGateLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth

data class BlockedUiState(
    val status: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val extensionUntil: Long = 0L,
    val dailyLimitMinutes: Int = 0
)

@HiltViewModel
class BlockedOverlayViewModel @Inject constructor(
    private val extensionManager: ExtensionManager,
    private val firestoreRepository: FirestoreRepository,
    private val sessionManager: SessionManager,
    private val usageRepository: com.mindgate.data.repository.UsageRepository,
    private val userRepository: com.mindgate.data.repository.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedUiState())
    val uiState = _uiState.asStateFlow()

    private var targetPackage: String = ""
    private var appName: String = ""

    fun init(packageName: String, name: String) {
        targetPackage = packageName
        appName = name
        checkCurrentExtension()
        loadDailyLimit()
    }

    private fun loadDailyLimit() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            val limit = usageRepository.getLimit(userId, targetPackage)
            val minutes = limit?.getEffectiveLimitMinutes() ?: 0
            _uiState.update { it.copy(dailyLimitMinutes = minutes) }
            MindGateLog.d("BLOCKED", "Loaded daily limit for $targetPackage: $minutes min")
        }
    }

    private fun checkCurrentExtension() {
        viewModelScope.launch {
            val grant = extensionManager.getActiveExtension(targetPackage)
            if (grant != null) {
                _uiState.update { it.copy(extensionUntil = grant.expiresAt) }
            }
        }
    }

    fun requestFromParent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // 1. Ensure Firebase Auth is active
            val auth = FirebaseAuth.getInstance()
            val currentUid = auth.currentUser?.uid

            if (currentUid == null) {
                _uiState.update { it.copy(error = "You must be logged in to request an extension.", isLoading = false) }
                return@launch
            }

            // 2. Resolve Parent ID (Primary: Firestore for real UID, Secondary: SessionManager)
            // We search Firestore first because it contains the long Firebase string (JRFu...)
            var parentId = firestoreRepository.getParentIdForStudent(currentUid)
            
            if (parentId == null || parentId.isBlank() || parentId.length < 5) {
                parentId = sessionManager.linkedParentUid.first()
            }
            
            if (parentId != null && parentId.isNotBlank() && parentId.length > 5) {
                val studentName = userRepository.getUser(sessionManager.getUserId())?.name ?: "sonu"
                
                // 3. Create request in extension_requests (Standardizing on Uppercase PENDING)
                val result = firestoreRepository.createExtensionRequest(
                    studentUid = currentUid,
                    parentUid = parentId,
                    appPackage = targetPackage,
                    appName = appName,
                    studentName = studentName,
                    status = "PENDING"
                )
                
                result.onSuccess { docId ->
                    _uiState.update { it.copy(status = "PENDING", isLoading = false) }
                    listenForApproval(docId)
                    // Sync link status in background if we found it via Firestore
                    viewModelScope.launch { extensionManager.syncLinkStatus() }
                }.onFailure { e ->
                    _uiState.update { it.copy(error = "Failed to create request: ${e.message}", isLoading = false) }
                    MindGateLog.e("BLOCKED", "Firestore request failed", e)
                }
            } else {
                // Try one last sync from backend
                extensionManager.syncLinkStatus()
                val freshParentId = sessionManager.linkedParentUid.first()
                if (freshParentId != null) {
                    requestFromParent() // Retry once
                } else {
                    _uiState.update { it.copy(error = "No linked parent found. Please ensure accounts are linked in your Profile.", isLoading = false) }
                }
            }
        }
    }

    private fun listenForApproval(requestId: String) {
        viewModelScope.launch {
            firestoreRepository.observeRequestStatus(requestId).collect { request ->
                if (request == null) return@collect
                
                // ALIGNED: lowercase comparison per rules
                val status = request.status.lowercase()
                _uiState.update { it.copy(status = status) }
                
                if (status == "approved") {
                    // Logic to grant extension in local Room DB
                    extensionManager.grantExtension(targetPackage, "parent_approved")
                    
                    val grant = extensionManager.getActiveExtension(targetPackage)
                    if (grant != null) {
                        _uiState.update { it.copy(extensionUntil = grant.expiresAt) }
                    }
                    
                    // Requirement: match rule precisely (Mark as consumed if approved)
                    firestoreRepository.markRequestConsumed(requestId)
                }
            }
        }
    }
}
