package com.mindgate.util

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

enum class UsageMethod {
    METHOD_A, // Single foreground package state machine
    METHOD_B, // Per-activity event state machine
    METHOD_C, // queryAndAggregateUsageStats (System Aggregated)
    METHOD_D  // queryUsageStats (INTERVAL_DAILY)
}

data class UsageAccuracyReport(
    val startOfDayMs: Long,
    val endTimeMs: Long,
    val maxSecondsToday: Long,
    val methodAResults: Map<String, Long>,
    val methodBResults: Map<String, Long>,
    val methodCResults: Map<String, Long>,
    val methodDResults: Map<String, Long>,
    val excludedPackagesLog: List<Pair<String, String>>
)

@Singleton
class UsageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        var USAGE_METHOD: UsageMethod = UsageMethod.METHOD_C
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun getTodayUsageMinutes(): Map<String, Int> {
        return getTodayUsageSecondsByPackage().mapValues { (pkg, seconds) ->
            val mins = (seconds / 60L).toInt()
            MindGateLog.d("USAGE", "Package: $pkg, Minutes: $mins (Calculated from seconds)")
            mins
        }
    }

    fun getTodayUsageSecondsByPackage(): Map<String, Long> {
        if (!hasUsageStatsPermission()) return emptyMap()

        val usageStatsManager = try {
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        } catch (e: Exception) {
            null
        } ?: return emptyMap()

        val zoneId = try {
            ZoneId.systemDefault()
        } catch (e: Exception) {
            ZoneId.of("UTC")
        }

        val startOfDay = LocalDate.now(zoneId)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val endTime = System.currentTimeMillis()
        if (endTime <= startOfDay) return emptyMap()

        val maxSecondsToday = ((endTime - startOfDay) / 1000L).coerceAtLeast(0L)

        val results = when (USAGE_METHOD) {
            UsageMethod.METHOD_A -> calculateMethodA(usageStatsManager, startOfDay, endTime, maxSecondsToday)
            UsageMethod.METHOD_B -> calculateMethodB(usageStatsManager, startOfDay, endTime, maxSecondsToday)
            UsageMethod.METHOD_C -> calculateMethodC(usageStatsManager, startOfDay, endTime, maxSecondsToday)
            UsageMethod.METHOD_D -> calculateMethodD(usageStatsManager, startOfDay, endTime, maxSecondsToday)
        }

        MindGateLog.d("USAGE_CALC", "Used Method: $USAGE_METHOD, Total Included Packages: ${results.size}, Total Seconds: ${results.values.sum()}")
        return results
    }

    fun getTodayTotalScreenSeconds(): Long {
        return getTodayUsageSecondsByPackage().values.sum()
    }

    private fun getIgnoredPackageReason(pkg: String, launcherPkg: String?, imePackages: Set<String>): String? {
        if (pkg.isBlank()) return "Blank package name"
        if (pkg == context.packageName) return "MindGate app itself"
        if (pkg == "com.android.systemui") return "System UI"
        if (pkg == "android") return "Android OS core"
        if (launcherPkg != null && pkg == launcherPkg) return "Default Launcher ($launcherPkg)"
        if (imePackages.contains(pkg)) return "Active Keyboard (IME)"
        return null
    }

    fun generateAccuracyReport(): UsageAccuracyReport {
        if (!hasUsageStatsPermission()) {
            return UsageAccuracyReport(0, 0, 0, emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyList())
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val zoneId = try { ZoneId.systemDefault() } catch (e: Exception) { ZoneId.of("UTC") }
        val startOfDay = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endTime = System.currentTimeMillis()
        val maxSecondsToday = ((endTime - startOfDay) / 1000L).coerceAtLeast(0L)

        val methodA = calculateMethodA(usageStatsManager, startOfDay, endTime, maxSecondsToday)
        val methodB = calculateMethodB(usageStatsManager, startOfDay, endTime, maxSecondsToday)
        val methodC = calculateMethodC(usageStatsManager, startOfDay, endTime, maxSecondsToday)
        val methodD = calculateMethodD(usageStatsManager, startOfDay, endTime, maxSecondsToday)

        val launcherPkg = try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
        } catch (e: Exception) { null }

        val imePackages = try {
            context.packageManager.queryIntentServices(Intent("android.view.InputMethod"), 0)
                .mapNotNull { it.serviceInfo?.packageName }.toSet()
        } catch (e: Exception) { emptySet() }

        val allTestedPkgs = (methodA.keys + methodB.keys + methodC.keys + methodD.keys).toSet()
        val excludedLogs = mutableListOf<Pair<String, String>>()
        allTestedPkgs.forEach { pkg ->
            val reason = getIgnoredPackageReason(pkg, launcherPkg, imePackages)
            if (reason != null) {
                excludedLogs.add(pkg to reason)
                MindGateLog.d("EXCLUDED_PACKAGE", "Excluded $pkg: $reason")
            }
        }

        val report = UsageAccuracyReport(
            startOfDayMs = startOfDay,
            endTimeMs = endTime,
            maxSecondsToday = maxSecondsToday,
            methodAResults = methodA,
            methodBResults = methodB,
            methodCResults = methodC,
            methodDResults = methodD,
            excludedPackagesLog = excludedLogs
        )

        MindGateLog.d("ACCURACY_REPORT", "=== ACCURACY REPORT ===")
        MindGateLog.d("ACCURACY_REPORT", "Method A Total: ${methodA.values.sum()}s")
        MindGateLog.d("ACCURACY_REPORT", "Method B Total: ${methodB.values.sum()}s")
        MindGateLog.d("ACCURACY_REPORT", "Method C Total: ${methodC.values.sum()}s")
        MindGateLog.d("ACCURACY_REPORT", "Method D Total: ${methodD.values.sum()}s")

        return report
    }

    private fun calculateMethodA(
        usageStatsManager: UsageStatsManager,
        startOfDay: Long,
        endTime: Long,
        maxSecondsToday: Long
    ): Map<String, Long> {
        val launcherPkg = try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
        } catch (e: Exception) { null }

        val imePackages = try {
            context.packageManager.queryIntentServices(Intent("android.view.InputMethod"), 0)
                .mapNotNull { it.serviceInfo?.packageName }.toSet()
        } catch (e: Exception) { emptySet() }

        val usageMsMap = mutableMapOf<String, Long>()
        var currentPkg: String? = null
        var currentStart: Long? = null

        fun closeCurrentSession(closeTime: Long) {
            val p = currentPkg ?: return
            val start = currentStart ?: return
            val effectiveStart = maxOf(start, startOfDay)
            val duration = closeTime - effectiveStart
            if (duration > 0) {
                usageMsMap[p] = (usageMsMap[p] ?: 0L) + duration
            }
            currentPkg = null
            currentStart = null
        }

        try {
            val events = usageStatsManager.queryEvents(startOfDay - (12 * 3600 * 1000L), endTime)
            val event = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val time = event.timeStamp
                val pkg = event.packageName ?: continue
                val type = event.eventType

                if (type == 16 || type == 18 || type == 26) {
                    closeCurrentSession(time)
                    continue
                }

                if (getIgnoredPackageReason(pkg, launcherPkg, imePackages) != null) continue

                if (type == 1) {
                    if (currentPkg != null && currentPkg != pkg) {
                        closeCurrentSession(time)
                    }
                    if (currentPkg == null) {
                        currentPkg = pkg
                        currentStart = time
                    }
                } else if (type == 2 || type == 23) {
                    if (pkg == currentPkg) {
                        closeCurrentSession(time)
                    }
                }
            }

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isInteractive = powerManager?.isInteractive == true

            if (isInteractive && currentPkg != null && currentStart != null) {
                closeCurrentSession(endTime)
            } else {
                currentPkg = null
                currentStart = null
            }
        } catch (e: SecurityException) {
            return emptyMap()
        } catch (e: Exception) {
            return emptyMap()
        }

        val resultMap = mutableMapOf<String, Long>()
        var accumulatedTotalSeconds = 0L

        usageMsMap.forEach { (pkg, totalMs) ->
            var seconds = (totalMs / 1000L).coerceIn(0L, maxSecondsToday)
            if (accumulatedTotalSeconds + seconds > maxSecondsToday) {
                seconds = (maxSecondsToday - accumulatedTotalSeconds).coerceAtLeast(0L)
            }
            if (seconds > 0L) {
                resultMap[pkg] = seconds
                accumulatedTotalSeconds += seconds
            }
        }
        return resultMap
    }

    private fun calculateMethodB(
        usageStatsManager: UsageStatsManager,
        startOfDay: Long,
        endTime: Long,
        maxSecondsToday: Long
    ): Map<String, Long> {
        val launcherPkg = try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
        } catch (e: Exception) { null }

        val imePackages = try {
            context.packageManager.queryIntentServices(Intent("android.view.InputMethod"), 0)
                .mapNotNull { it.serviceInfo?.packageName }.toSet()
        } catch (e: Exception) { emptySet() }

        val usageMsMap = mutableMapOf<String, Long>()
        val openSessionsMap = mutableMapOf<String, Long>() // "pkg/className" -> startTime

        try {
            val events = usageStatsManager.queryEvents(startOfDay - (6 * 3600 * 1000L), endTime)
            val event = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val time = event.timeStamp
                val pkg = event.packageName ?: continue
                val className = event.className ?: pkg
                val type = event.eventType

                if (type == 16 || type == 18 || type == 26) {
                    openSessionsMap.forEach { (key, startTime) ->
                        val p = key.substringBefore("/")
                        val effectiveStart = maxOf(startTime, startOfDay)
                        val duration = time - effectiveStart
                        if (duration > 0) {
                            usageMsMap[p] = (usageMsMap[p] ?: 0L) + duration
                        }
                    }
                    openSessionsMap.clear()
                    continue
                }

                if (getIgnoredPackageReason(pkg, launcherPkg, imePackages) != null) continue

                val sessionKey = "$pkg/$className"
                if (type == 1) {
                    openSessionsMap[sessionKey] = time
                } else if (type == 2 || type == 23) {
                    val startTime = openSessionsMap.remove(sessionKey)
                    if (startTime != null) {
                        val effectiveStart = maxOf(startTime, startOfDay)
                        val duration = time - effectiveStart
                        if (duration > 0) {
                            usageMsMap[pkg] = (usageMsMap[pkg] ?: 0L) + duration
                        }
                    }
                }
            }

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isInteractive = powerManager?.isInteractive == true

            if (isInteractive) {
                openSessionsMap.forEach { (key, startTime) ->
                    val p = key.substringBefore("/")
                    val effectiveStart = maxOf(startTime, startOfDay)
                    val duration = endTime - effectiveStart
                    if (duration > 0) {
                        usageMsMap[p] = (usageMsMap[p] ?: 0L) + duration
                    }
                }
            }
            openSessionsMap.clear()
        } catch (e: SecurityException) {
            return emptyMap()
        } catch (e: Exception) {
            return emptyMap()
        }

        val resultMap = mutableMapOf<String, Long>()
        var accumulatedTotalSeconds = 0L

        usageMsMap.forEach { (pkg, totalMs) ->
            var seconds = (totalMs / 1000L).coerceIn(0L, maxSecondsToday)
            if (accumulatedTotalSeconds + seconds > maxSecondsToday) {
                seconds = (maxSecondsToday - accumulatedTotalSeconds).coerceAtLeast(0L)
            }
            if (seconds > 0L) {
                resultMap[pkg] = seconds
                accumulatedTotalSeconds += seconds
            }
        }
        return resultMap
    }

    private fun calculateMethodC(
        usageStatsManager: UsageStatsManager,
        startOfDay: Long,
        endTime: Long,
        maxSecondsToday: Long
    ): Map<String, Long> {
        val launcherPkg = try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
        } catch (e: Exception) { null }

        val imePackages = try {
            context.packageManager.queryIntentServices(Intent("android.view.InputMethod"), 0)
                .mapNotNull { it.serviceInfo?.packageName }.toSet()
        } catch (e: Exception) { emptySet() }

        return try {
            val statsMap = usageStatsManager.queryAndAggregateUsageStats(startOfDay, endTime)
            val resultMap = mutableMapOf<String, Long>()
            var accumulatedTotalSeconds = 0L

            statsMap.forEach { (pkg, stats) ->
                if (getIgnoredPackageReason(pkg, launcherPkg, imePackages) == null) {
                    var seconds = (stats.totalTimeInForeground / 1000L).coerceIn(0L, maxSecondsToday)
                    if (accumulatedTotalSeconds + seconds > maxSecondsToday) {
                        seconds = (maxSecondsToday - accumulatedTotalSeconds).coerceAtLeast(0L)
                    }
                    if (seconds > 0L) {
                        resultMap[pkg] = seconds
                        accumulatedTotalSeconds += seconds
                    }
                }
            }
            resultMap
        } catch (e: SecurityException) {
            emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun calculateMethodD(
        usageStatsManager: UsageStatsManager,
        startOfDay: Long,
        endTime: Long,
        maxSecondsToday: Long
    ): Map<String, Long> {
        val launcherPkg = try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
        } catch (e: Exception) { null }

        val imePackages = try {
            context.packageManager.queryIntentServices(Intent("android.view.InputMethod"), 0)
                .mapNotNull { it.serviceInfo?.packageName }.toSet()
        } catch (e: Exception) { emptySet() }

        return try {
            val statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                endTime
            ) ?: emptyList()

            val tempMap = mutableMapOf<String, Long>()
            statsList.forEach { stats ->
                val pkg = stats.packageName ?: return@forEach
                if (getIgnoredPackageReason(pkg, launcherPkg, imePackages) == null) {
                    val seconds = stats.totalTimeInForeground / 1000L
                    if (seconds > 0L) {
                        tempMap[pkg] = (tempMap[pkg] ?: 0L) + seconds
                    }
                }
            }

            val resultMap = mutableMapOf<String, Long>()
            var accumulatedTotalSeconds = 0L
            tempMap.forEach { (pkg, secRaw) ->
                var seconds = secRaw.coerceIn(0L, maxSecondsToday)
                if (accumulatedTotalSeconds + seconds > maxSecondsToday) {
                    seconds = (maxSecondsToday - accumulatedTotalSeconds).coerceAtLeast(0L)
                }
                if (seconds > 0L) {
                    resultMap[pkg] = seconds
                    accumulatedTotalSeconds += seconds
                }
            }
            resultMap
        } catch (e: SecurityException) {
            emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
