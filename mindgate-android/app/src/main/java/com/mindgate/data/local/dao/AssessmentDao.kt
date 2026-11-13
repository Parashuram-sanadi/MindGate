package com.mindgate.data.local.dao

import androidx.room.*
import com.mindgate.data.local.entities.AssessmentResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {
    @Query("SELECT * FROM assessment_results WHERE userId = :userId AND isLatest = 1 LIMIT 1")
    fun observeLatest(userId: Long): Flow<AssessmentResultEntity?>

    @Query("SELECT * FROM assessment_results WHERE userId = :userId AND isLatest = 1 LIMIT 1")
    suspend fun getLatest(userId: Long): AssessmentResultEntity?

    @Query("SELECT * FROM assessment_results WHERE id = :resultId")
    suspend fun getById(resultId: Long): AssessmentResultEntity?

    @Query("SELECT * FROM assessment_results WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeHistory(userId: Long): Flow<List<AssessmentResultEntity>>

    @Query("UPDATE assessment_results SET isLatest = 0 WHERE userId = :userId")
    suspend fun clearLatestFlag(userId: Long)

    @Insert
    suspend fun insert(result: AssessmentResultEntity): Long
}
