package com.mindgate.data.local.dao

import androidx.room.*
import com.mindgate.data.local.entities.LimitWarningEntity
import com.mindgate.data.local.entities.StudySessionEntity

@Dao
interface StudySessionDao {
    @Insert
    suspend fun insert(session: StudySessionEntity): Long

    @Query("SELECT SUM(durationMinutes) FROM study_sessions WHERE studentId = :studentId AND dateKey = :dateKey")
    suspend fun getStudyMinutesToday(studentId: Long, dateKey: String): Int?

    @Query("SELECT * FROM study_sessions WHERE isSynced = 0")
    suspend fun getUnsynced(): List<StudySessionEntity>

    @Update
    suspend fun markSynced(sessions: List<StudySessionEntity>)
}

@Dao
interface LimitWarningDao {
    @Insert
    suspend fun insert(warning: LimitWarningEntity): Long

    @Query("SELECT COUNT(*) FROM limit_warnings WHERE studentId = :studentId AND appPackage = :appPackage AND dateKey = :dateKey AND thresholdPercent = :threshold")
    suspend fun hasBeenWarned(studentId: Long, appPackage: String, dateKey: String, threshold: Int): Boolean
}
