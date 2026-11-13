package com.mindgate.data.repository

import com.mindgate.data.local.dao.ExtensionGrantDao
import com.mindgate.data.local.entities.ExtensionGrant
import com.mindgate.data.remote.api.MindGateApiService
import com.mindgate.data.remote.dto.ExtensionGrantRequest
import com.mindgate.data.remote.dto.TodayExtensionsResponse
import com.mindgate.util.MindGateLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionRepository @Inject constructor(
    private val api: MindGateApiService,
    private val extensionGrantDao: ExtensionGrantDao
) {

    suspend fun grantExtensionRemote(studentId: Long, pkg: String, minutes: Int, method: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.grantExtension(ExtensionGrantRequest(studentId, pkg, minutes, method))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            MindGateLog.e("EXT", "Failed to grant extension remotely", e)
            Result.failure(e)
        }
    }

    suspend fun fetchTodayExtensions(studentId: Long): Result<TodayExtensionsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTodayExtensions(studentId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            MindGateLog.e("EXT", "Failed to fetch today's extensions", e)
            Result.failure(e)
        }
    }

    suspend fun reconcileLocalGrants(studentId: Long, serverResponse: TodayExtensionsResponse) = withContext(Dispatchers.IO) {
        // Simple reconciliation: overwrite local for today if there's a mismatch
        // In a more complex app, we'd merge, but "server wins" is requested.
        val now = System.currentTimeMillis()
        extensionGrantDao.clearExpired(now) // Just in case

        serverResponse.extensions.forEach { dto ->
            // Check if exists
            // We might need a server-side ID in local DB to avoid duplicates, 
            // but for now we'll just check package and grantedAt
            // This is a placeholder for actual reconciliation logic
            extensionGrantDao.insert(
                ExtensionGrant(
                    userId = studentId,
                    packageName = dto.appPackage,
                    grantedAt = dto.grantedAt,
                    expiresAt = dto.grantedAt + dto.extensionMinutes * 60_000L,
                    extensionMinutes = dto.extensionMinutes,
                    date = dto.dateKey,
                    quizType = dto.source
                )
            )
        }
    }

    suspend fun reportLimitExtended(userId: Long, pkg: String, minutes: Int) = withContext(Dispatchers.IO) {
        try {
            api.reportLimitExtended(
                mapOf(
                    "studentId" to userId,
                    "appPackage" to pkg,
                    "minutes" to minutes,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            MindGateLog.e("EXT", "Failed to report limit extended", e)
        }
    }

    suspend fun reportProtectionDisabled(userId: Long) = withContext(Dispatchers.IO) {
        try {
            api.reportProtectionDisabled(mapOf("studentId" to userId, "timestamp" to System.currentTimeMillis()))
        } catch (e: Exception) {
            MindGateLog.e("EXT", "Failed to report protection disabled", e)
        }
    }

    suspend fun reportBehaviorSignal(userId: Long, type: String, payload: String? = null) = withContext(Dispatchers.IO) {
        try {
            val data = mutableMapOf<String, Any>(
                "studentId" to userId,
                "type" to type,
                "timestamp" to System.currentTimeMillis()
            )
            payload?.let { data["payload"] = it }
            api.reportLimitExtended(data) // Reuse generic event reporter
        } catch (e: Exception) {
            MindGateLog.e("EXT", "Failed to report signal $type", e)
        }
    }

    suspend fun requestApproval(studentId: Long, pkg: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val response = api.requestExtensionApproval(mapOf("studentId" to studentId, "appPackage" to pkg))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.id)
            } else {
                Result.failure(Exception("Approval request failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkApprovalStatus(requestId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPendingExtensionStatus(requestId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.status)
            } else {
                Result.failure(Exception("Status check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
