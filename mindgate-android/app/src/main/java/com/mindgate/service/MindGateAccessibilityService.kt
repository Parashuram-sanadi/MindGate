package com.mindgate.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.mindgate.di.MindGateEntryPoint
import com.mindgate.presentation.ui.blocked.BlockedOverlayActivity
import com.mindgate.util.Constants
import com.mindgate.util.MindGateLog
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import java.time.LocalDate

class MindGateAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == lastPackage) return
        lastPackage = pkg

        scope.launch {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext, MindGateEntryPoint::class.java
            )
            val userId = entryPoint.sessionManager().getUserId()
            if (userId <= 0) return@launch

            val database = entryPoint.database()
            val limit = database.appLimitDao().getLimit(userId, pkg) ?: return@launch
            if (!limit.isEnabled) return@launch

            if (!entryPoint.usageStatsHelper().hasUsageStatsPermission()) {
                MindGateLog.d("ACCESSIBILITY", "Usage Access permission missing, skipping limit check.")
                return@launch
            }

            // USE THE CENTRAL HELPER TO ENSURE CONSISTENT USAGE REPORTING
            val currentSecondsMap = entryPoint.usageStatsHelper().getTodayUsageSecondsByPackage()
            val usedSeconds = currentSecondsMap[pkg] ?: 0L
            val usedMinutes = (usedSeconds / 60L).toInt()
            val effectiveLimitMinutes = limit.getEffectiveLimitMinutes()
            val effectiveLimitSeconds = effectiveLimitMinutes * 60L
            
            val hasExtension = limit.extensionUntil > System.currentTimeMillis()
            val isExceeded = usedSeconds >= effectiveLimitSeconds

            Log.d("MindGateBlocking", "Check - pkg: $pkg, used: $usedMinutes, limit: $effectiveLimitMinutes, hasExtension: $hasExtension, isExceeded: $isExceeded")

            if (isExceeded && !hasExtension) {
                Log.d("MindGateBlocking", "BLOCKING: $pkg")
                val intent = Intent(this@MindGateAccessibilityService, BlockedOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    putExtra(Constants.EXTRA_PACKAGE_NAME, pkg)
                    putExtra(Constants.EXTRA_APP_NAME, limit.appName)
                    putExtra("LIMIT_MINUTES", effectiveLimitMinutes)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {}
}
