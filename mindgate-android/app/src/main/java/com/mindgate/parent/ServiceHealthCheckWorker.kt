package com.mindgate.parent

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters

class ServiceHealthCheckWorker(context: Context, params: WorkerParameters) :
    Worker(context, params) {
    override fun doWork(): Result {
        val serviceIntent = Intent(applicationContext, ParentApprovalListenerService::class.java)
        applicationContext.startForegroundService(serviceIntent)
        return Result.success()
    }
}
