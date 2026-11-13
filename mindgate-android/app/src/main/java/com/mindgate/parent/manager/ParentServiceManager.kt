package com.mindgate.parent.manager

import android.content.Context
import android.content.Intent
import androidx.work.*
import com.mindgate.parent.ParentApprovalListenerService
import com.mindgate.parent.ServiceHealthCheckWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentServiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun startService() {
        val intent = Intent(context, ParentApprovalListenerService::class.java)
        context.startForegroundService(intent)
        scheduleHealthCheck()
    }

    fun stopService() {
        val intent = Intent(context, ParentApprovalListenerService::class.java)
        context.stopService(intent)
        WorkManager.getInstance(context).cancelUniqueWork("ParentServiceHealthCheck")
    }

    private fun scheduleHealthCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ServiceHealthCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ParentServiceHealthCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
