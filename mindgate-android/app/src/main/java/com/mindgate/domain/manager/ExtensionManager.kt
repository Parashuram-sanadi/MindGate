package com.mindgate.domain.manager

import com.google.firebase.auth.FirebaseAuth
import com.mindgate.data.local.SessionManager
import com.mindgate.data.local.dao.AppLimitDao
import com.mindgate.data.local.dao.BehaviorSignalDao
import com.mindgate.data.local.dao.ExtensionGrantDao
import com.mindgate.data.local.entities.ExtensionGrant
import com.mindgate.data.repository.AssessmentRepository
import com.mindgate.data.repository.ExtensionRepository
import com.mindgate.data.repository.FirestoreRepository
import com.mindgate.data.repository.ParentRepository
import com.mindgate.data.remote.util.NetworkResult
import com.mindgate.domain.model.ExtensionRequest
import com.mindgate.util.MindGateLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionManager @Inject constructor(
    private val extensionGrantDao: ExtensionGrantDao,
    private val appLimitDao: AppLimitDao,
    private val behaviorSignalDao: BehaviorSignalDao,
    private val assessmentRepository: AssessmentRepository,
    private val extensionRepository: ExtensionRepository,
    private val firestoreRepository: FirestoreRepository,
    private val parentRepository: ParentRepository,
    private val userRepository: com.mindgate.data.repository.UserRepository,
    private val policyEngine: PolicyEngine,
    private val sessionManager: SessionManager,
    private val appInfoHelper: com.mindgate.util.AppInfoHelper
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val EXTENSION_MINUTES = 15
        const val MAX_PER_DAY = 3
        val EXTENSION_MS = EXTENSION_MINUTES * 60 * 1000L
    }

    suspend fun grantExtension(packageName: String, quizType: String): ExtensionResult {
        val userId = sessionManager.getUserId()
        val latestRisk = assessmentRepository.getLatestResult(userId)
        val topFactors = latestRisk?.topFactors ?: emptyList()
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
        val circumventionCount = behaviorSignalDao.countSignals(userId, sevenDaysAgo)
        
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        if (policyEngine.isExtensionBlocked(topFactors, hour)) {
            return ExtensionResult.PolicyBlocked("Extensions disabled based on your profile.")
        }
        
        val extensionMinutes = policyEngine.evaluateExtensionMinutes(topFactors, circumventionCount, EXTENSION_MINUTES)
        val extensionMs = extensionMinutes * 60_000L
        val now = System.currentTimeMillis()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
        val count = extensionGrantDao.countGrantsToday(packageName, today)
        
        if (count >= MAX_PER_DAY) {
            val parentUid = sessionManager.linkedParentUid.first()
            val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
            if (parentUid != null && parentUid.isNotBlank() && firebaseUid != null) {
                val studentName = userRepository.getUser(userId)?.name ?: "Student"
                val result = firestoreRepository.createExtensionRequest(
                    studentUid = firebaseUid,
                    parentUid = parentUid,
                    appPackage = packageName,
                    appName = appInfoHelper.getAppName(packageName),
                    studentName = studentName,
                    requestedMinutes = extensionMinutes
                )
                
                if (result.isSuccess) {
                    scope.launch { parentRepository.notifyParentOfRequest(appInfoHelper.getAppName(packageName)) }
                    return ExtensionResult.PendingFirestore(result.getOrNull()!!)
                }
            }
            
            val result = extensionRepository.requestApproval(userId, packageName)
            return result.fold(
                onSuccess = { ExtensionResult.Pending(it) },
                onFailure = { ExtensionResult.DailyCapReached(MAX_PER_DAY) }
            )
        }
        
        val expiresAt = now + extensionMs
        extensionGrantDao.insert(ExtensionGrant(userId=userId, packageName=packageName, grantedAt=now, expiresAt=expiresAt, extensionMinutes=extensionMinutes, date=today, quizType=quizType))
        appLimitDao.setExtension(userId, packageName, expiresAt)
        appLimitDao.setBlocked(userId, packageName, false)

        scope.launch {
            val pUid = sessionManager.linkedParentUid.first()
            val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
            if (pUid != null && pUid.isNotBlank() && firebaseUid != null) {
                firestoreRepository.createExtensionRequest(
                    studentUid = firebaseUid,
                    parentUid = pUid,
                    appPackage = packageName,
                    appName = appInfoHelper.getAppName(packageName),
                    studentName = userRepository.getUser(userId)?.name ?: "Student",
                    status = "EARNED"
                )
            }
        }

        scope.launch { extensionRepository.reportLimitExtended(userId, packageName, extensionMinutes) }
        extensionRepository.grantExtensionRemote(userId, packageName, extensionMinutes, quizType)

        return ExtensionResult.Granted(expiresAt = expiresAt, remainingToday = MAX_PER_DAY - count - 1)
    }

    suspend fun syncFirestoreApprovals() {
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        val approvedRequests = firestoreRepository.getApprovedRequestsForStudent(firebaseUid)
        approvedRequests.forEach { req ->
            val respondedAtMs = req.respondedAt?.toDate()?.time ?: System.currentTimeMillis()
            val applied = applyFirestoreApproval(req.appPackage, req.approvedMinutes ?: 15, respondedAtMs, req.id)
            if (applied) {
                firestoreRepository.markRequestConsumed(req.id)
            }
        }
    }

    suspend fun applyFirestoreApproval(packageName: String, minutes: Int, respondedAtMs: Long, requestId: String? = null): Boolean {
        if (minutes <= 0 || respondedAtMs <= 0) return false
        val userId = sessionManager.getUserId()
        if (userId <= 0) return false

        if (requestId != null && extensionGrantDao.existsWithExternalId(requestId)) return true

        val expiresAt = respondedAtMs + (minutes * 60_000L)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(respondedAtMs))

        extensionGrantDao.insert(ExtensionGrant(userId=userId, packageName=packageName, grantedAt=respondedAtMs, expiresAt=expiresAt, extensionMinutes=minutes, date=today, quizType="parent_approved", externalId=requestId))
        appLimitDao.setExtension(userId, packageName, expiresAt)
        appLimitDao.setBlocked(userId, packageName, false)
        return true
    }

    suspend fun syncLinkStatus() {
        val userId = sessionManager.getUserId()
        if (userId <= 0) return
        try {
            val result = parentRepository.checkConnectionStatus()
            if (result is NetworkResult.Success) {
                sessionManager.setLinked(result.data.isLinked)
                sessionManager.saveParentUid(result.data.linkedParentFirebaseUid)
            }
        } catch (e: Exception) {}
    }

    suspend fun createFirestoreRequest(
        studentUid: String,
        parentUid: String,
        appPackage: String,
        studentName: String,
        appName: String = "",
        requestedMinutes: Int = 15,
        status: String = "PENDING"
    ) = firestoreRepository.createExtensionRequest(studentUid, parentUid, appPackage, studentName, appName, requestedMinutes, status)

    suspend fun markFirestoreRequestConsumed(requestId: String) = firestoreRepository.markRequestConsumed(requestId)

    suspend fun getParentIdForStudent(studentUid: String): String? = firestoreRepository.getParentIdForStudent(studentUid)

    fun observeFirestoreRequest(requestId: String): Flow<ExtensionRequest?> = firestoreRepository.observeRequestStatus(requestId)

    suspend fun getConnectionStatus() = parentRepository.checkConnectionStatus()

    suspend fun notifyParentOfRequest(appName: String) = parentRepository.notifyParentOfRequest(appName)

    suspend fun getUserId(): Long = sessionManager.getUserId()

    suspend fun getActiveExtension(packageName: String): ExtensionGrant? = extensionGrantDao.getActiveExtension(packageName, System.currentTimeMillis())

    suspend fun hasActiveExtension(packageName: String): Boolean = getActiveExtension(packageName) != null
}

sealed class ExtensionResult {
    data class Granted(val expiresAt: Long, val remainingToday: Int) : ExtensionResult()
    data class DailyCapReached(val cap: Int) : ExtensionResult()
    data class Pending(val requestId: Long) : ExtensionResult()
    data class PendingFirestore(val requestId: String) : ExtensionResult()
    data class PolicyBlocked(val reason: String) : ExtensionResult()
}
