package com.mindgate.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import com.mindgate.R
import com.mindgate.data.local.SessionManager
import com.mindgate.data.repository.ExtensionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class ProtectionWatchdogService : Service() {

    @Inject lateinit var extensionRepository: ExtensionRepository
    @Inject lateinit var sessionManager: SessionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastReportedTime = 0L

    companion object {
        private const val CHANNEL_ID = "protection_watchdog"
        private const val NOTIFICATION_ID = 1002
        private const val CHECK_INTERVAL = 60_000L // 1 minute

        fun start(context: Context) {
            val intent = Intent(context, ProtectionWatchdogService::class.java)
            context.startForegroundService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startChecking()
    }

    private fun startChecking() {
        serviceScope.launch {
            while (isActive) {
                if (!isAccessibilityServiceEnabled()) {
                    val userId = sessionManager.getUserId()
                    if (userId > 0 && System.currentTimeMillis() - lastReportedTime > 300_000L) { // Cooldown 5 mins
                        extensionRepository.reportProtectionDisabled(userId)
                        lastReportedTime = System.currentTimeMillis()
                    }
                }
                delay(CHECK_INTERVAL)
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedService = "${packageName}/${MindGateAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (enabledServices == null) return false
        
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(expectedService, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MindGate Protection Active")
            .setContentText("Ensuring your wellness limits are enforced")
            .setSmallIcon(R.drawable.ic_mindgate_logo)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Protection Watchdog",
            NotificationManager.IMPORTANCE_MIN
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
