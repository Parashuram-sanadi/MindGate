package com.mindgate.presentation.ui.screens.setup

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*
import com.mindgate.service.ProtectionWatchdogService
import com.mindgate.service.UsageMonitorService
import com.mindgate.service.WorkScheduler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun PermissionSetupScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var usageGranted by remember { mutableStateOf(checkUsagePermission(context)) }
    var accessibilityEnabled by remember { mutableStateOf(checkAccessibilityPermission(context)) }
    var notificationsGranted by remember { mutableStateOf(checkNotificationsPermission(context)) }
    var batteryIgnored by remember { mutableStateOf(checkBatteryPermission(context)) }

    val refreshState = {
        usageGranted = checkUsagePermission(context)
        accessibilityEnabled = checkAccessibilityPermission(context)
        notificationsGranted = checkNotificationsPermission(context)
        batteryIgnored = checkBatteryPermission(context)
        
        val activationEnabled = usageGranted && accessibilityEnabled && notificationsGranted
        Log.d("MindGatePermissions", "Refresh - usageAccessGranted: $usageGranted, accessibilityServiceEnabled: $accessibilityEnabled, notificationsGranted: $notificationsGranted, batteryIgnored: $batteryIgnored, activationEnabled: $activationEnabled")
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { 
        notificationsGranted = it
        refreshState()
    }

    Scaffold(
        containerColor = Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Secure Your Focus",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "We need a few permissions to help you stay in control of your screen time.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step 1: Usage Access
                PermissionCard(
                    title = "Track Usage",
                    description = "Allow MindGate to see which apps you use and for how long.",
                    icon = Icons.Default.Timeline,
                    isGranted = usageGranted,
                    onGrant = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )

                // Step 2: Accessibility Service
                AnimatedVisibility(visible = usageGranted) {
                    PermissionCard(
                        title = "Block Apps",
                        description = "Enable the MindGate Accessibility Service to block apps in real-time.",
                        icon = Icons.Default.Accessibility,
                        isGranted = accessibilityEnabled,
                        onGrant = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                }

                // Step 3: Notifications (only show if Step 2 is done)
                AnimatedVisibility(visible = usageGranted && accessibilityEnabled) {
                    PermissionCard(
                        title = "Receive Alerts",
                        description = "Get notified when your daily limit is running out.",
                        icon = Icons.Default.Notifications,
                        isGranted = notificationsGranted,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationsGranted = true
                            }
                        }
                    )
                }

                // Step 4: Battery (only show if Step 3 is done)
                AnimatedVisibility(visible = usageGranted && accessibilityEnabled && notificationsGranted) {
                    PermissionCard(
                        title = "Stay Active",
                        description = "Keep MindGate running in the background to enforce limits.",
                        icon = Icons.Default.BatteryAlert,
                        isGranted = batteryIgnored,
                        onGrant = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            val activationEnabled = usageGranted && accessibilityEnabled && notificationsGranted

            MindGateButton(
                text = "Activate MindGate",
                onClick = {
                    Log.d("MindGatePermissions", "activationClicked - Re-verifying permissions...")
                    val finalUsage = checkUsagePermission(context)
                    val finalAccessibility = checkAccessibilityPermission(context)
                    val finalNotifications = checkNotificationsPermission(context)
                    
                    Log.d("MindGatePermissions", "Pre-start check - usage: $finalUsage, accessibility: $finalAccessibility, notifications: $finalNotifications")

                    if (!finalUsage) {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    } else if (!finalAccessibility) {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } else {
                        try {
                            UsageMonitorService.start(context)
                            ProtectionWatchdogService.start(context)
                            WorkScheduler.schedule(context)
                            Log.d("MindGatePermissions", "UsageMonitorService start result: SUCCESS")
                            onComplete()
                        } catch (e: Exception) {
                            Log.e("MindGatePermissions", "UsageMonitorService start result: FAILED", e)
                        }
                    }
                },
                enabled = activationEnabled,
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Primary
            )

            Text(
                text = if (activationEnabled) "MindGate is ready to activate." else "Enable both permissions above to activate MindGate.",
                style = MaterialTheme.typography.labelSmall,
                color = if (activationEnabled) ActionSuccess else TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            TextButton(
                onClick = { refreshState() }
            ) {
                Text("Refresh Status", color = ActionPrimary)
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) ActionSuccess.copy(alpha = 0.05f) else SurfaceCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = Shapes.medium,
        border = if (isGranted) androidx.compose.foundation.BorderStroke(1.dp, ActionSuccess.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isGranted) ActionSuccess.copy(alpha = 0.1f) else ActionPrimary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isGranted) ActionSuccess else ActionPrimary
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            Spacer(Modifier.width(8.dp))

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = ActionSuccess,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                MindGateButton(
                    text = "Grant",
                    onClick = onGrant,
                    style = ButtonStyle.Primary,
                    modifier = Modifier.width(80.dp).height(36.dp)
                )
            }
        }
    }
}

private fun checkUsagePermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun checkAccessibilityPermission(context: Context): Boolean {
    val expectedService = "${context.packageName}/com.mindgate.service.MindGateAccessibilityService"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    val isEnabled = enabledServices?.contains(expectedService) == true
    Log.d("MindGatePermissions", "checkAccessibility - expected: $expectedService, raw: $enabledServices, result: $isEnabled")
    return isEnabled
}

private fun checkNotificationsPermission(context: Context): Boolean {
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun checkBatteryPermission(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
