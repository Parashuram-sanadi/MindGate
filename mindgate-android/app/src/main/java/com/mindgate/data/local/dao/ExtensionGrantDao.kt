package com.mindgate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mindgate.data.local.entities.ExtensionGrant

@Dao
interface ExtensionGrantDao {

    @Insert
    suspend fun insert(grant: ExtensionGrant): Long

    // Active = not yet expired
    @Query("""
        SELECT * FROM extension_grants
        WHERE packageName = :pkg AND expiresAt > :now
        ORDER BY expiresAt DESC LIMIT 1
    """)
    suspend fun getActiveExtension(pkg: String, now: Long): ExtensionGrant?

    // Count grants today for daily cap check
    @Query("""
        SELECT COUNT(*) FROM extension_grants
        WHERE packageName = :pkg AND date = :date
    """)
    suspend fun countGrantsToday(pkg: String, date: String): Int

    // Housekeeping — call at midnight
    @Query("DELETE FROM extension_grants WHERE expiresAt < :now")
    suspend fun clearExpired(now: Long)

    @Query("""
        SELECT EXISTS(SELECT 1 FROM extension_grants 
        WHERE userId = :userId AND packageName = :pkg AND grantedAt = :grantedAt)
    """)
    suspend fun exists(userId: Long, pkg: String, grantedAt: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM extension_grants WHERE externalId = :externalId)")
    suspend fun existsWithExternalId(externalId: String): Boolean
}
