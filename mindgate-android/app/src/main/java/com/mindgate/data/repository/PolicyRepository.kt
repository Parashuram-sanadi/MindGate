package com.mindgate.data.repository

import com.mindgate.data.remote.api.MindGateApiService
import com.mindgate.util.MindGateLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class PolicyConfig(
    val lateNightCutoffHour: Int = 23,
    val circumventionThreshold: Int = 5,
    val reducedExtensionMinutes: Int = 10,
    val severeCircumventionMinutes: Int = 5,
    val warningThresholdPercent: Int = 80,
    val studyApps: List<String> = listOf(
        "com.google.android.apps.docs",
        "com.microsoft.office.word",
        "com.adobe.reader",
        "com.google.android.keep",
        "org.coursera.android",
        "com.edx.mobile"
    )
)

@Singleton
class PolicyRepository @Inject constructor(
    private val api: MindGateApiService
) {
    private var cachedConfig: PolicyConfig = PolicyConfig()

    suspend fun getPolicyConfig(): PolicyConfig = withContext(Dispatchers.IO) {
        // ideally fetch from API
        cachedConfig
    }

    suspend fun syncPolicy() = withContext(Dispatchers.IO) {
        try {
            // api.getPolicy()
        } catch (e: Exception) {
            MindGateLog.e("POLICY", "Failed to sync", e)
        }
    }
}
