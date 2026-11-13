package com.mindgate.data.repository

import android.content.Context
import androidx.work.*
import com.mindgate.data.local.dao.UsageLimitEventDao
import com.mindgate.data.local.entities.UsageLimitEventEntity
import com.mindgate.data.remote.api.ParentApiService
import com.mindgate.data.remote.dto.LimitEventRequest
import com.mindgate.service.sync.UsageLimitEventSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageLimitEventRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: UsageLimitEventDao,
    private val api: ParentApiService
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun recordLimitExceeded(packageName: String?, limitSeconds: Int, actualUsageSeconds: Int) {
        val now = System.currentTimeMillis()
        val event = UsageLimitEventEntity(
            appPackage = packageName,
            limitSeconds = limitSeconds,
            actualUsageSeconds = actualUsageSeconds,
            timestamp = now,
            eventDate = dateFormat.format(Date(now))
        )
        dao.insert(event)
        
        // Enqueue background sync
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val workRequest = OneTimeWorkRequestBuilder<UsageLimitEventSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
            
        WorkManager.getInstance(context).enqueueUniqueWork(
            "usage_limit_sync",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            workRequest
        )
    }

    suspend fun syncPendingEvents(): Result<Unit> = try {
        val pending = dao.getUnsyncedEvents()
        var allSuccess = true
        
        for (event in pending) {
            val resp = api.reportLimitEvent(LimitEventRequest(
                appPackage = event.appPackage,
                limitSeconds = event.limitSeconds,
                actualUsageSeconds = event.actualUsageSeconds,
                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(event.timestamp)),
                eventDate = event.eventDate
            ))
            if (resp.isSuccessful) {
                dao.markSynced(event.id)
            } else {
                allSuccess = false
            }
        }
        
        if (allSuccess) Result.success(Unit)
        else Result.failure(Exception("Some events failed to sync"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
