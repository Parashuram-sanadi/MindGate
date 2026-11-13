package com.mindgate.presentation.navigation

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.navArgument
import com.mindgate.presentation.ui.assessment.AssessmentScreen
import com.mindgate.presentation.ui.auth.AuthScreen
import com.mindgate.presentation.ui.home.HomeScreen
import com.mindgate.presentation.ui.explore.*
import com.mindgate.presentation.ui.onboarding.OnboardingScreen
import com.mindgate.presentation.ui.onboarding.RoleGateScreen
import com.mindgate.presentation.ui.parent.*
import com.mindgate.presentation.ui.profile.ProfileTabScreen
import com.mindgate.presentation.ui.result.ResultScreen
import com.mindgate.presentation.ui.role.RoleSelectionScreen
import com.mindgate.presentation.ui.settings.ConnectionSettingsScreen
import com.mindgate.presentation.ui.student.ParentConnectionScreen
import com.mindgate.util.navigation.navigateOnce
import com.mindgate.presentation.ui.screens.applimit.AppLimitSetupScreen
import com.mindgate.presentation.ui.screens.applimit.AppLimitSetupScreen
import com.mindgate.presentation.ui.screens.apppicker.AppPickerScreen
import com.mindgate.presentation.ui.screens.assessment.AssessmentChoiceScreen
import com.mindgate.presentation.ui.screens.checkin.CheckInScreen
import com.mindgate.presentation.ui.screens.setup.PermissionSetupScreen
import com.mindgate.presentation.ui.splash.SplashScreen
import com.mindgate.presentation.ui.splash.SplashViewModel
import com.mindgate.presentation.ui.teacher.TeacherDashboardScreen
import com.mindgate.presentation.ui.teacher.TeacherResultsScreen
import com.mindgate.presentation.viewmodel.TeacherViewModel
import com.mindgate.presentation.viewmodel.ExploreViewModel
import com.mindgate.presentation.ui.auth.AuthViewModel
import com.mindgate.data.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MindGateNavGraph(
    navController: NavHostController = rememberNavController(),
    sessionManager: com.mindgate.data.local.SessionManager,
    userRepository: UserRepository
) {
    val motionDuration = com.mindgate.presentation.ui.theme.MindGateMotion.DurationTransition
    val motionEasing = com.mindgate.presentation.ui.theme.MindGateMotion.EaseInOut
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        sessionManager.logoutEvents.collect {
            navController.navigate(Screen.RoleSelection.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { 
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(motionDuration, easing = motionEasing)) + 
            fadeIn(animationSpec = tween(motionDuration)) 
        },
        exitTransition = { 
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(motionDuration, easing = motionEasing)) + 
            fadeOut(animationSpec = tween(motionDuration)) 
        },
        popEnterTransition = { 
            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(motionDuration, easing = motionEasing)) + 
            fadeIn(animationSpec = tween(motionDuration)) 
        },
        popExitTransition = { 
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(motionDuration, easing = motionEasing)) + 
            fadeOut(animationSpec = tween(motionDuration)) 
        }
    ) {
        composable(Screen.Splash.route) {
            val vm: SplashViewModel = hiltViewModel()
            val destination by vm.destination.collectAsStateWithLifecycle()
            SplashScreen(onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }, destination = destination)
        }

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onStudentClick = {
                    navController.navigateOnce(Screen.Onboarding.route)
                },
                onParentClick = {
                    navController.navigateOnce("${Screen.Auth.route}?role=parent")
                },
                onSettingsClick = {
                    navController.navigateOnce(Screen.ConnectionSettings.route)
                }
            )
        }

        composable(Screen.ConnectionSettings.route) {
            ConnectionSettingsScreen(
                sessionManager = sessionManager,
                onBack = { navController.popBackStack() }
            )
        }


        composable(Screen.TeacherDashboard.route) {
            RoleGuard(navController, sessionManager, userRepository, "teacher") {
                TeacherDashboardScreen(
                    onLogout = {
                        scope.launch {
                            userRepository.logout()
                        }
                    },
                    onNavigateToResults = {
                        navController.navigate(Screen.TeacherResults.route)
                    }
                )
            }
        }

        composable(Screen.RoleGate.route) {
            RoleGateScreen(
                onRoleSelected = { role ->
                    if (role == "parent") {
                        navController.navigate(Screen.ParentDashboard.route) {
                            popUpTo(Screen.RoleGate.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Assessment.route) {
                            popUpTo(Screen.RoleGate.route) { inclusive = true }
                        }
                    }
                },
                onSettingsClick = {
                    navController.navigateOnce(Screen.ConnectionSettings.route)
                }
            )
        }
        
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onDone = {
                navController.navigateOnce(Screen.Auth.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        
        composable(
            route = "${Screen.Auth.route}?role={role}",
            arguments = listOf(navArgument("role") { defaultValue = "student" })
        ) { backStackEntry ->
            val roleParam = backStackEntry.arguments?.getString("role") ?: "student"
            val scope = rememberCoroutineScope()
            val authViewModel: AuthViewModel = hiltViewModel<AuthViewModel>()
            
            AuthScreen(
                initialRole = roleParam,
                onSuccess = { userId, userRole, needsAssessment ->
                    val normalizedRole = userRole.trim().lowercase()
                    if (roleParam == "teacher" && normalizedRole != "teacher") {
                        authViewModel.setError("This account is not a teacher account.")
                        return@AuthScreen
                    }

                    Log.d("MG_DIAG", "MG_DIAG: NavGraph read role = [$userRole], ts = [${System.currentTimeMillis()}]")
                    scope.launch {
                        val dest = when (normalizedRole) {
                            "parent" -> {
                                Log.d("MG_DIAG", "MG_DIAG: routing decision = PARENT_DASHBOARD")
                                Screen.ParentDashboard.route
                            }
                            "teacher" -> {
                                Log.d("MG_DIAG", "MG_DIAG: routing decision = TEACHER_DASHBOARD")
                                Screen.TeacherDashboard.route
                            }
                            "student" -> {
                                Log.d("MG_DIAG", "MG_DIAG: routing decision = STUDENT_DASHBOARD (needsAssessment=$needsAssessment)")
                                if (needsAssessment) Screen.AssessmentChoice.route else Screen.Home.route
                            }
                            else -> {
                                Log.d("MG_DIAG", "MG_DIAG: routing decision = FALLBACK/LANDING — reason: role [$userRole] did not match parent/teacher/student")
                                Screen.RoleSelection.route
                            }
                        }
                        Log.d("MG_DIAG", "MG_DIAG: navController.navigateOnce route = [$dest]")
                        navController.navigateOnce(dest) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onSettingsClick = {
                    navController.navigateOnce(Screen.ConnectionSettings.route)
                },
                onTeacherClick = {
                    navController.navigateOnce("${Screen.Auth.route}?role=teacher")
                },
                viewModel = authViewModel
            )
        }
        
        composable(Screen.Assessment.route) {
            RoleGuard(navController, sessionManager, userRepository, "student") {
                AssessmentScreen(
                    onComplete = {
                        navController.navigateOnce(Screen.Result.route) {
                            popUpTo(Screen.Assessment.route) { inclusive = true }
                        }
                    },
                    onProfileClick = {
                        navController.navigateOnce(Screen.Profile.route)
                    }
                )
            }
        }

        composable(Screen.AssessmentChoice.route) {
            RoleGuard(navController, sessionManager, userRepository, "student") {
                AssessmentChoiceScreen(
                    onStartAssessment = {
                        navController.navigateOnce(Screen.Assessment.route)
                    },
                    onSkip = {
                        navController.navigateOnce(Screen.Profile.route) {
                            popUpTo(Screen.AssessmentChoice.route) { inclusive = true }
                        }
                    }
                )
            }
        }
        
        composable(Screen.Result.route) {
            RoleGuard(navController, sessionManager, userRepository, "student") {
                ResultScreen(
                    onSetLimits = {
                        navController.navigateOnce(Screen.AppPicker.route)
                    },
                    onProfileClick = {
                        navController.navigateOnce(Screen.Profile.route)
                    },
                    onBackToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.AppPicker.route) {
            RoleGuard(navController, sessionManager, userRepository, "student") {
                AppPickerScreen(
                    onNavigateToLimitSetup = { pkgs ->
                        val pkgsStr = pkgs.joinToString(",")
                        navController.navigateOnce("${Screen.AppLimitSetup.route}/$pkgsStr")
                    },
                    onProfileClick = {
                        navController.navigateOnce(Screen.Profile.route)
                    }
                )
            }
        }

        composable(
            route = "${Screen.AppLimitSetup.route}/{packages}",
            arguments = listOf(navArgument("packages") { defaultValue = "" })
        ) { backStackEntry ->
            val pkgs = backStackEntry.arguments?.getString("packages")?.split(",") ?: emptyList()
            RoleGuard(navController, sessionManager, userRepository, "student") {
                AppLimitSetupScreen(
                    packageNames = pkgs,
                    onSave = {
                        navController.navigate(Screen.PermissionSetup.route)
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                    onProfileClick = {
                        navController.navigateOnce(Screen.Profile.route)
                    }
                )
            }
        }

        composable(Screen.PermissionSetup.route) {
            PermissionSetupScreen(onComplete = {
                navController.navigateOnce(Screen.Home.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }

        composable(Screen.CheckIn.route) {
            CheckInScreen(onComplete = {
                runBlocking { sessionManager.setCheckInDone(true) }
                navController.navigateOnce(Screen.Home.route) {
                    popUpTo(Screen.CheckIn.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            RoleGuard(navController, sessionManager, userRepository, "student") {
                HomeScreen(
                    onRetakeAssessment = {
                        navController.navigate(Screen.Assessment.route)
                    },
                    onLogout = {
                        navController.navigate(Screen.RoleSelection.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onAddRestriction = {
                        navController.navigateOnce(Screen.AppPicker.route)
                    },
                    onParentConnect = {
                        navController.navigateOnce(Screen.ParentConnection.route)
                    },
                    onProfileClick = {
                        navController.navigateOnce(Screen.Profile.route)
                    },
                    onExploreClick = {
                        navController.navigateOnce(Screen.Explore.route)
                    }
                )
            }
        }

        composable(Screen.Profile.route) {
            RoleGuard(navController, sessionManager, userRepository, "student") {
                ProfileTabScreen(
                    onRetakeAssessment = {
                        navController.navigate(Screen.Assessment.route)
                    },
                    onParentConnect = {
                        navController.navigateOnce(Screen.ParentConnection.route)
                    },
                    onLogout = {
                        navController.navigate(Screen.RoleSelection.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.ParentConnection.route) {
            RoleGuard(navController, sessionManager, userRepository, "student") {
                ParentConnectionScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Explore.route) {
            val exploreViewModel: ExploreViewModel = hiltViewModel()
            RoleGuard(navController, sessionManager, userRepository, "student") {
                ExploreScreen(
                    onBack = { navController.popBackStack() },
                    onPlayVideo = { video ->
                        exploreViewModel.playVideo(video)
                        navController.navigate(Screen.ExplorePlayer.route)
                    },
                    viewModel = exploreViewModel
                )
            }
        }

        composable(Screen.ExplorePlayer.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.Explore.route) }
            val exploreViewModel: ExploreViewModel = hiltViewModel(parentEntry)
            val state by exploreViewModel.uiState.collectAsStateWithLifecycle()
            
            RoleGuard(navController, sessionManager, userRepository, "student") {
                state.currentlyPlayingVideo?.let { video ->
                    ExploreVideoPlayerScreen(
                        video = video,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        composable(Screen.ParentLinkStudent.route) {
            RoleGuard(navController, sessionManager, userRepository, "parent") {
                ParentLinkStudentScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.navigate(Screen.ParentDashboard.route) {
                            popUpTo(Screen.ParentDashboard.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.ParentDashboard.route) {
            RoleGuard(navController, sessionManager, userRepository, "parent") {
                ParentDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToChild = { id -> navController.navigate("${Screen.ChildUsage.route}/$id") },
                    onNavigateToConnect = {
                        navController.navigateOnce(Screen.ParentLinkStudent.route)
                    },
                    onLogout = {
                        navController.navigate(Screen.RoleSelection.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(
            route = "${Screen.ChildUsage.route}/{studentId}",
            arguments = listOf(androidx.navigation.navArgument("studentId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val idStr = backStackEntry.arguments?.getString("studentId") ?: ""
            val id = idStr.toLongOrNull() ?: 0L
            RoleGuard(navController, sessionManager, userRepository, "parent") {
                ChildUsageDetailScreen(studentId = id, onBack = { navController.popBackStack() })
            }
        }

        composable(Screen.ParentAlerts.route) {
            RoleGuard(navController, sessionManager, userRepository, "parent") {
                ParentAlertsScreen(onBack = { navController.popBackStack() })
            }
        }

        navigation(
            startDestination = Screen.TeacherDashboard.route,
            route = "teacher_flow"
        ) {
            composable(Screen.TeacherDashboard.route) { entry ->
                val parentEntry = remember(entry) { navController.getBackStackEntry("teacher_flow") }
                val teacherViewModel: TeacherViewModel = hiltViewModel<TeacherViewModel>(parentEntry)
                
                RoleGuard(navController, sessionManager, userRepository, "teacher") {
                    TeacherDashboardScreen(
                        onLogout = {
                            scope.launch {
                                userRepository.logout()
                            }
                        },
                        onNavigateToResults = {
                            navController.navigate(Screen.TeacherResults.route)
                        },
                        viewModel = teacherViewModel
                    )
                }
            }

            composable(Screen.TeacherResults.route) { entry ->
                val parentEntry = remember(entry) { navController.getBackStackEntry("teacher_flow") }
                val teacherViewModel: TeacherViewModel = hiltViewModel<TeacherViewModel>(parentEntry)
                
                RoleGuard(navController, sessionManager, userRepository, "teacher") {
                    TeacherResultsScreen(
                        onBack = { navController.popBackStack() },
                        viewModel = teacherViewModel
                    )
                }
            }
        }
    }
}
