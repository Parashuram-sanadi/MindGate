package com.mindgate.presentation.ui.applimit

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.presentation.ui.common.AppUsageListItem
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLimitSetupScreen(
    onActivated: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: AppLimitViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Restrictions", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(AppIcons.Profile, contentDescription = "Profile", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.mindgate.presentation.ui.theme.Surface)
            )
        },
        containerColor = com.mindgate.presentation.ui.theme.Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryContainer.copy(alpha = 0.3f),
                shape = Shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "We've suggested these limits based on your addiction risk level to help you regain balance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnPrimaryContainer
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceCard,
                shape = Shapes.medium,
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoStories, null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Study Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Stricter limits during study hours", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = state.studyMode, 
                            onCheckedChange = viewModel::setStudyMode,
                            colors = SwitchDefaults.colors(checkedTrackColor = ActionPrimary)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Installed Apps", 
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = viewModel::resetSuggested,
                    color = Color.Transparent,
                    shape = Shapes.small
                ) {
                    Text(
                        "Suggested", 
                        fontWeight = FontWeight.Bold, 
                        color = Primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.items, key = { it.packageName }) { item ->
                    val icon = remember(item.packageName) {
                        try {
                            context.packageManager.getApplicationIcon(item.packageName).toBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceCard,
                        shape = Shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = Shapes.small,
                                    color = SurfaceVariant
                                ) {
                                    if (icon != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = icon.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    } else {
                                        Icon(Icons.Default.Apps, null, modifier = Modifier.padding(8.dp), tint = Primary)
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(item.appName, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = item.enabled, 
                                    onCheckedChange = { viewModel.toggleApp(item.packageName) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = ActionSuccess)
                                )
                            }
                            
                            if (item.enabled) {
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Slider(
                                        value = item.limitMinutes.toFloat(),
                                        onValueChange = { viewModel.updateLimit(item.packageName, it.toInt()) },
                                        valueRange = 5f..240f,
                                        steps = 15,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        "${item.limitMinutes}m", 
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                        color = Primary,
                                        modifier = Modifier.width(48.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            
            MindGateButton(
                text = "Apply Wellness Plan",
                onClick = {
                    context.startActivity(viewModel.usageStatsIntent())
                    context.startActivity(viewModel.accessibilityIntent())
                    viewModel.activate(onActivated)
                },
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Brand,
                icon = AppIcons.Approved
            )
        }
    }
}

@Composable
fun LimitsTabScreen(viewModel: AppLimitViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    if (state.selectedForEdit != null) {
        RescheduleLimitSheet(
            limit = state.selectedForEdit!!,
            onDismiss = { viewModel.clearEdit() },
            onSave = { viewModel.updateFullLimit(it) }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(com.mindgate.presentation.ui.theme.Surface).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Active Restrictions", 
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = TextPrimary
            )
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = viewModel::resetSuggested,
                color = Color.Transparent,
                shape = Shapes.small
            ) {
                Text(
                    "Restore Defaults", 
                    fontWeight = FontWeight.Bold, 
                    color = Primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.items, key = { it.packageName }) { item ->
                val icon = remember(item.packageName) {
                    try {
                        context.packageManager.getApplicationIcon(item.packageName).toBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceCard,
                    shape = Shapes.medium,
                    shadowElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = Shapes.small,
                            color = SurfaceVariant
                        ) {
                            if (icon != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = icon.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                Icon(Icons.Default.Apps, null, modifier = Modifier.padding(8.dp), tint = Primary)
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(Modifier.weight(1f)) {
                            Text(item.appName, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = "${item.limitMinutes} min / day", 
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        
                        Switch(
                            checked = item.enabled,
                            onCheckedChange = { viewModel.toggleApp(item.packageName); viewModel.activate {} },
                            colors = SwitchDefaults.colors(checkedTrackColor = ActionSuccess)
                        )
                        
                        IconButton(onClick = { viewModel.selectForEdit(item.packageName) }) {
                            Icon(Icons.Default.Edit, "Edit", tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        MindGateButton(
            text = "Re-verify Permissions",
            onClick = {
                context.startActivity(viewModel.usageStatsIntent())
                context.startActivity(viewModel.accessibilityIntent())
            },
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Neutral,
            isSecondary = true,
            icon = Icons.Default.VpnKey
        )
    }
}
