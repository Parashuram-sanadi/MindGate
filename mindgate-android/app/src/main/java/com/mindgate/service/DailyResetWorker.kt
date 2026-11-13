package com.mindgate.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindgate.data.local.PreferencesManager
import com.mindgate.data.repository.UsageRepository
import com.mindgate.data.local.dao.ExtensionGrantDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageRepository: UsageRepository,
    private val extensionGrantDao: ExtensionGrantDao,
    private val prefs: PreferencesManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = prefs.currentUserId.first()
        if (userId <= 0) return Result.success()
        val now = System.currentTimeMillis()
        extensionGrantDao.clearExpired(now)

        val yesterday = LocalDate.now().minusDays(1).toString()
        usageRepository.resetDailyUsage(yesterday)
        
        // Reset blocked status for all apps for the new day
        val limits = usageRepository.getLimits(userId)
        val withinLimits = limits.all { !it.isBlocked }
        
        // Clear blocking for the new day
        entryPoint().database().appLimitDao().setAllBlocked(userId, false)
        
        usageRepository.updateStreak(userId, withinLimits)
        return Result.success()
    }

    private fun entryPoint() = dagger.hilt.android.EntryPointAccessors.fromApplication(
        applicationContext, com.mindgate.di.MindGateEntryPoint::class.java
    )
}
