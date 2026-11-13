package com.mindgate.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mindgate.data.local.dao.*
import com.mindgate.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        AssessmentResultEntity::class,
        AppLimitEntity::class,
        DailyUsageEntity::class,
        QuizAttemptEntity::class,
        UserStreakEntity::class,
        UserSettingsEntity::class,
        ExtensionGrant::class,
        ParentChildRelationEntity::class,
        UsageLimitEventEntity::class,
        BehaviorSignalEntity::class,
        CheckInEntity::class,
        ReflectionEntity::class,
        StudySessionEntity::class,
        LimitWarningEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class MindGateDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun usageDao(): UsageDao
    abstract fun settingsDao(): SettingsDao
    abstract fun extensionGrantDao(): ExtensionGrantDao
    abstract fun parentDao(): ParentDao
    abstract fun usageLimitEventDao(): UsageLimitEventDao
    abstract fun behaviorSignalDao(): BehaviorSignalDao
    abstract fun checkInDao(): CheckInDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun limitWarningDao(): LimitWarningDao

    companion object {
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to users table
                db.execSQL("ALTER TABLE users ADD COLUMN roleSelected INTEGER NOT NULL DEFAULT 0")
                
                // Create new tables
                db.execSQL("CREATE TABLE IF NOT EXISTS `parent_child_relations` (`studentId` INTEGER NOT NULL, `name` TEXT NOT NULL, `status` TEXT NOT NULL, `lastSyncAt` INTEGER NOT NULL, PRIMARY KEY(`studentId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `usage_limit_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `appPackage` TEXT, `limitSeconds` INTEGER NOT NULL, `actualUsageSeconds` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `eventDate` TEXT NOT NULL, `synced` INTEGER NOT NULL)")
            }
        }
    }
}
