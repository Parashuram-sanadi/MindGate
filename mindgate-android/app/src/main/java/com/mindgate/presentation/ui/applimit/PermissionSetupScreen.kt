package com.mindgate.presentation.ui.applimit

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.mindgate.service.UsageMonitorService
import com.mindgate.service.WorkScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    
    var usageGranted by remember { mutableStateOf(checkUsagePermission(context)) }
    var notificationsGranted by remember { mutableStateOf(checkNotificationsPermission(context)) }
    var batteryIgnored by remember { mutableStateOf(checkBatteryPermission(context)) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { notificationsGranted = it }

    // Re-check on resume
    DisposableEffect(Unit) {
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Final Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "MindGate needs these permissions to monitor your usage and block apps in real-time.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Spacer(Modifier.height(24.dp))

        PermissionCard(
            title = "Usage Access",
            description = "Needed to track how long you spend in each app.",
            icon = Icons.Default.Timeline,
            isGranted = usageGranted,
            onGrant = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        )

        PermissionCard(
            title = "Notifications",
            description = "Needed to alert you when you're nearing your limits.",
            icon = Icons.Default.Notifications,
            isGranted = notificationsGranted,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )

        PermissionCard(
            title = "Battery Optimization",
            description = "Prevents the system from stopping MindGate in the background.",
            icon = Icons.Default.BatteryAlert,
            isGranted = batteryIgnored,
            onGrant = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                UsageMonitorService.start(context)
                WorkScheduler.schedule(context)
                onComplete()
            },
            enabled = usageGranted && notificationsGranted,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Activate MindGate")
        }
        
        TextButton(onClick = { 
            usageGranted = checkUsagePermission(context)
            notificationsGranted = checkNotificationsPermission(context)
            batteryIgnored = checkBatteryPermission(context)
        }) {
            Text("Refresh Permission Status")
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            
            if (isGranted) {
                AssistChip(
                    onClick = {},
                    label = { Text("Granted") },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp)) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF4CAF50))
                )
            } else {
                Button(onClick = onGrant, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Grant", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun checkUsagePermission(context: Context): Boolean {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val now = System.currentTimeMillis()
    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 1000 * 60, now)
    return stats != null && stats.isNotEmpty()
}

private fun checkNotificationsPermission(context: Context): Boolean {
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun checkBatteryPermission(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
