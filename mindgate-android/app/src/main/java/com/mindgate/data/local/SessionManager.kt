package com.mindgate.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mindgate.util.MindGateLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore("mindgate_prefs")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore = context.sessionDataStore

    companion object {
        val USER_ID_KEY = longPreferencesKey("user_id")
        /** Legacy key from earlier builds — read for migration, write only to [USER_ID_KEY]. */
        val LEGACY_USER_ID_KEY = longPreferencesKey("current_user_id")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val ASSESSMENT_STEP = intPreferencesKey("assessment_step")
        val ASSESSMENT_ANSWERS = stringPreferencesKey("assessment_answers_json")
        val LAST_QUIZ_TYPE = stringPreferencesKey("last_quiz_type")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val CHECK_IN_DONE = booleanPreferencesKey("check_in_done")
        val IS_LINKED = booleanPreferencesKey("is_linked")
        val LINKED_PARENT_UID = stringPreferencesKey("linked_parent_uid")
        val ROLE = stringPreferencesKey("role")
    }

    val checkInDone: Flow<Boolean> = dataStore.data.map { it[CHECK_IN_DONE] ?: false }

    suspend fun setCheckInDone(done: Boolean) {
        dataStore.edit { it[CHECK_IN_DONE] = done }
    }

    val isLinked: Flow<Boolean> = dataStore.data.map { it[IS_LINKED] ?: false }

    suspend fun setLinked(linked: Boolean) {
        dataStore.edit { it[IS_LINKED] = linked }
        MindGateLog.d("SESSION", "setLinked called with: $linked")
    }

    val linkedParentUid: Flow<String?> = dataStore.data.map { it[LINKED_PARENT_UID] }

    suspend fun saveParentUid(uid: String?) {
        dataStore.edit {
            if (uid != null) it[LINKED_PARENT_UID] = uid
            else it.remove(LINKED_PARENT_UID)
        }
        MindGateLog.d("SESSION", "saveParentUid called with: $uid")
    }

    val userRole: Flow<String?> = dataStore.data.map { it[ROLE] }

    suspend fun saveUserRole(role: String) {
        dataStore.edit { it[ROLE] = role }
        MindGateLog.d("SESSION", "saveUserRole called with: $role")
    }

    val apiBaseUrl: Flow<String> = dataStore.data.map { 
        it[API_BASE_URL] ?: com.mindgate.BuildConfig.API_BASE_URL 
    }

    suspend fun saveApiBaseUrl(url: String) {
        dataStore.edit { it[API_BASE_URL] = url }
    }

    val authToken: Flow<String?> = dataStore.data.map { it[AUTH_TOKEN] }

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { it[AUTH_TOKEN] = token }
    }

    val currentUserId: Flow<Long> = dataStore.data.map {
        val userId = it[USER_ID_KEY] ?: it[LEGACY_USER_ID_KEY] ?: -1L
        android.util.Log.d("MG_DIAG", "MG_DIAG: SessionManager.currentUserId flow emitting = [$userId]")
        userId
    }

    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    val assessmentStep: Flow<Int> = dataStore.data.map { it[ASSESSMENT_STEP] ?: 0 }

    val assessmentAnswersJson: Flow<String> = dataStore.data.map { it[ASSESSMENT_ANSWERS] ?: "{}" }

    private val _logoutEvents = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutEvents = _logoutEvents.asSharedFlow()

    fun isLoggedIn(): Flow<Boolean> = dataStore.data.map {
        (it[USER_ID_KEY] ?: it[LEGACY_USER_ID_KEY] ?: -1L) > 0L && !it[AUTH_TOKEN].isNullOrBlank()
    }

    suspend fun saveUserId(id: Long) {
        dataStore.edit {
            if (id > 0) {
                it[USER_ID_KEY] = id
            } else {
                it.remove(USER_ID_KEY)
            }
            it.remove(LEGACY_USER_ID_KEY)
        }
        MindGateLog.d("SESSION", "Updated userId in DataStore: $id")
    }

    suspend fun getUserId(): Long {
        val userId = dataStore.data.map {
            it[USER_ID_KEY] ?: it[LEGACY_USER_ID_KEY] ?: -1L
        }.first()
        MindGateLog.d("SESSION", "Read userId from DataStore: $userId")
        return userId
    }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[ONBOARDING_DONE] = done }
    }

    suspend fun saveAssessmentProgress(step: Int, answersJson: String) {
        dataStore.edit {
            it[ASSESSMENT_STEP] = step
            it[ASSESSMENT_ANSWERS] = answersJson
        }
    }

    suspend fun clearAssessmentProgress() {
        dataStore.edit {
            it.remove(ASSESSMENT_STEP)
            it.remove(ASSESSMENT_ANSWERS)
        }
    }

    suspend fun setLastQuizType(type: String) {
        dataStore.edit { it[LAST_QUIZ_TYPE] = type }
    }

    suspend fun getLastQuizType(): String? =
        dataStore.data.map { it[LAST_QUIZ_TYPE] }.first()

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(USER_ID_KEY)
            prefs.remove(LEGACY_USER_ID_KEY)
            prefs.remove(AUTH_TOKEN)
            prefs.remove(IS_LINKED)
            prefs.remove(LINKED_PARENT_UID)
            prefs.remove(ROLE)
        }
        _logoutEvents.emit(Unit)
        MindGateLog.d("SESSION", "Session cleared")
    }
}
