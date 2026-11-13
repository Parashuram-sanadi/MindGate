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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.components.shimmerEffect
import com.mindgate.presentation.ui.theme.*
import com.mindgate.presentation.viewmodel.ParentAlertsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentAlertsScreen(
    onBack: () -> Unit,
    viewModel: ParentAlertsViewModel = hiltViewModel()
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Alerts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.Back, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.mindgate.presentation.ui.theme.Surface)
            )
        },
        containerColor = Surface
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && alerts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(8) {
                        Box(Modifier.fillMaxWidth().height(80.dp).shimmerEffect())
                    }
                }
            } else if (error != null && alerts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(error!!, color = ActionDanger)
                    Spacer(Modifier.height(16.dp))
                    MindGateButton(
                        text = "Retry",
                        onClick = { viewModel.loadAlerts() },
                        style = ButtonStyle.Primary
                    )
                }
            } else if (alerts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No alerts yet.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(alerts) { alert ->
                        AlertItem(alert.studentName, alert.type, alert.timestamp)
                    }
                }
            }
        }
    }
}

@Composable
fun AlertItem(studentName: String, type: String, timestamp: String) {
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
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (type == "limit_exceeded") ActionDanger.copy(alpha = 0.1f) else ActionWarning.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (type == "limit_exceeded") Icons.Default.Warning else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (type == "limit_exceeded") ActionDanger else ActionWarning,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(studentName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    text = if (type == "limit_exceeded") "Daily app usage limit exceeded" else type,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = timestamp.substringBefore('T') + " " + timestamp.substringAfter('T').substring(0, 5),
                    style = MaterialTheme.typography.labelSmall,
                    color = ActionDisabledText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
