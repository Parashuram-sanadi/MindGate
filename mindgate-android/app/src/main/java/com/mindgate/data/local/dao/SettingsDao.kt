package com.mindgate.data.local.dao

import androidx.room.*
import com.mindgate.data.local.entities.QuizAttemptEntity
import com.mindgate.data.local.entities.UserSettingsEntity
import com.mindgate.data.local.entities.UserStreakEntity

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getSettings(userId: Long): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: UserSettingsEntity)

    @Query("SELECT * FROM user_streak WHERE userId = :userId LIMIT 1")
    suspend fun getStreak(userId: Long): UserStreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStreak(streak: UserStreakEntity)

    @Insert
    suspend fun insertQuizAttempt(attempt: QuizAttemptEntity)

    @Query("SELECT quizType FROM quiz_attempts WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastQuizType(userId: Long): String?
}
