package com.mindgate.presentation.ui.applimit

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.local.PreferencesManager
import com.mindgate.data.local.entities.AppLimitEntity
import com.mindgate.data.local.entities.UserSettingsEntity
import com.mindgate.data.repository.AssessmentRepository
import com.mindgate.data.repository.UsageRepository
import com.mindgate.util.AppInfoHelper
import com.mindgate.util.PermissionHelper
import com.mindgate.util.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppLimitUiItem(
    val packageName: String,
    val appName: String,
    val limitMinutes: Int,
    val suggested: Int,
    val enabled: Boolean,
    val usedMinutes: Int = 0,
    val isBlocked: Boolean = false,
    val extensionUntil: Long = 0L,
    val isCustomSchedule: Boolean = false,
    val monLimit: Int? = null,
    val tueLimit: Int? = null,
    val wedLimit: Int? = null,
    val thuLimit: Int? = null,
    val friLimit: Int? = null,
    val satLimit: Int? = null,
    val sunLimit: Int? = null
)

data class AppLimitUiState(
    val items: List<AppLimitUiItem> = emptyList(),
    val quietStart: String = "22:00",
    val quietEnd: String = "07:00",
    val studyMode: Boolean = false,
    val userId: Long = -1L,
    val selectedForEdit: AppLimitEntity? = null
)

@HiltViewModel
class AppLimitViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val assessmentRepository: AssessmentRepository,
    private val prefs: PreferencesManager,
    private val permissionHelper: PermissionHelper,
    private val usageStatsHelper: UsageStatsHelper,
    private val appInfoHelper: AppInfoHelper
) : ViewModel() {

    private val _state = MutableStateFlow(AppLimitUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val userId = prefs.currentUserId.first()
            val risk = assessmentRepository.getLatestResult(userId)?.riskLevel ?: "Moderate"
            val suggested = usageRepository.suggestedLimitForRisk(risk)
            
            val installedApps = appInfoHelper.getInstalledApps()
            
            // Collect flow once to get current usage
            usageRepository.observeLimitsWithUsage(userId).first().let { currentLimits ->
                val items = installedApps.map { app ->
                    val saved = currentLimits.find { it.packageName == app.packageName }
                    AppLimitUiItem(
                        packageName = app.packageName,
                        appName = app.appName,
                        limitMinutes = saved?.dailyLimitMinutes ?: suggested,
                        suggested = suggested,
                        enabled = saved?.isEnabled ?: false,
                        usedMinutes = saved?.usedMinutes ?: 0,
                        isBlocked = saved?.isBlocked ?: false,
                        extensionUntil = 0L, // Handled by repository
                        isCustomSchedule = saved?.isCustomSchedule ?: false
                    )
                }

                val settings = usageRepository.getSettings(userId)
                _state.value = _state.value.copy(
                    userId = userId,
                    items = items,
                    quietStart = settings.quietHoursStart,
                    quietEnd = settings.quietHoursEnd,
                    studyMode = settings.studyModeEnabled
                )
            }
        }
    }

    fun updateLimit(pkg: String, minutes: Int) {
        _state.value = _state.value.copy(
            items = _state.value.items.map {
                if (it.packageName == pkg) it.copy(limitMinutes = minutes) else it
            }
        )
    }

    fun toggleApp(pkg: String) {
        _state.value = _state.value.copy(
            items = _state.value.items.map {
                if (it.packageName == pkg) it.copy(enabled = !it.enabled) else it
            }
        )
    }

    fun resetSuggested() {
        _state.value = _state.value.copy(
            items = _state.value.items.map { it.copy(limitMinutes = it.suggested) }
        )
    }

    fun setQuietHours(start: String, end: String) {
        _state.value = _state.value.copy(quietStart = start, quietEnd = end)
    }

    fun setStudyMode(enabled: Boolean) {
        _state.value = _state.value.copy(studyMode = enabled)
    }

    fun updateFullLimit(limit: AppLimitEntity) {
        viewModelScope.launch {
            usageRepository.updateLimit(limit)
            // Re-check blocking status immediately
            usageRepository.checkAndBlockExceeded(limit.userId)
            _state.value = _state.value.copy(selectedForEdit = null)
            load()
        }
    }

    fun selectForEdit(pkg: String) {
        viewModelScope.launch {
            val limit = usageRepository.getLimit(_state.value.userId, pkg)
            _state.value = _state.value.copy(selectedForEdit = limit)
        }
    }

    fun clearEdit() {
        _state.value = _state.value.copy(selectedForEdit = null)
    }

    fun usageStatsIntent(): Intent = usageStatsHelper.openUsageAccessSettings()
    fun overlayIntent(): Intent = permissionHelper.overlaySettingsIntent()
    fun accessibilityIntent(): Intent = permissionHelper.accessibilitySettingsIntent()

    fun activate(onDone: () -> Unit) {
        viewModelScope.launch {
            val s = _state.value
            usageRepository.saveLimits(
                s.userId,
                s.items.map {
                    AppLimitEntity(
                        userId = s.userId,
                        packageName = it.packageName,
                        appName = it.appName,
                        dailyLimitMinutes = it.limitMinutes,
                        isEnabled = it.enabled,
                        suggestedLimit = it.suggested,
                        isBlocked = it.isBlocked,
                        extensionUntil = it.extensionUntil,
                        isCustomSchedule = it.isCustomSchedule,
                        monLimit = it.monLimit,
                        tueLimit = it.tueLimit,
                        wedLimit = it.wedLimit,
                        thuLimit = it.thuLimit,
                        friLimit = it.friLimit,
                        satLimit = it.satLimit,
                        sunLimit = it.sunLimit
                    )
                }
            )
            usageRepository.saveSettings(
                UserSettingsEntity(
                    userId = s.userId,
                    quietHoursStart = s.quietStart,
                    quietHoursEnd = s.quietEnd,
                    studyModeEnabled = s.studyMode,
                    limitsActive = true
                )
            )
            onDone()
        }
    }
}
