package com.mindgate.data.local.dao

import androidx.room.*
import com.mindgate.data.local.entities.BehaviorSignalEntity

@Dao
interface BehaviorSignalDao {
    @Insert
    suspend fun insert(signal: BehaviorSignalEntity): Long

    @Query("SELECT * FROM behavior_signals WHERE isSynced = 0")
    suspend fun getUnsynced(): List<BehaviorSignalEntity>

    @Query("UPDATE behavior_signals SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM behavior_signals WHERE studentId = :studentId AND timestamp > :since")
    suspend fun countSignals(studentId: Long, since: Long): Int
}
