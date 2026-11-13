package com.mindgate.presentation.ui.parent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.firebase.auth.FirebaseAuth
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*
import com.mindgate.presentation.viewmodel.ParentDashboardViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ExtensionRequestCard(
    request: com.mindgate.domain.model.ExtensionRequest,
    childName: String,
    requestCountToday: Int = 0,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    var confirmedStatus by remember { mutableStateOf<String?>(null) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = Shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ActionWarning.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = ActionWarning.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = childName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = ActionWarning
                        )
                    }
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(childName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    
                    val timestamp = request.requestedAt ?: request.createdAt
                    val relativeTime = remember(timestamp) {
                        timestamp?.toDate()?.time?.let { getRelativeTime(it) } ?: "just now"
                    }
                    Text(relativeTime, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }

                if (requestCountToday > 0) {
                    Surface(
                        color = ActionWarning.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (requestCountToday == 1) "1st today" else "${requestCountToday}th today",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = ActionWarning
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))

            Text(
                text = "Requesting ${request.requestedMinutes} extra minutes for ${request.appName}.",
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
            )
            
            Spacer(Modifier.height(24.dp))
            
            AnimatedContent(
                targetState = confirmedStatus,
                transitionSpec = {
                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                },
                label = "approval_confirmation"
            ) { status ->
                if (status != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val isApproved = status == "approved"
                        Icon(
                            if (isApproved) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            null,
                            tint = if (isApproved) ActionSuccess else ActionDanger,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (isApproved) "Approved" else "Denied",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isApproved) ActionSuccess else ActionDanger
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MindGateButton(
                            text = "Approve",
                            onClick = { 
                                confirmedStatus = "approved"
                                onApprove() 
                            },
                            modifier = Modifier.weight(1f),
                            style = ButtonStyle.Success,
                            icon = Icons.Default.Check
                        )
                        MindGateButton(
                            text = "Deny",
                            onClick = { 
                                confirmedStatus = "denied"
                                onDeny() 
                            },
                            modifier = Modifier.weight(1f),
                            style = ButtonStyle.Danger,
                            isSecondary = true,
                            icon = Icons.Default.Close
                        )
                    }
                }
            }
        }
    }
}

private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val mins = diff / 60000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "$mins min ago"
        mins < 1440 -> "${mins / 60} hours ago"
        else -> "${mins / 1440} days ago"
    }
}

