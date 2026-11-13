package com.mindgate.presentation.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.local.PreferencesManager
import com.mindgate.data.repository.UserRepository
import com.mindgate.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _destination = MutableStateFlow<String?>(null)
    val destination = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Minimum splash duration
                val delayJob = launch { delay(2000) }
                
                val onboardingDone = prefs.onboardingDone.first()
                val userId = prefs.currentUserId.first()
                
                // Only consider user logged in if userId > 0
                val user = if (userId > 0) userRepository.getUser(userId) else null

                val dest = when {
                    !onboardingDone -> {
                        android.util.Log.d("MG_DIAG", "MG_DIAG: Splash routing decision = LANDING (onboarding not done)")
                        Screen.RoleSelection.route
                    }
                    user == null -> {
                        android.util.Log.d("MG_DIAG", "MG_DIAG: Splash routing decision = LANDING (user null)")
                        Screen.RoleSelection.route
                    }
                    else -> {
                        // Ensure Firebase is synced before moving into dashboards
                        userRepository.ensureFirebaseAuthenticated()
                        
                        when {
                            user.role == "teacher" -> {
                                android.util.Log.d("MG_DIAG", "MG_DIAG: Splash routing decision = TEACHER_DASHBOARD")
                                Screen.TeacherDashboard.route
                            }
                            user.role == "parent" -> {
                                android.util.Log.d("MG_DIAG", "MG_DIAG: Splash routing decision = PARENT_DASHBOARD")
                                Screen.ParentDashboard.route
                            }
                            user.role == "student" && !user.assessmentCompleted -> {
                                android.util.Log.d("MG_DIAG", "MG_DIAG: Splash routing decision = ASSESSMENT")
                                Screen.Assessment.route
                            }
                            user.role == "student" -> {
                                val checkInDone = prefs.checkInDone.first()
                                android.util.Log.d("MG_DIAG", "MG_DIAG: Splash routing decision = STUDENT_HOME (checkInDone=$checkInDone)")
                                if (checkInDone) Screen.Home.route else Screen.CheckIn.route
                            }
                            else -> {
                                android.util.Log.d("MG_DIAG", "MG_DIAG: Splash routing decision = FALLBACK/LANDING - raw role [${user.role}]")
                                Screen.RoleSelection.route
                            }
                        }
                    }
                }
                
                delayJob.join()
                _destination.value = dest
            } catch (e: Exception) {
                android.util.Log.e("Splash", "Error resolving destination", e)
                _destination.value = Screen.RoleSelection.route
            }
        }
    }
}
