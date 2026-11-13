package com.mindgate.data.repository

import com.mindgate.data.local.dao.LimitWarningDao
import com.mindgate.data.local.entities.LimitWarningEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LimitWarningRepository @Inject constructor(
    private val limitWarningDao: LimitWarningDao
) {
    suspend fun hasBeenWarned(studentId: Long, appPackage: String, dateKey: String, threshold: Int): Boolean = withContext(Dispatchers.IO) {
        limitWarningDao.hasBeenWarned(studentId, appPackage, dateKey, threshold)
    }

    suspend fun recordWarning(studentId: Long, appPackage: String, dateKey: String, threshold: Int) = withContext(Dispatchers.IO) {
        limitWarningDao.insert(LimitWarningEntity(studentId = studentId, appPackage = appPackage, dateKey = dateKey, thresholdPercent = threshold))
    }
}