@Composable
fun ActivityItem(activity: com.mindgate.domain.model.ExtensionRequest) {
    val status = activity.status.lowercase()
    val icon = when (status) {
        "earned" -> AppIcons.Approved
        "approved" -> Icons.Default.Verified
        "denied" -> AppIcons.Denied
        "consumed" -> AppIcons.History
        else -> Icons.Default.Info
    }
    
    val tint = when (status) {
        "earned", "approved" -> ActionSuccess
        "denied" -> ActionDanger
        else -> TextSecondary
    }

    val description = when (status) {
        "earned" -> "${activity.studentName} earned ${activity.requestedMinutes}m by completing a quiz."
        "approved" -> "You approved ${activity.approvedMinutes ?: activity.requestedMinutes}m for ${activity.studentName}."
        "denied" -> "You denied the request from ${activity.studentName}."
        "consumed" -> "${activity.studentName} used the extension for ${activity.appName}."
        else -> "Activity recorded for ${activity.studentName}."
    }

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
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                val timestamp = activity.requestedAt ?: activity.createdAt
                val time = timestamp?.toDate()?.let {
                    java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(it)
                } ?: "recently"
                Text(time, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    onBack: () -> Unit,
    onNavigateToChild: (Long) -> Unit,
    onNavigateToConnect: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ParentDashboardViewModel = hiltViewModel()
) {
    val children by viewModel.children.collectAsStateWithLifecycle()
    val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()
    val recentActivity by viewModel.recentActivity.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "Not Logged In" }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Retry",
                duration = SnackbarDuration.Long
            ).also { result ->
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.refresh()
                }
            }
        }
    }

    val context = LocalContext.current
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    // Requirement 4: Fallback sync on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Requirement 4: Battery Optimization Exemption Prompt (One-time)
    LaunchedEffect(Unit) {
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS as fallback
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Parent Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.Back, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }, enabled = !isRefreshing) {
                        Icon(AppIcons.Sync, contentDescription = "Refresh", tint = ActionPrimary)
                    }
                    IconButton(onClick = onNavigateToConnect) {
                        Icon(AppIcons.Add, contentDescription = "Connect Child", tint = ActionPrimary)
                    }
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(AppIcons.Logout, contentDescription = "Logout", tint = ActionDanger)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (children.isEmpty() && !isRefreshing) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No children linked yet.", color = TextSecondary)
                    
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = { clipboardManager.setText(AnnotatedString(currentUid)) },
                        color = Color.Transparent,
                        shape = Shapes.small
                    ) {
                        Text(
                            text = "My Account ID: $currentUid", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = ActionPrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    MindGateButton(
                        text = "Connect Child",
                        onClick = onNavigateToConnect,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        style = ButtonStyle.Primary
                    )
                    Spacer(Modifier.height(16.dp))
                    MindGateButton(
                        text = "Logout",
                        onClick = onLogout,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        style = ButtonStyle.Danger,
                        isSecondary = true,
                        icon = Icons.AutoMirrored.Filled.Logout
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            onClick = { clipboardManager.setText(AnnotatedString(currentUid)) },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent,
                            shape = Shapes.small
                        ) {
                            Text(
                                text = "Account ID: $currentUid (Tap to Copy)", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    if (pendingRequests.isNotEmpty()) {
                        item {
                            Text(
                                "Extension Requests",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ActionWarning
                            )
                        }
                        
                        items(pendingRequests, key = { it.id }) { req ->
                            val childName = children.find { 
                                it.studentId.toString() == (req.studentUid.ifBlank { req.studentId }) 
                            }?.name ?: req.studentName
                            
                            // Requirement B3: Context for parent (Number of requests today)
                            val countToday = recentActivity.count { 
                                (it.studentId == req.studentId || it.studentUid == req.studentId)
                            } + 1

                            ExtensionRequestCard(
                                request = req,
                                childName = childName,
                                requestCountToday = countToday,
                                onApprove = { viewModel.approveRequest(req.id) },
                                onDeny = { viewModel.denyRequest(req.id) }
                            )
                        }
                        
                        item { Spacer(Modifier.height(8.dp)) }
                    }

                    if (recentActivity.isNotEmpty()) {
                        item {
                            Text(
                                "Recent Activity",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        items(recentActivity, key = { it.id }) { activity ->
                            ActivityItem(activity)
                        }
                        
                        item { Spacer(Modifier.height(16.dp)) }
                    }

                    item {
                        Text(
                            "My Children",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(children, key = { it.studentId }) { child ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by MindGateMotion.tapScale(isPressed)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale),
                            onClick = { onNavigateToChild(child.studentId) },
                            interactionSource = interactionSource,
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(child.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    IconButton(onClick = { viewModel.revoke(child.studentId) }) {
                                        Icon(Icons.Default.LinkOff, contentDescription = "Disconnect", tint = ActionDisabledText)
                                    }
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Today's Usage", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Text("Check details", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Current Status", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Text(
                                            text = child.status.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (child.status == "active") ActionSuccess else ActionWarning
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                
                                MindGateButton(
                                    text = "View Details",
                                    onClick = { onNavigateToChild(child.studentId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    style = ButtonStyle.Neutral,
                                    isSecondary = true,
                                    icon = Icons.Default.Analytics
                                )
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(32.dp))
                        MindGateButton(
                            text = "Logout",
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(),
                            style = ButtonStyle.Danger,
                            isSecondary = true,
                            icon = Icons.AutoMirrored.Filled.Logout
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            if (isRefreshing && children.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
