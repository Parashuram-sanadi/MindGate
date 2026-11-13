package com.mindgate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.local.entities.ParentChildRelationEntity
import com.mindgate.data.remote.util.NetworkResult
import com.mindgate.data.repository.FirestoreRepository
import com.mindgate.data.repository.ParentRepository
import com.mindgate.data.repository.UserRepository
import com.mindgate.domain.model.ExtensionRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val repository: ParentRepository,
    private val userRepository: UserRepository,
    private val firestoreRepository: FirestoreRepository,
    private val parentServiceManager: com.mindgate.parent.manager.ParentServiceManager
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<ExtensionRequest>>(emptyList())
    val pendingRequests = _pendingRequests.asStateFlow()

    private val _recentActivity = MutableStateFlow<List<ExtensionRequest>>(emptyList())
    val recentActivity = _recentActivity.asStateFlow()

    val children = repository.children.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        refresh()
        observeRequests()
        observeActivity()
    }

    private fun observeRequests() {
        viewModelScope.launch {
            // Re-attach listener if auth state changes
            callbackFlow {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { 
                    trySend(it.currentUser?.uid)
                }
                auth.addAuthStateListener(listener)
                awaitClose { auth.removeAuthStateListener(listener) }
            }
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { uid ->
                android.util.Log.e("MG_PARENT", "LISTENING ON PARENT UID: $uid")
                firestoreRepository.observePendingRequestsForParent(uid)
            }
            .collect { requests ->
                android.util.Log.i("MG_PARENT", "UPDATED: Found ${requests.size} pending requests")
                _pendingRequests.value = requests
            }
        }
    }

    private fun observeActivity() {
        viewModelScope.launch {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid
            if (uid != null) {
                firestoreRepository.observeRecentActivityForParent(uid)
                    .collect { activity ->
                        _recentActivity.value = activity
                    }
            }
        }
    }

    fun approveRequest(requestId: String, minutes: Int = 15) {
        viewModelScope.launch {
            firestoreRepository.updateRequestStatus(requestId, "APPROVED", minutes)
        }
    }

    fun denyRequest(requestId: String) {
        viewModelScope.launch {
            firestoreRepository.updateRequestStatus(requestId, "DENIED")
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            
            // 1. Sync Children from SQL
            val result = repository.syncChildren()
            if (result is NetworkResult.Error) {
                _error.value = result.errorDisplayMessage()
            }
            
            // 2. Fallback sync for pending requests from Firestore
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            auth.currentUser?.uid?.let { uid ->
                val requests = firestoreRepository.getPendingRequestsForParent(uid)
                if (requests.isNotEmpty()) {
                    _pendingRequests.value = requests
                }
            }

            _isRefreshing.value = false
        }
    }

    fun revoke(studentId: Long) {
        viewModelScope.launch {
            repository.revokeLink(studentId)
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            parentServiceManager.stopService()
            userRepository.logout()
            onComplete()
        }
    }
}
