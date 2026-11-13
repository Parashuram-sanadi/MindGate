package com.mindgate.data.repository

import com.mindgate.data.local.dao.CheckInDao
import com.mindgate.data.local.dao.ReflectionDao
import com.mindgate.data.local.entities.CheckInEntity
import com.mindgate.data.local.entities.ReflectionEntity
import com.mindgate.data.remote.api.MindGateApiService
import com.mindgate.util.MindGateLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInRepository @Inject constructor(
    private val api: MindGateApiService,
    private val checkInDao: CheckInDao,
    private val reflectionDao: ReflectionDao
) {

    suspend fun saveCheckIn(studentId: Long, mood: Int, reason: String) = withContext(Dispatchers.IO) {
        checkInDao.insert(CheckInEntity(studentId = studentId, mood = mood, reason = reason))
        syncCheckIns()
    }

    suspend fun saveReflection(studentId: Long, tag: String, response: String) = withContext(Dispatchers.IO) {
        reflectionDao.insert(ReflectionEntity(studentId = studentId, promptTag = tag, response = response))
        syncReflections()
    }

    private suspend fun syncCheckIns() {
        // Opportunistic sync implementation
    }

    private suspend fun syncReflections() {
        // Opportunistic sync implementation
    }
}
