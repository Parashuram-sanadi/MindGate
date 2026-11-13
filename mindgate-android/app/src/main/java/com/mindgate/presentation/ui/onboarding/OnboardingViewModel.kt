package com.mindgate.presentation.ui.onboarding

import androidx.lifecycle.ViewModel
import com.mindgate.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {
    suspend fun completeOnboarding() = prefs.setOnboardingDone(true)
}
