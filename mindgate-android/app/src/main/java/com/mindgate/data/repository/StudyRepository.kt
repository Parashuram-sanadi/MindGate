package com.mindgate.data.repository

import com.mindgate.data.local.dao.StudySessionDao
import com.mindgate.data.local.entities.StudySessionEntity
import com.mindgate.data.remote.api.MindGateApiService
import com.mindgate.util.MindGateLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class EligibilityResponse(
    val puzzleAvailable: Boolean,
    val studyUnlockAvailable: Boolean,
    val studyMinutesToday: Int,
    val targetMinutes: Int
)

@Singleton
class StudyRepository @Inject constructor(
    private val api: MindGateApiService,
    private val studySessionDao: StudySessionDao
) {
    suspend fun saveSession(studentId: Long, startTime: Long, endTime: Long, source: String, dateKey: String) = withContext(Dispatchers.IO) {
        val durationMs = endTime - startTime
        if (durationMs < 2 * 60 * 1000L) return@withContext // Ignore sessions < 2 min

        val durationMinutes = (durationMs / 60_000L).toInt()
        val session = StudySessionEntity(
            studentId = studentId,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMinutes,
            source = source,
            dateKey = dateKey
        )
        studySessionDao.insert(session)
        syncSessions()
    }

    suspend fun getLocalStudyMinutes(studentId: Long, dateKey: String): Int {
        return studySessionDao.getStudyMinutesToday(studentId, dateKey) ?: 0
    }

    suspend fun getEligibility(studentId: Long): Result<EligibilityResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getExtensionEligibility(studentId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(
                    EligibilityResponse(
                        puzzleAvailable = body["puzzleAvailable"] as? Boolean ?: true,
                        studyUnlockAvailable = body["studyUnlockAvailable"] as? Boolean ?: false,
                        studyMinutesToday = (body["studyMinutesToday"] as? Number)?.toInt() ?: 0,
                        targetMinutes = (body["targetMinutes"] as? Number)?.toInt() ?: 120
                    )
                )
            } else {
                Result.failure(Exception("Failed to fetch eligibility"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncSessions() {
        try {
            val unsynced = studySessionDao.getUnsynced()
            if (unsynced.isEmpty()) return

            unsynced.forEach { session ->
                val payload = mapOf(
                    "studentId" to session.studentId,
                    "startTime" to session.startTime,
                    "endTime" to session.endTime,
                    "durationMinutes" to session.durationMinutes,
                    "source" to session.source,
                    "dateKey" to session.dateKey
                )
                val response = api.reportStudySession(payload)
                if (response.isSuccessful) {
                    studySessionDao.markSynced(listOf(session.copy(isSynced = true)))
                }
            }
        } catch (e: Exception) {
            MindGateLog.e("STUDY", "Sync failed", e)
        }
    }
}
