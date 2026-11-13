package com.mindgate.data.local.dao

import androidx.room.*
import com.mindgate.data.local.entities.ParentChildRelationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentDao {
    @Query("SELECT * FROM parent_child_relations")
    fun observeChildren(): Flow<List<ParentChildRelationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(relations: List<ParentChildRelationEntity>)

    @Query("DELETE FROM parent_child_relations WHERE studentId = :studentId")
    suspend fun delete(studentId: Long)

    @Query("DELETE FROM parent_child_relations")
    suspend fun clearAll()
}
