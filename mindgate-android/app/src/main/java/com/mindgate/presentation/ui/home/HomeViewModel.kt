package com.mindgate.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.local.PreferencesManager
import com.mindgate.data.repository.AssessmentRepository
import com.mindgate.data.repository.UsageRepository
import com.mindgate.data.repository.UserRepository
import com.mindgate.data.repository.ParentRepository
import com.mindgate.data.remote.util.NetworkResult
import com.mindgate.domain.manager.ExtensionManager
import com.mindgate.domain.model.AppLimitModel
import com.mindgate.domain.model.RiskResult
import com.mindgate.domain.model.ExtensionRequest
import com.mindgate.domain.usecase.GetUsageStatsUseCase
import com.mindgate.data.repository.FirestoreRepository
import com.mindgate.util.AppInfoHelper
import com.mindgate.util.MindGateLog
import com.mindgate.util.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val usedSeconds: Long
)

data class DashboardUiState(
    val userName: String = "",
    val risk: RiskResult? = null,
    val history: List<RiskResult> = emptyList(),
    val limits: List<AppLimitModel> = emptyList(),
    val totalScreenTimeSeconds: Long = 0L,
    val topAppsUsage: List<AppUsageItem> = emptyList(),
    val totalUsedMinutes: Int = 0,
    val totalReclaimedMinutes: Int = 0,
    val streak: Int = 0,
    val isGlobalEnabled: Boolean = true,
    val isLinked: Boolean = false,
    val isLoading: Boolean = false,
    val pendingRequest: ExtensionRequest? = null,
    val requestStatus: String? = null, // PENDING, APPROVED, DENIED
    val hasUsagePermission: Boolean = true,
    val hasUsageData: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val assessmentRepository: AssessmentRepository,
    private val usageRepository: UsageRepository,
    private val parentRepository: ParentRepository,
    private val firestoreRepository: FirestoreRepository,
    private val extensionManager: ExtensionManager,
    private val getUsageStats: GetUsageStatsUseCase,
    private val usageStatsHelper: UsageStatsHelper,
    private val appInfoHelper: AppInfoHelper,
    private val sessionManager: com.mindgate.data.local.SessionManager,
    private val parentServiceManager: com.mindgate.parent.manager.ParentServiceManager,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state = _state.asStateFlow()

    private var requestJob: Job? = null

    init {
        viewModelScope.launch {
            // Force a one-time link sync at app open
            extensionManager.syncLinkStatus()

            // Auto-refresh usage stats every 30 seconds while HomeViewModel is active
            launch {
                while (isActive) {
                    val userId = prefs.currentUserId.first()
                    if (userId > 0) {
                        checkUsagePermissionAndRefreshInternal(userId)
                    }
                    delay(30_000L)
                }
            }
            
            prefs.currentUserId.collectLatest { userId ->
                if (userId <= 0) return@collectLatest
                
                _state.update { it.copy(isLoading = true) }
                
                checkUsagePermissionAndRefreshInternal(userId)
                
                val user = userRepository.getUser(userId)
                val streak = usageRepository.getStreak(userId).currentStreak
                val settings = usageRepository.getSettings(userId)
                
                // Check connection status
                val connectionResult = parentRepository.checkConnectionStatus()
                val isLinked = connectionResult is NetworkResult.Success && connectionResult.data.isLinked

                combine(
                    assessmentRepository.observeLatestResult(userId),
                    assessmentRepository.observeHistory(userId),
                    usageRepository.observeLimitsWithUsage(userId),
                    sessionManager.isLinked
                ) { risk, history, limits, isParentLinked ->
                    val hasPermission = usageStatsHelper.hasUsageStatsPermission()
                    if (!hasPermission) {
                        MindGateLog.d("USAGE_ACCESS", "Usage Access permission is OFF.")
                        _state.update {
                            it.copy(
                                userName = user?.name ?: "Student",
                                risk = risk,
                                history = history,
                                limits = limits,
                                hasUsagePermission = false,
                                hasUsageData = false,
                                totalScreenTimeSeconds = 0L,
                                topAppsUsage = emptyList(),
                                streak = streak,
                                isGlobalEnabled = settings.limitsActive,
                                isLinked = isLinked || isParentLinked,
                                isLoading = false
                            )
                        }
                    } else {
                        val usageSecondsMap = usageStatsHelper.getTodayUsageSecondsByPackage()
                        val totalSeconds = usageStatsHelper.getTodayTotalScreenSeconds()
                        val hasData = usageSecondsMap.isNotEmpty() && totalSeconds > 0L

                        val topApps = usageSecondsMap.entries
                            .filter { it.value >= 60L } // Skip apps under 1 minute (60s)
                            .sortedByDescending { it.value }
                            .take(8)
                            .map { (pkg, sec) ->
                                AppUsageItem(
                                    packageName = pkg,
                                    appName = appInfoHelper.getAppName(pkg),
                                    usedSeconds = sec
                                )
                            }

                        val reclaimed = limits.sumOf { 
                            if (it.usedMinutes < it.dailyLimitMinutes) it.dailyLimitMinutes - it.usedMinutes else 0 
                        }

                        _state.update {
                            it.copy(
                                userName = user?.name ?: "Student",
                                risk = risk,
                                history = history,
                                limits = limits,
                                hasUsagePermission = true,
                                hasUsageData = hasData,
                                totalScreenTimeSeconds = totalSeconds,
                                topAppsUsage = topApps,
                                totalUsedMinutes = limits.sumOf { l -> l.usedMinutes },
                                totalReclaimedMinutes = reclaimed,
                                streak = streak,
                                isGlobalEnabled = settings.limitsActive,
                                isLinked = isLinked || isParentLinked,
                                isLoading = false
                            )
                        }
                    }
                }.collect()
            }
        }
    }

    fun checkUsagePermissionAndRefresh() {
        viewModelScope.launch {
            val userId = prefs.currentUserId.first()
            if (userId > 0) {
                checkUsagePermissionAndRefreshInternal(userId)
            }
        }
    }

    private suspend fun checkUsagePermissionAndRefreshInternal(userId: Long) {
        val hasPermission = usageStatsHelper.hasUsageStatsPermission()
        if (!hasPermission) {
            MindGateLog.d("USAGE_ACCESS", "Usage Access permission missing, skipping calculation.")
            _state.update {
                it.copy(
                    hasUsagePermission = false,
                    hasUsageData = false,
                    totalScreenTimeSeconds = 0L,
                    topAppsUsage = emptyList(),
                    isLoading = false
                )
            }
        } else {
            getUsageStats(userId)
        }
    }

    fun syncConnection() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            extensionManager.syncLinkStatus()
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun requestMoreTime() {
        viewModelScope.launch {
            val userId = prefs.currentUserId.first()
            if (userId <= 0) return@launch

            val parentUid = sessionManager.linkedParentUid.first()
            val firebaseUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (parentUid != null && parentUid.isNotBlank() && firebaseUid != null) {
                val user = userRepository.getUser(userId)
                val result = firestoreRepository.createExtensionRequest(
                    studentUid = firebaseUid,
                    parentUid = parentUid,
                    appPackage = "manual_request", // placeholder for manual request
                    studentName = user?.name ?: "Student"
                )
                
                result.onSuccess { docId ->
                    observeRequest(docId)
                    parentRepository.notifyParentOfRequest("Manual Request")
                }.onFailure { e ->
                    MindGateLog.e("HOME", "Manual Firestore request failed.", e)
                }
            } else {
                // Fallback check
                extensionManager.syncLinkStatus()
                val freshUid = sessionManager.linkedParentUid.first()
                if (freshUid != null && freshUid.isNotBlank()) {
                    requestMoreTime()
                } else {
                    MindGateLog.e("HOME", "Manual request failed: No linked parent found.")
                }
            }
        }
    }

    private fun observeRequest(requestId: String) {
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            firestoreRepository.observeRequestStatus(requestId).collect { request ->
                _state.update { 
                    it.copy(
                        pendingRequest = request,
                        requestStatus = request?.status
                    )
                }
            }
        }
    }

    fun setGlobalEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val userId = prefs.currentUserId.first()
            if (userId <= 0) return@launch
            val settings = usageRepository.getSettings(userId)
            usageRepository.saveSettings(settings.copy(limitsActive = enabled))
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val userId = prefs.currentUserId.first()
            if (userId > 0) checkUsagePermissionAndRefreshInternal(userId)
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
