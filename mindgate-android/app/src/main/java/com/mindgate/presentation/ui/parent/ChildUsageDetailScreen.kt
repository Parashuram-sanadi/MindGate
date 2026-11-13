package com.mindgate.presentation.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.components.shimmerEffect
import com.mindgate.presentation.ui.theme.*
import com.mindgate.presentation.viewmodel.ChildUsageDetailViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildUsageDetailScreen(
    studentId: Long,
    onBack: () -> Unit,
    viewModel: ChildUsageDetailViewModel = hiltViewModel()
) {
    val usageData by viewModel.usageData.collectAsStateWithLifecycle()
    val earnedExtensions by viewModel.earnedExtensions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(studentId) {
        viewModel.loadUsage(studentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Usage Breakdown", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.Back, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && usageData == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(Modifier.fillMaxWidth().height(120.dp).shimmerEffect())
                    Box(Modifier.fillMaxWidth().height(40.dp).shimmerEffect())
                    repeat(5) {
                        Box(Modifier.fillMaxWidth().height(60.dp).shimmerEffect())
                    }
                }
            } else if (error != null && usageData == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    MindGateButton(
                        text = "Retry",
                        onClick = { viewModel.loadUsage(studentId) },
                        modifier = Modifier.padding(top = 16.dp),
                        style = ButtonStyle.Primary
                    )
                }
            } else if (usageData != null) {
                val data = usageData!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        UsageSummaryHeader(data.totalSeconds, data.limitSeconds)
                    }

                    if (earnedExtensions.isNotEmpty()) {
                        item {
                            QuizExtensionsCard(earnedExtensions)
                        }
                    }

                    item {
                        Text("App Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    if (data.byApp.isEmpty()) {
                        item {
                            Text("No app usage reported today.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    } else {
                        items(data.byApp) { app ->
                            AppUsageRow(app.label, app.seconds)
                        }
                    }

                    if (data.events.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("Recent Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        items(data.events) { event ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp), tint = ActionDanger)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Limit exceeded at ${event.timestamp.toString().substringAfter('T').substring(0, 5)} UTC",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    
                    item {
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun QuizExtensionsCard(extensions: List<com.mindgate.domain.model.ExtensionRequest>) {
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
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = ActionWarning.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Quiz, null, tint = ActionWarning, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text("Wellness Challenges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                Surface(color = ActionWarning, shape = CircleShape) {
                    Text(
                        "${extensions.size}/3", 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            extensions.take(3).forEach { ext ->
                val timeStr = ext.createdAt?.toDate()?.let { 
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(it)
                } ?: "just now"
                
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.Extension, null, tint = ActionWarning.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${ext.requestedMinutes} min extension earned at $timeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun UsageSummaryHeader(usedSeconds: Int, limitSeconds: Int) {
    val usedMinutes = usedSeconds / 60
    val limitMinutes = limitSeconds / 60
    val progress = if (limitMinutes > 0) usedMinutes.toFloat() / limitMinutes else 1f
    val isExceeded = usedMinutes >= limitMinutes
    
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(800),
        label = "usage_header_progress"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isExceeded) ActionDanger.copy(alpha = 0.08f) else SurfaceVariant,
        shape = Shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isExceeded) ActionDanger.copy(alpha = 0.2f) else Outline)
    ) {
        Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total Usage Today", style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$usedMinutes",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isExceeded) ActionDanger else Primary
                )
                Text(
                    text = " / ${limitMinutes}m",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = if (isExceeded) ActionDanger else Primary,
                trackColor = (if (isExceeded) ActionDanger else Primary).copy(alpha = 0.1f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            if (isExceeded) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Limit exceeded by ${usedMinutes - limitMinutes} minutes",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = ActionDanger
                )
            }
        }
    }
}

@Composable
fun AppUsageRow(label: String, seconds: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text("${seconds / 60}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
