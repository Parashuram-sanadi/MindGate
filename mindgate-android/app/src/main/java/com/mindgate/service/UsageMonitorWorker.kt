package com.mindgate.service

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindgate.R
import com.mindgate.data.local.PreferencesManager
import com.mindgate.domain.usecase.CheckLimitsUseCase
import com.mindgate.domain.usecase.GetUsageStatsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class UsageMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getUsageStats: GetUsageStatsUseCase,
    private val checkLimits: CheckLimitsUseCase,
    private val prefs: PreferencesManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = prefs.currentUserId.first()
        if (userId <= 0) return Result.success()
        
        // Sync and check all limits
        getUsageStats(userId)
        val blockedPackages = checkLimits(userId)
        
        if (blockedPackages.isNotEmpty()) {
            val nm = applicationContext.getSystemService(NotificationManager::class.java)
            val notification = NotificationCompat.Builder(applicationContext, "usage_alerts")
                .setSmallIcon(R.drawable.ic_mindgate_logo)
                .setContentTitle("Limits reached")
                .setContentText("${blockedPackages.size} app(s) have reached their daily limit.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            nm.notify(1001, notification)
        }
        return Result.success()
    }
}
