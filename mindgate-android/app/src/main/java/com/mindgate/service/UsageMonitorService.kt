package com.mindgate.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mindgate.R
import com.mindgate.data.local.SessionManager
import com.mindgate.data.repository.*
import com.mindgate.domain.manager.ExtensionManager
import com.mindgate.presentation.ui.blocked.BlockedOverlayActivity
import com.mindgate.util.Constants
import com.mindgate.util.MindGateLog
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class UsageMonitorService : Service() {

    @Inject lateinit var usageRepository: UsageRepository
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var extensionManager: ExtensionManager
    @Inject lateinit var studyRepository: StudyRepository
    @Inject lateinit var limitWarningRepository: LimitWarningRepository
    @Inject lateinit var policyRepository: PolicyRepository
    @Inject lateinit var limitEventRepository: UsageLimitEventRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val blockedApps = mutableSetOf<String>()
    private val lastReported = mutableMapOf<String, Long>()

    companion object {
        private const val CHANNEL_ID = "mindgate_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val STUDENT_ALERT_CHANNEL = "student_wellness_nudge"

        fun start(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                try {
                    checkUsage()
                    checkNudges()
                } catch (e: Exception) {
                    MindGateLog.e("MONITOR", "Error in monitor loop", e)
                }
                delay(15000)
            }
        }
    }

    private suspend fun checkUsage() {
        val userId = sessionManager.getUserId()
        if (userId <= 0) return

        if (!entryPoint().usageStatsHelper().hasUsageStatsPermission()) {
            MindGateLog.d("MONITOR", "Usage Access permission missing, skipping limit evaluation.")
            return
        }

        // 4.9 Real-time Firestore approval sync
        extensionManager.syncFirestoreApprovals()

        // SYNC USAGE FROM THE PRECISE EVENT-BASED HELPER
        usageRepository.syncUsageFromSystem(userId)

        val limits = usageRepository.getLimits(userId)
        
        // Single source of truth for current usage seconds and minutes
        val currentSecondsMap = entryPoint().usageStatsHelper().getTodayUsageSecondsByPackage()
        val currentUsageMap = currentSecondsMap.mapValues { (_, seconds) -> (seconds / 60L).toInt() }

        // SYNC TO PARENT DASHBOARD
        serviceScope.launch {
            val entry = entryPoint()
            entry.parentRepository().syncFullUsage(currentUsageMap, entry.appInfoHelper())
        }

        limits.filter { it.isEnabled }.forEach { limit ->
            val usedSeconds = currentSecondsMap[limit.packageName] ?: 0L
            val usedMinutes = (usedSeconds / 60L).toInt()
            val effectiveLimitMinutes = limit.getEffectiveLimitMinutes()
            val effectiveLimitSeconds = effectiveLimitMinutes * 60L

            if (usedSeconds >= effectiveLimitSeconds) {
                if (!extensionManager.hasActiveExtension(limit.packageName)) {
                    val last = lastReported[limit.packageName] ?: 0L
                    if (System.currentTimeMillis() - last >= 300_000L) {
                        limitEventRepository.recordLimitExceeded(
                            limit.packageName,
                            effectiveLimitMinutes * 60,
                            usedMinutes * 60
                        )
                        lastReported[limit.packageName] = System.currentTimeMillis()
                    }

                    usageRepository.checkAndBlockExceeded(userId)

                    if (!blockedApps.contains(limit.packageName)) {
                        blockedApps.add(limit.packageName)
                        launchBlockedOverlay(limit.packageName, limit.appName, effectiveLimitMinutes)
                    }
                } else {
                    blockedApps.remove(limit.packageName)
                }
            } else {
                if (blockedApps.contains(limit.packageName)) {
                    blockedApps.remove(limit.packageName)
                    usageRepository.checkAndBlockExceeded(userId)
                }
            }
        }
    }

    private suspend fun checkNudges() {
        val userId = sessionManager.getUserId()
        if (userId <= 0) return

        if (!entryPoint().usageStatsHelper().hasUsageStatsPermission()) {
            MindGateLog.d("MONITOR", "Usage Access permission missing, skipping nudges.")
            return
        }
        
        val date = usageRepository.today()
        val currentSecondsMap = entryPoint().usageStatsHelper().getTodayUsageSecondsByPackage()
        val currentUsageMap = currentSecondsMap.mapValues { (_, seconds) -> (seconds / 60L).toInt() }

        val limits = usageRepository.getLimits(userId)
        limits.filter { it.isEnabled }.forEach { limit ->
            val usedMinutes = currentUsageMap[limit.packageName] ?: 0
            val effectiveLimitMinutes = limit.getEffectiveLimitMinutes()
            
            if (effectiveLimitMinutes > 0) {
                val percent = (usedMinutes.toFloat() / effectiveLimitMinutes * 100).toInt()
                val remaining = effectiveLimitMinutes - usedMinutes
                
                // 1. Check "Very Close" threshold (90% or 5 mins remaining)
                if ((percent >= 90 || remaining <= 5) && usedMinutes < effectiveLimitMinutes) {
                    if (!limitWarningRepository.hasBeenWarned(userId, limit.packageName, date, 90)) {
                        showStudentNudge(limit.appName, 90, remaining)
                        limitWarningRepository.recordWarning(userId, limit.packageName, date, 90)
                        // extensionManager.reportSignal(userId, "limit_warning_shown", ...)
                    }
                } 
                // 2. Check "Approaching" threshold (75%)
                else if (percent >= 75 && usedMinutes < effectiveLimitMinutes) {
                    if (!limitWarningRepository.hasBeenWarned(userId, limit.packageName, date, 75)) {
                        showStudentNudge(limit.appName, 75, remaining)
                        limitWarningRepository.recordWarning(userId, limit.packageName, date, 75)
                        // extensionManager.reportSignal(userId, "limit_warning_shown", ...)
                    }
                }
            }
        }
    }

    private fun showStudentNudge(appName: String, threshold: Int, remaining: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val message = if (threshold >= 90) {
            "Only $remaining minutes remaining for $appName."
        } else {
            "You're approaching your limit for $appName."
        }
        
        val notification = NotificationCompat.Builder(this, STUDENT_ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_mindgate_logo)
            .setContentTitle("MindGate Warning")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(appName.hashCode() + threshold, notification)
    }

    private fun launchBlockedOverlay(packageName: String, appName: String, limitMinutes: Int) {
        val intent = Intent(this, BlockedOverlayActivity::class.java).apply {
            putExtra(Constants.EXTRA_PACKAGE_NAME, packageName)
            putExtra(Constants.EXTRA_APP_NAME, appName)
            putExtra("LIMIT_MINUTES", limitMinutes)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MindGate Active")
            .setContentText("Monitoring your app usage")
            .setSmallIcon(R.drawable.ic_mindgate_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        
        val monitorChannel = NotificationChannel(
            CHANNEL_ID,
            "Usage Monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(monitorChannel)

        val nudgeChannel = NotificationChannel(
            STUDENT_ALERT_CHANNEL,
            "Wellness Nudges",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(nudgeChannel)
    }

    private fun entryPoint() = EntryPointAccessors.fromApplication(
        applicationContext, com.mindgate.di.MindGateEntryPoint::class.java
    )

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
