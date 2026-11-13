package com.mindgate.data.repository

import com.mindgate.data.local.dao.AppLimitDao
import com.mindgate.data.local.dao.SettingsDao
import com.mindgate.data.local.dao.UsageDao
import com.mindgate.data.local.entities.AppLimitEntity
import com.mindgate.data.local.entities.DailyUsageEntity
import com.mindgate.data.local.entities.UserSettingsEntity
import com.mindgate.data.local.entities.UserStreakEntity
import com.mindgate.domain.model.AppLimitModel
import com.mindgate.util.MindGateLog
import com.mindgate.util.UsageStatsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    private val appLimitDao: AppLimitDao,
    private val usageDao: UsageDao,
    private val settingsDao: SettingsDao,
    private val usageStatsHelper: UsageStatsHelper,
    private val limitEventRepository: UsageLimitEventRepository
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    fun today(): String = LocalDate.now().format(dateFmt)

    fun observeLimitsWithUsage(userId: Long): Flow<List<AppLimitModel>> {
        val date = today()
        return combine(
            appLimitDao.observeLimits(userId),
            usageDao.observeUsageForDate(userId, date)
        ) { limits, usage ->
            limits.map { limit ->
                val used = usage.find { it.packageName == limit.packageName }?.usedMinutes ?: 0
                AppLimitModel(
                    packageName = limit.packageName,
                    appName = limit.appName,
                    dailyLimitMinutes = limit.dailyLimitMinutes,
                    isEnabled = limit.isEnabled,
                    suggestedLimit = limit.suggestedLimit,
                    usedMinutes = used,
                    isBlocked = limit.isBlocked,
                    effectiveLimitMinutes = limit.getEffectiveLimitMinutes(),
                    isCustomSchedule = limit.isCustomSchedule,
                    extensionUntil = limit.extensionUntil
                )
            }
        }
    }

    suspend fun getLimits(userId: Long): List<AppLimitEntity> = appLimitDao.getLimits(userId)

    suspend fun saveLimits(userId: Long, limits: List<AppLimitEntity>) {
        appLimitDao.upsertAll(limits)
    }

    suspend fun updateLimit(limit: AppLimitEntity) {
        appLimitDao.upsert(limit)
    }

    suspend fun getLimit(userId: Long, packageName: String): AppLimitEntity? =
        appLimitDao.getLimit(userId, packageName)

    suspend fun getSettings(userId: Long): UserSettingsEntity =
        settingsDao.getSettings(userId) ?: UserSettingsEntity(userId = userId)

    suspend fun saveSettings(settings: UserSettingsEntity) =
        settingsDao.upsertSettings(settings)

    suspend fun syncUsageFromSystem(userId: Long) {
        if (!usageStatsHelper.hasUsageStatsPermission()) {
            MindGateLog.d("USAGE_REPO", "Usage Access missing. Skipping sync to daily_usage.")
            return
        }
        val date = today()
        val stats = usageStatsHelper.getTodayUsageSecondsByPackage()
        
        // Clear today's existing daily_usage rows so old wrong values cannot linger
        usageDao.clearDate(date)

        stats.forEach { (pkg, seconds) ->
            val minutes = (seconds / 60L).toInt()
            usageDao.upsert(
                DailyUsageEntity(
                    userId = userId,
                    packageName = pkg,
                    date = date,
                    usedMinutes = minutes
                )
            )
        }
    }

    suspend fun checkAndBlockExceeded(userId: Long): List<String> {
        val date = today()
        val limits = appLimitDao.getLimits(userId)
        val blocked = mutableListOf<String>()
        limits.filter { it.isEnabled }.forEach { limit ->
            val used = usageDao.getUsage(userId, date, limit.packageName)?.usedMinutes ?: 0
            val hasExtension = limit.extensionUntil > System.currentTimeMillis()
            val effectiveLimit = limit.getEffectiveLimitMinutes()
            
            if (used >= effectiveLimit && !hasExtension) {
                if (!limit.isBlocked) {
                    // First time crossing the limit today
                    limitEventRepository.recordLimitExceeded(limit.packageName, effectiveLimit * 60, used * 60)
                }
                appLimitDao.setBlocked(userId, limit.packageName, true)
                blocked.add(limit.packageName)
            } else if (used < effectiveLimit || hasExtension) {
                // Handle unblocking if limit was rescheduled/increased or extension granted
                if (limit.isBlocked) {
                    appLimitDao.setBlocked(userId, limit.packageName, false)
                }
            }
        }
        return blocked
    }

    suspend fun grantExtension(userId: Long, packageName: String, minutes: Int) {
        val until = System.currentTimeMillis() + minutes * 60_000L
        appLimitDao.setExtension(userId, packageName, until)
        appLimitDao.setBlocked(userId, packageName, false)
    }

    suspend fun getStreak(userId: Long): UserStreakEntity =
        settingsDao.getStreak(userId) ?: UserStreakEntity(userId = userId)

    suspend fun updateStreak(userId: Long, withinLimits: Boolean) {
        val today = today()
        val current = getStreak(userId)
        val newStreak = if (withinLimits) {
            if (current.lastCheckDate == today) current.currentStreak
            else if (current.lastCheckDate == LocalDate.now().minusDays(1).format(dateFmt))
                current.currentStreak + 1
            else 1
        } else 0
        settingsDao.upsertStreak(UserStreakEntity(userId, newStreak, today))
    }

    suspend fun resetDailyUsage(date: String) = usageDao.clearDate(date)

    fun suggestedLimitForRisk(riskLevel: String): Int = when (riskLevel.lowercase()) {
        "low" -> 120
        "moderate" -> 60
        "high" -> 30
        "severe" -> 20
        else -> 60
    }
}
