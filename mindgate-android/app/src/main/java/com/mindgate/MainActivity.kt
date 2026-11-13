package com.mindgate

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import com.mindgate.presentation.navigation.Screen
import com.mindgate.data.remote.api.MindGateApiService
import com.mindgate.presentation.navigation.MindGateNavGraph
import com.mindgate.presentation.ui.theme.MindGateTheme
import com.mindgate.util.FirebaseConnectionVerifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var apiService: MindGateApiService

    @Inject
    lateinit var sessionManager: com.mindgate.data.local.SessionManager

    @Inject
    lateinit var userRepository: com.mindgate.data.repository.UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 1. Update Device Token if authenticated
        lifecycleScope.launch {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            
            auth.currentUser?.uid?.let { uid ->
                // Start Parent Watchdog if user is a parent
                lifecycleScope.launch {
                    val role = sessionManager.userRole.first()
                    if (role == "parent") {
                        android.util.Log.d("MG_STARTUP", "Parent detected, starting Approval Watchdog Service")
                        val serviceIntent = Intent(this@MainActivity, com.mindgate.parent.ParentApprovalListenerService::class.java)
                        startForegroundService(serviceIntent)
                    }
                }

                try {
                    val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    firestore.collection("deviceTokens").document(uid).set(
                        mapOf(
                            "fcmToken" to token,
                            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                    ).await()
                    Log.d("FCM", "Device token synced for UID: $uid")
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to sync device token", e)
                }
            }
        }

        // ============================================================
        // MINDGATE / FIREBASE DEBUG START
        // ============================================================

        Log.e(
            "FirebaseVerifier",
            "================================================"
        )

        Log.e(
            "FirebaseVerifier",
            "===== MINDGATE MAINACTIVITY STARTED ====="
        )

        Log.e(
            "FirebaseVerifier",
            "Application package: $packageName"
        )

        Log.e(
            "FirebaseVerifier",
            "onCreate() reached successfully"
        )

        // ============================================================
        // FIREBASE CONNECTION VERIFICATION
        // ============================================================

        Log.e(
            "FirebaseVerifier",
            "ABOUT_TO_START_VERIFIER"
        )

        lifecycleScope.launch {

            try {

                Log.e(
                    "FirebaseVerifier",
                    "FirebaseConnectionVerifier.runTest() STARTED"
                )

                FirebaseConnectionVerifier.runTest()
                
                // CRITICAL FIX: For returning users, ensure we align Firebase session
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val currentUid = auth.currentUser?.uid
                val backendId = sessionManager.getUserId().toString()
                
                if (backendId != "-1" && (currentUid == null || currentUid != backendId)) {
                    Log.i("MINDGATE_AUTH", "Firebase session mismatch for returning user. backend=$backendId, firebase=$currentUid. Re-aligning...")
                    lifecycleScope.launch {
                        userRepository.refreshFirebaseSession()
                    }
                }

                Log.e(
                    "FirebaseVerifier",
                    "FirebaseConnectionVerifier.runTest() RETURNED SUCCESSFULLY"
                )

            } catch (e: Exception) {

                Log.e(
                    "FirebaseVerifier",
                    "FirebaseConnectionVerifier FAILED",
                    e
                )
            }
        }

        Log.e(
            "FirebaseVerifier",
            "Firebase verifier coroutine launched"
        )

        // ============================================================
        // BACKEND API HEALTH CHECK
        // ============================================================

        lifecycleScope.launch {

            try {

                val response = apiService.health()

                if (response.isSuccessful) {

                    Log.d(
                        "MINDGATE_HEALTH",
                        "API online: ${response.body()}"
                    )

                } else {

                    Log.e(
                        "MINDGATE_HEALTH",
                        "API returned error: ${response.code()}"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "MINDGATE_HEALTH",
                    "API unreachable",
                    e
                )
            }
        }

        // ============================================================
        // UI
        // ============================================================

        enableEdgeToEdge()

        setContent {

            MindGateTheme {

                val navController = rememberNavController()

                // Handle deep links from notifications
                // when the application is already running.
                LaunchedEffect(intent) {
                    val navigateTo = intent.getStringExtra("navigate_to")
                    val studentId = intent.getStringExtra("student_id")

                    if (navigateTo == "parent_dashboard") {
                        navController.navigate(Screen.ParentDashboard.route)
                    } else if (studentId != null) {
                        // Synthetic backstack:
                        // Dashboard -> Child Usage Detail
                        navController.navigate(Screen.ParentDashboard.route)
                        navController.navigate("${Screen.ChildUsage.route}/$studentId")
                    }
                }

                MindGateNavGraph(
                    navController = navController,
                    sessionManager = sessionManager,
                    userRepository = userRepository
                )
            }
        }
    }
}