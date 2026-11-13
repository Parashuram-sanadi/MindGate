package com.mindgate.data.local.dao

import androidx.room.*
import com.mindgate.data.local.entities.AppLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits WHERE userId = :userId")
    fun observeLimits(userId: Long): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits WHERE userId = :userId")
    suspend fun getLimits(userId: Long): List<AppLimitEntity>

    @Query("SELECT * FROM app_limits WHERE userId = :userId AND packageName = :pkg LIMIT 1")
    suspend fun getLimit(userId: Long, pkg: String): AppLimitEntity?

    @Query("SELECT * FROM app_limits WHERE userId = :userId AND packageName = :pkg LIMIT 1")
    fun observeLimit(userId: Long, pkg: String): Flow<AppLimitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(limits: List<AppLimitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: AppLimitEntity)

    @Query("UPDATE app_limits SET isBlocked = :blocked WHERE userId = :userId AND packageName = :pkg")
    suspend fun setBlocked(userId: Long, pkg: String, blocked: Boolean)

    @Query("UPDATE app_limits SET extensionUntil = :until WHERE userId = :userId AND packageName = :pkg")
    suspend fun setExtension(userId: Long, pkg: String, until: Long)

    @Query("UPDATE app_limits SET isBlocked = :blocked WHERE userId = :userId")
    suspend fun setAllBlocked(userId: Long, blocked: Boolean)

    @Query("DELETE FROM app_limits WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)
}
