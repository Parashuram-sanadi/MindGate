package com.mindgate.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mindgate.di.MindGateEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuietHoursReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val block = intent?.getBooleanExtra("block", false) ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext, MindGateEntryPoint::class.java
            )
            val userId = entryPoint.sessionManager().getUserId()
            if (userId <= 0) return@launch

            entryPoint.database().appLimitDao().setAllBlocked(userId, block)
        }
    }
}

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            UsageMonitorService.start(context)
            WorkScheduler.schedule(context)
        }
    }
}

object WorkScheduler {
    fun schedule(context: Context) {
        val usageRequest = androidx.work.PeriodicWorkRequestBuilder<UsageMonitorWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        ).build()
        val resetRequest = androidx.work.PeriodicWorkRequestBuilder<DailyResetWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        ).build()
        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "usage_monitor", androidx.work.ExistingPeriodicWorkPolicy.KEEP, usageRequest
        )
        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_reset", androidx.work.ExistingPeriodicWorkPolicy.KEEP, resetRequest
        )
    }
}
