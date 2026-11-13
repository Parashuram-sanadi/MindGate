package com.mindgate.data.local.dao

import androidx.room.*
import com.mindgate.data.local.entities.UsageLimitEventEntity

@Dao
interface UsageLimitEventDao {
    @Insert
    suspend fun insert(event: UsageLimitEventEntity): Long

    @Query("SELECT * FROM usage_limit_events WHERE synced = 0")
    suspend fun getUnsyncedEvents(): List<UsageLimitEventEntity>

    @Query("UPDATE usage_limit_events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM usage_limit_events WHERE synced = 1 AND timestamp < :before")
    suspend fun deleteOldSyncedEvents(before: Long)
}
