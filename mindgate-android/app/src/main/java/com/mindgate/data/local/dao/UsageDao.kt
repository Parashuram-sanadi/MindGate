package com.mindgate.data.local.dao

import androidx.room.*
import com.mindgate.data.local.entities.DailyUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM daily_usage WHERE userId = :userId AND date = :date")
    fun observeUsageForDate(userId: Long, date: String): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE userId = :userId AND date = :date AND packageName = :pkg LIMIT 1")
    suspend fun getUsage(userId: Long, date: String, pkg: String): DailyUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usage: DailyUsageEntity)

    @Query("DELETE FROM daily_usage WHERE date = :date")
    suspend fun clearDate(date: String)

    @Query("SELECT SUM(usedMinutes) FROM daily_usage WHERE userId = :userId AND date = :date")
    suspend fun totalMinutesForDate(userId: Long, date: String): Int?
}
