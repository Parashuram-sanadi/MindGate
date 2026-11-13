package com.mindgate.presentation.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindgate.di.MindGateEntryPoint
import com.mindgate.domain.model.AppLimitModel
import com.mindgate.presentation.ui.home.AppUsageItem
import com.mindgate.presentation.ui.home.HomeViewModel
import com.mindgate.presentation.ui.explore.ExploreEntryCard
import com.mindgate.presentation.ui.components.*
import com.mindgate.presentation.ui.theme.*
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HomeViewModel,
    onAddRestriction: () -> Unit,
    onExploreClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAccuracyReportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showAccuracyReportDialog) {
        val entryPoint = remember {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                MindGateEntryPoint::class.java
            )
        }
        UsageAccuracyReportDialog(
            usageStatsHelper = entryPoint.usageStatsHelper(),
            appInfoHelper = entryPoint.appInfoHelper(),
            onDismiss = { showAccuracyReportDialog = false }
        )
    }

    LaunchedEffect(state.requestStatus) {
        state.requestStatus?.let { status ->
            val message = when (status) {
                "APPROVED" -> "Extension request approved. Enjoy your time!"
                "DENIED" -> "Extension request denied. Focus on your goals."
                else -> null
            }
            message?.let { snackbarHostState.showSnackbar(it) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRestriction,
                containerColor = Primary,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 0.dp)
            ) {
                Icon(AppIcons.Add, contentDescription = "Add Restriction")
            }
        },
        containerColor = Surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                DashboardHeader(
                    userName = state.userName,
                    isGlobalEnabled = state.isGlobalEnabled,
                    onToggleGlobal = viewModel::setGlobalEnabled,
                    onLongClickTitle = { showAccuracyReportDialog = true }
                )
            }

            item {
                ConnectionStatusWidget(state.isLinked)
            }

            if (!state.hasUsagePermission) {
                item {
                    val entryPoint = remember {
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            MindGateEntryPoint::class.java
                        )
                    }
                    UsageAccessCard(
                        onOpenSettings = {
                            context.startActivity(entryPoint.usageStatsHelper().openUsageAccessSettings())
                        }
                    )
                }
            } else {
                item {
                    UsageHeroWidget(
                        totalScreenTimeSeconds = state.totalScreenTimeSeconds,
                        limitedAppsCount = state.limits.count { it.isEnabled },
                        hasUsagePermission = state.hasUsagePermission,
                        hasUsageData = state.hasUsageData,
                        hasExceededApp = state.limits.any { it.usedMinutes >= it.effectiveLimitMinutes }
                    )
                }

                if (state.topAppsUsage.isNotEmpty()) {
                    item {
                        TodaysAppsSection(topApps = state.topAppsUsage)
                    }
                }
            }

            item {
                AnimatedContent(
                    targetState = state.requestStatus,
                    transitionSpec = {
                        slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                    },
                    label = "request_widget"
                ) { status ->
                    ExtensionRequestWidget(
                        status = status,
                        onRequest = viewModel::requestMoreTime,
                        isLoading = state.isLoading
                    )
                }
            }

            item {
                ExploreEntryCard(onClick = onExploreClick)
            }

            item {
                Text(
                    text = "Restricted Apps",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (state.limits.isEmpty()) {
                item {
                    EmptyRestrictedApps()
                }
            } else {
                items(state.limits, key = { it.packageName }) { limit ->
                    RestrictedAppRow(limit)
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun UsageAccessCard(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ActionWarning.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, ActionWarning.copy(alpha = 0.3f)),
        shape = Shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = ActionWarning, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Usage Access Required",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Usage Access is off. MindGate needs it to show accurate screen time",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = ActionWarning, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TodaysAppsSection(topApps: List<AppUsageItem>) {
    val context = LocalContext.current
    var showAll by remember { mutableStateOf(false) }
    val maxUsageSeconds = topApps.firstOrNull()?.usedSeconds?.coerceAtLeast(1L) ?: 1L
    val displayedApps = if (showAll) topApps else topApps.take(6)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Apps",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = TextPrimary
            )
            if (topApps.size > 6) {
                TextButton(onClick = { showAll = !showAll }) {
                    Text(if (showAll) "Show less" else "See all (${topApps.size})", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = Shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                displayedApps.forEach { item ->
                    val icon = remember(item.packageName) {
                        try {
                            context.packageManager.getApplicationIcon(item.packageName).toBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val hours = item.usedSeconds / 3600L
                    val minutes = (item.usedSeconds % 3600L) / 60L
                    val timeStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                    val appProgress = (item.usedSeconds.toFloat() / maxUsageSeconds).coerceIn(0.02f, 1f)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = Shapes.small,
                                    color = SurfaceVariant
                                ) {
                                    if (icon != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = icon.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().padding(4.dp)
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Apps, null, tint = Primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = item.appName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Primary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { appProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = Primary,
                            trackColor = ActionNeutral,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusWidget(isLinked: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isLinked) ActionSuccess.copy(alpha = 0.08f) else SurfaceVariant,
        shape = Shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isLinked) ActionSuccess.copy(alpha = 0.2f) else Outline
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isLinked) ActionSuccess.copy(alpha = 0.15f) else ActionDisabled
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isLinked) AppIcons.Approved else Icons.Default.LinkOff,
                        contentDescription = null,
                        tint = if (isLinked) ActionSuccess else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = if (isLinked) "Parent Connected" else "Parent Not Connected",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isLinked) Success else TextPrimary
                )
                Text(
                    text = if (isLinked) "Your usage is being monitored" else "Link a parent to enable remote monitoring",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ExtensionRequestWidget(
    status: String?,
    onRequest: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = Shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Manual Extension Request",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(Modifier.height(16.dp))
            
            if (status == null) {
                MindGateButton(
                    text = "Request More Time",
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Neutral,
                    isSecondary = true,
                    isLoading = isLoading,
                    icon = AppIcons.Extension
                )
            } else {
                val normalizedStatus = status.lowercase()
                val (color, icon, label) = when (normalizedStatus) {
                    "pending" -> Triple(ActionWarning, AppIcons.Pending, "Pending Approval")
                    "approved" -> Triple(ActionSuccess, AppIcons.Approved, "Request Approved")
                    "denied" -> Triple(ActionDanger, AppIcons.Denied, "Request Denied")
                    else -> Triple(TextSecondary, AppIcons.Extension, "Status: $status")
                }
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = color.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (normalizedStatus == "pending") {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = color, strokeWidth = 2.dp)
                        } else {
                            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = color
                        )
                    }
                }
                
                if (normalizedStatus == "approved" || normalizedStatus == "denied") {
                    MindGateButton(
                        text = "Send New Request",
                        onClick = onRequest,
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                        style = ButtonStyle.Neutral,
                        isSecondary = true
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardHeader(
    userName: String,
    isGlobalEnabled: Boolean,
    onToggleGlobal: (Boolean) -> Unit,
    onLongClickTitle: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Primary,
        shape = Shapes.medium,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClickTitle
                    )
            ) {
                Text(
                    text = "Hello, $userName",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isGlobalEnabled) "MindGate is Active" else "MindGate is Paused",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Switch(
                checked = isGlobalEnabled,
                onCheckedChange = onToggleGlobal,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Primary,
                    checkedTrackColor = Color.White,
                    uncheckedThumbColor = ActionDisabled,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun UsageHeroWidget(
    totalScreenTimeSeconds: Long,
    limitedAppsCount: Int,
    hasUsagePermission: Boolean = true,
    hasUsageData: Boolean = true,
    hasExceededApp: Boolean = false
) {
    val hours = totalScreenTimeSeconds / 3600L
    val minutes = (totalScreenTimeSeconds % 3600L) / 60L
    val timeText = when {
        !hasUsagePermission -> "Access Off"
        !hasUsageData -> "No usage recorded yet"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }

    val maxDisplaySeconds = 8 * 3600L
    val progress = if (hasUsagePermission && hasUsageData) (totalScreenTimeSeconds.toFloat() / maxDisplaySeconds).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = MindGateMotion.EaseOut),
        label = "hero_progress"
    )
    
    val pillText = if (limitedAppsCount == 1) "1 app restricted" else "$limitedAppsCount apps restricted"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = Shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(180.dp),
                    color = ActionNeutral,
                    strokeWidth = 16.dp,
                    strokeCap = StrokeCap.Round
                )
                // Active Progress
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(180.dp),
                    color = if (hasExceededApp) ActionDanger else Primary,
                    strokeWidth = 16.dp,
                    strokeCap = StrokeCap.Round
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeText,
                        style = if (!hasUsagePermission || !hasUsageData) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                else MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
                        maxLines = 1,
                        softWrap = false,
                        color = TextPrimary
                    )
                    Text(
                        text = "Total Screen Time",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Surface(
                color = SurfaceVariant,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.Extension, null, modifier = Modifier.size(16.dp), tint = Primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = pillText,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Primary
                    )
                }
            }
        }
    }
}

@Composable
fun RestrictedAppRow(limit: AppLimitModel) {
    val context = LocalContext.current
    val effectiveLimit = limit.effectiveLimitMinutes.coerceAtLeast(1)
    val progress = (limit.usedMinutes.toFloat() / effectiveLimit).coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = Shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = remember(limit.packageName) {
                try {
                    context.packageManager.getApplicationIcon(limit.packageName).toBitmap()
                } catch (e: Exception) {
                    null
                }
            }

            Surface(
                modifier = Modifier.size(52.dp),
                shape = Shapes.small,
                color = SurfaceVariant,
                shadowElevation = 1.dp
            ) {
                if (icon != null) {
                    androidx.compose.foundation.Image(
                        bitmap = icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Apps, null, tint = Primary, modifier = Modifier.size(28.dp))
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = limit.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "${limit.usedMinutes}m / ${effectiveLimit}m",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (progress >= 1f) Danger else TextSecondary
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (progress >= 1f) ActionDanger else if (progress >= 0.8f) ActionWarning else Primary,
                    trackColor = ActionNeutral,
                    strokeCap = StrokeCap.Round
                )
                
                Spacer(Modifier.height(12.dp))
                
                val status = when {
                    limit.extensionUntil > System.currentTimeMillis() -> "Extension Active"
                    progress >= 1f -> "Limit Exceeded"
                    progress >= 0.9f || (limit.effectiveLimitMinutes - limit.usedMinutes) <= 5 -> "Almost full"
                    progress >= 0.75f -> "Approaching limit"
                    else -> "On Track"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (status) {
                        "Extension Active" -> Success
                        "Limit Exceeded" -> Danger
                        "Almost full", "Approaching limit" -> Warning
                        else -> TextSecondary
                    }
                    
                    val statusIcon = when (status) {
                        "Extension Active" -> AppIcons.Extension
                        "Limit Exceeded" -> AppIcons.Locked
                        "Almost full", "Approaching limit" -> Icons.Default.Warning
                        else -> Icons.Default.Check
                    }
                    
                    Icon(statusIcon, null, modifier = Modifier.size(14.dp), tint = statusColor)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                    
                    if (status == "Extension Active") {
                        val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                        Text(
                            text = " • until ${timeFormat.format(java.util.Date(limit.extensionUntil))}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyRestrictedApps() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No restricted apps yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tap + to start focusing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
