package com.mindgate.data.repository

import com.mindgate.data.local.PreferencesManager
import com.mindgate.data.local.SessionManager
import com.mindgate.data.local.dao.UserDao
import com.mindgate.data.local.entities.UserEntity
import com.mindgate.data.remote.api.MindGateApiService
import com.mindgate.data.remote.dto.*
import com.mindgate.data.remote.util.NetworkResult
import com.mindgate.domain.model.UserModel
import com.mindgate.util.PasswordHasher
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

sealed class GoogleAuthResult {
    data class Success(val userId: Long) : GoogleAuthResult()
    data class RoleRequired(val email: String) : GoogleAuthResult()
}

@Singleton
class UserRepository @Inject constructor(
    private val api: MindGateApiService,
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val prefs: PreferencesManager,
    private val parentRepository: dagger.Lazy<ParentRepository>
) {
    val currentUserId: Flow<Long> = prefs.currentUserId

    suspend fun register(
        name: String,
        email: String,
        password: String,
        yearOfStudy: Int,
        gender: String,
        role: String = "student"
    ): Result<AuthResponse> {
        try {
            if (userDao.getByEmail(email.trim().lowercase()) != null) {
                return Result.failure(Exception("Email already registered locally"))
            }

            // 1. Remote Register
            val response = api.register(RegisterRequest(name, email, password, role))
            Log.d("MG_DIAG", "MG_DIAG: raw registration response body = [${response.body()}]")
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                val serverMsg = try {
                    val json = com.google.gson.JsonParser().parse(errorBody).asJsonObject
                    json.get("detail").asString
                } catch (parseEx: Exception) {
                    response.message()
                }
                return Result.failure(Exception("Server registration failed: $serverMsg"))
            }
            val remoteId = response.body()?.id ?: -1L

            // 2. Local Sync metadata
            userDao.insert(
                UserEntity(
                    id = if (remoteId > 0) remoteId else 0,
                    name = name.trim(),
                    email = email.trim().lowercase(),
                    passwordHash = PasswordHasher.hash(password),
                    yearOfStudy = yearOfStudy,
                    gender = gender,
                    role = role,
                    roleSelected = true
                )
            )

            // 3. Login to get token and sync full session
            return login(email, password)
        } catch (e: Exception) {
            val currentUrl = runBlocking { sessionManager.apiBaseUrl.first() }
            return Result.failure(Exception("Network error while connecting to $currentUrl: ${e.message}"))
        }
    }

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        try {
            // 1. Remote Login
            val response = api.login(email.trim().lowercase(), password)
            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null) {
                    return handleBackendLoginSuccess(authResponse, password)
                }
                return Result.failure(Exception("Empty response from server"))
            } else {
                val errorBody = response.errorBody()?.string()
                val serverMsg = try {
                    val json = com.google.gson.JsonParser().parse(errorBody).asJsonObject
                    json.get("detail").asString
                } catch (parseEx: Exception) {
                    response.message()
                }
                return Result.failure(Exception("Server login failed: $serverMsg"))
            }
        } catch (e: Exception) {
            val currentUrl = runBlocking { sessionManager.apiBaseUrl.first() }
            return Result.failure(Exception("Network error while connecting to $currentUrl: ${e.message}"))
        }
    }

    suspend fun googleAuth(idToken: String): Result<GoogleAuthResult> {
        return try {
            val response = api.googleAuth(GoogleAuthRequest(idToken))
            if (response.isSuccessful) {
                val body = response.body()!!
                if (body.has("status") && body.get("status").asString == "role_required") {
                    Result.success(GoogleAuthResult.RoleRequired(body.get("email").asString))
                } else {
                    val authResponse = com.google.gson.Gson().fromJson(body, AuthResponse::class.java)
                    val loginResult = handleBackendLoginSuccess(authResponse, null)
                    return loginResult.map { GoogleAuthResult.Success(it.userId) }
                }
            } else {
                Result.failure(Exception("Google auth failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun googleAuthComplete(idToken: String, role: String): Result<AuthResponse> {
        return try {
            val response = api.googleAuthComplete(GoogleAuthCompleteRequest(idToken, role))
            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null) {
                    return handleBackendLoginSuccess(authResponse, null)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                Result.failure(Exception("Registration failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshFirebaseSession(): Result<Unit> {
        return try {
            val response = api.getFirebaseToken()
            if (response.isSuccessful) {
                val token = response.body()?.get("firebase_token")
                ensureFirebaseAuthenticated(token)
            } else {
                Result.failure(Exception("Failed to get fresh Firebase token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ensureFirebaseAuthenticated(customToken: String? = null): Result<Unit> {
        val auth = FirebaseAuth.getInstance()
        
        // If we have a token, we MUST use it to align with backend ID
        if (customToken != null) {
            return try {
                Log.d("MINDGATE_REPO", "Signing into Firebase with Custom Token")
                auth.signInWithCustomToken(customToken).await()
                Log.i("MINDGATE_REPO", "Firebase session aligned. UID: ${auth.currentUser?.uid}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("MINDGATE_REPO", "Custom Token sign-in failed", e)
                Result.failure(e)
            }
        }

        if (auth.currentUser != null) return Result.success(Unit)

        // FALLBACK REMOVED per Requirement 1: Direct UID only.
        // If no token and no user, we are not authenticated.
        return Result.failure(Exception("No Firebase credentials available"))
    }

    private suspend fun handleBackendLoginSuccess(authResponse: AuthResponse, password: String?): Result<AuthResponse> {
        Log.d("MG_DIAG", "MG_DIAG: role from API = [${authResponse.role}]")
        
        // 1. Establish Session IMMEDIATELY
        val userId = authResponse.userId
        val role = authResponse.role.trim().lowercase()
        
        prefs.setCurrentUserId(userId)
        sessionManager.saveUserRole(role)
        sessionManager.saveAuthToken(authResponse.accessToken)
        
        Log.d("MG_DIAG", "MG_DIAG: session persistence completed")
        
        // 2. Sync User to Local DB
        val existingUser = userDao.getById(userId)
        userDao.insert(
            UserEntity(
                id = userId,
                name = authResponse.name,
                email = authResponse.email,
                passwordHash = if (password != null) PasswordHasher.hash(password) else (existingUser?.passwordHash ?: ""),
                yearOfStudy = existingUser?.yearOfStudy ?: 1,
                gender = existingUser?.gender ?: "Unknown",
                role = role,
                roleSelected = true,
                assessmentCompleted = existingUser?.assessmentCompleted ?: false
            )
        )

        // 3. Ensure Firebase Auth is active and ALIGNED with backend
        ensureFirebaseAuthenticated(authResponse.firebaseToken)
        
        // 4. Update device token and link information
        updateFirebaseState(userId, role)

        return Result.success(authResponse)
    }

    private suspend fun updateFirebaseState(userId: Long, role: String) {
        val auth = FirebaseAuth.getInstance()
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val currentUid = auth.currentUser?.uid ?: return

        // Store device token for FCM
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()?.let { token ->
            val tokenData = mapOf(
                "fcmToken" to token,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "backendUserId" to userId,
                "role" to role
            )
            firestore.collection("deviceTokens").document(currentUid).set(tokenData)
        }
    }

    suspend fun getUser(id: Long): UserModel? {
        val user = userDao.getById(id)?.toModel()
        if (com.mindgate.BuildConfig.DEBUG) android.util.Log.d("MINDGATE_REPO", "getUserById($id) returned: $user")
        return user
    }

    suspend fun getFirstUser(): UserModel? = userDao.getFirstUser()?.toModel()

    suspend fun hasUsers(): Boolean = userDao.countUsers() > 0

    suspend fun logout() {
        sessionManager.clearSession()
        
        // Sign out from Firebase
        try {
            FirebaseAuth.getInstance().signOut()
            Log.d("MINDGATE_FIREBASE_AUTH", "Firebase signed out successfully")
        } catch (e: Exception) {
            Log.e("MINDGATE_FIREBASE_AUTH", "Error during Firebase sign-out: ${e.message}")
        }
    }

    private fun UserEntity.toModel() = UserModel(
        id = id,
        name = name,
        email = email,
        yearOfStudy = yearOfStudy,
        gender = gender,
        role = role,
        roleSelected = roleSelected,
        assessmentCompleted = assessmentCompleted
    )
}
