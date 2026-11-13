package com.mindgate.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class AppCategory {
    ALL, SOCIAL, GAMES, ENTERTAINMENT, OTHER
}

@Singleton
class AppInfoHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pm: PackageManager = context.packageManager

    fun getInstalledApps(): List<AppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val leanbackIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }

        val launcherApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, PackageManager.GET_META_DATA)
        }

        val leanbackApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(leanbackIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(leanbackIntent, PackageManager.GET_META_DATA)
        }
        
        val allResolveInfo = launcherApps + leanbackApps

        return allResolveInfo
            .map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                AppInfo(
                    appName = resolveInfo.loadLabel(pm).toString(),
                    packageName = packageName,
                    icon = resolveInfo.loadIcon(pm),
                    category = getAppCategory(pm, packageName)
                )
            }
            .filter { it.packageName != context.packageName } // exclude MindGate itself
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    private fun getAppCategory(pm: PackageManager, packageName: String): AppCategory {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val info = pm.getApplicationInfo(packageName, 0)
                when (info.category) {
                    ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
                    ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
                    ApplicationInfo.CATEGORY_VIDEO,
                    ApplicationInfo.CATEGORY_AUDIO,
                    ApplicationInfo.CATEGORY_IMAGE -> AppCategory.ENTERTAINMENT
                    else -> AppCategory.OTHER
                }
            } else {
                AppCategory.OTHER
            }
        } catch (e: Exception) {
            AppCategory.OTHER
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            pm.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    fun getAppName(packageName: String): String {
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            "App"
        }
    }
}

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    val category: AppCategory
)
