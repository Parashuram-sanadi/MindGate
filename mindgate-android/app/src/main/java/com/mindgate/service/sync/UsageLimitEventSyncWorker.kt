package com.mindgate.service.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindgate.data.repository.UsageLimitEventRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UsageLimitEventSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: UsageLimitEventRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = repository.syncPendingEvents()
        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
