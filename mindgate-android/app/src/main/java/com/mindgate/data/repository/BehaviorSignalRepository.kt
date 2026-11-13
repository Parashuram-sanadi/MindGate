package com.mindgate.data.repository

import com.mindgate.data.local.dao.BehaviorSignalDao
import com.mindgate.data.local.entities.BehaviorSignalEntity
import com.mindgate.data.remote.api.MindGateApiService
import com.mindgate.util.MindGateLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BehaviorSignalRepository @Inject constructor(
    private val api: MindGateApiService,
    private val signalDao: BehaviorSignalDao
) {

    suspend fun logSignal(studentId: Long, type: String, payload: String? = null) = withContext(Dispatchers.IO) {
        val signal = BehaviorSignalEntity(studentId = studentId, type = type, payload = payload)
        signalDao.insert(signal)
        // Opportunistic sync
        syncSignals(studentId)
    }

    suspend fun syncSignals(studentId: Long) = withContext(Dispatchers.IO) {
        try {
            val unsynced = signalDao.getUnsynced()
            if (unsynced.isEmpty()) return@withContext

            // Simple loop for now, ideally batch
            unsynced.forEach { signal ->
                val payload = mutableMapOf<String, Any>(
                    "studentId" to signal.studentId,
                    "type" to signal.type,
                    "timestamp" to signal.timestamp
                )
                signal.payload?.let { payload["data"] = it }

                val response = api.reportLimitExtended(payload) // Reusing endpoint or adding new one
                if (response.isSuccessful) {
                    signalDao.markSynced(listOf(signal.id))
                }
            }
        } catch (e: Exception) {
            MindGateLog.e("SIGNAL", "Sync failed", e)
        }
    }
}
