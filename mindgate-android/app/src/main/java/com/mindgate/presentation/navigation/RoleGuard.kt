package com.mindgate.presentation.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.mindgate.data.local.SessionManager
import com.mindgate.data.repository.UserRepository
import kotlinx.coroutines.flow.first

@Composable
fun RoleGuard(
    navController: NavController,
    sessionManager: SessionManager,
    userRepository: UserRepository,
    requiredRole: String,
    content: @Composable () -> Unit
) {
    var authorized by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        sessionManager.currentUserId.collect { userId ->
            if (userId > 0) {
                // Fetch user and wait if null (database might still be writing after login)
                var user = userRepository.getUser(userId)
                if (user == null) {
                    kotlinx.coroutines.delay(500)
                    user = userRepository.getUser(userId)
                }

                if (user != null) {
                    val userRole = user.role.trim().lowercase()
                    val targetRole = requiredRole.trim().lowercase()
                    
                    Log.d("MG_DIAG", "RoleGuard: userId=$userId, role=$userRole, required=$targetRole")
                    
                    if (userRole == targetRole) {
                        authorized = true
                    } else {
                        Log.w("MG_DIAG", "RoleGuard: Mismatch! Redirecting. userRole=$userRole != target=$targetRole")
                        authorized = false
                        navController.navigate(Screen.RoleSelection.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                } else {
                    Log.e("MG_DIAG", "RoleGuard: User $userId exists in session but not in local DB. Waiting...")
                    // Keep authorized as null to show loading
                }
            } else if (userId == -1L && authorized == null) {
                // Truly not logged in
                Log.d("MG_DIAG", "RoleGuard: Not logged in. Redirecting to RoleSelection.")
                authorized = false
                navController.navigate(Screen.RoleSelection.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    when (authorized) {
        true -> content()
        null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {} // Navigation or Mismatch
    }
}
