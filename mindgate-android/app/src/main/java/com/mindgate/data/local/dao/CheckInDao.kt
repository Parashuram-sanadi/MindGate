package com.mindgate.data.local.dao

import androidx.room.*
import com.mindgate.data.local.entities.CheckInEntity
import com.mindgate.data.local.entities.ReflectionEntity

@Dao
interface CheckInDao {
    @Insert
    suspend fun insert(checkIn: CheckInEntity): Long

    @Query("SELECT * FROM check_ins WHERE studentId = :studentId ORDER BY timestamp DESC")
    suspend fun getHistory(studentId: Long): List<CheckInEntity>

    @Query("SELECT * FROM check_ins WHERE isSynced = 0")
    suspend fun getUnsynced(): List<CheckInEntity>

    @Query("UPDATE check_ins SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}

@Dao
interface ReflectionDao {
    @Insert
    suspend fun insert(reflection: ReflectionEntity): Long

    @Query("SELECT * FROM reflections WHERE studentId = :studentId ORDER BY timestamp DESC")
    suspend fun getHistory(studentId: Long): List<ReflectionEntity>

    @Query("SELECT * FROM reflections WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ReflectionEntity>

    @Query("UPDATE reflections SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
