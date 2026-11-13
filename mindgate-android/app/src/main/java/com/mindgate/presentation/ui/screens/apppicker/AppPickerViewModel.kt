package com.mindgate.presentation.ui.screens.apppicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.local.PreferencesManager
import com.mindgate.data.repository.UsageRepository
import com.mindgate.util.AppCategory
import com.mindgate.util.AppInfo
import com.mindgate.util.AppInfoHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppPickerUiState(
    val apps: List<AppInfo> = emptyList(),
    val usageMap: Map<String, Int> = emptyMap(),
    val searchQuery: String = "",
    val selectedCategory: AppCategory = AppCategory.ALL,
    val selectedPackages: Set<String> = emptySet(),
    val isLoading: Boolean = false
)

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val appInfoHelper: AppInfoHelper,
    private val usageRepository: UsageRepository,
    private val prefs: PreferencesManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _usageMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(AppCategory.ALL)
    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(false)

    val state: StateFlow<AppPickerUiState> = combine(
        combine(_allApps, _usageMap) { a, u -> a to u },
        _searchQuery,
        _selectedCategory,
        _selectedPackages,
        _isLoading
    ) { (apps, usage), query, category, selected, loading ->
        val filtered = apps.filter { app ->
            val matchesSearch = app.appName.contains(query, ignoreCase = true)
            val matchesCategory = category == AppCategory.ALL || app.category == category
            matchesSearch && matchesCategory
        }
        AppPickerUiState(
            apps = filtered,
            usageMap = usage,
            searchQuery = query,
            selectedCategory = category,
            selectedPackages = selected,
            isLoading = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPickerUiState())

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val userId = prefs.currentUserId.first()
            if (userId > 0) {
                usageRepository.syncUsageFromSystem(userId)
                val usage = entryPoint().database().usageDao().observeUsageForDate(userId, usageRepository.today()).first()
                _usageMap.value = usage.associate { it.packageName to it.usedMinutes }
            }
            _allApps.value = appInfoHelper.getInstalledApps()
            _isLoading.value = false
        }
    }

    private fun entryPoint() = dagger.hilt.android.EntryPointAccessors.fromApplication(
        context.applicationContext as android.app.Application, com.mindgate.di.MindGateEntryPoint::class.java
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryChange(category: AppCategory) {
        _selectedCategory.value = category
    }

    fun toggleSelection(packageName: String) {
        _selectedPackages.value = _selectedPackages.value.toMutableSet().apply {
            if (contains(packageName)) remove(packageName) else add(packageName)
        }
    }
}
