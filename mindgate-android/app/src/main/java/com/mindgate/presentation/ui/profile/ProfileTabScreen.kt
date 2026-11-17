package com.mindgate.presentation.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.home.HomeViewModel
import com.mindgate.presentation.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTabScreen(
    onRetakeAssessment: () -> Unit,
    onParentConnect: () -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val clipboardManager = LocalClipboardManager.current
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "Not Logged In" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(AppIcons.Back, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(AppIcons.Logout, contentDescription = "Logout", tint = ActionDanger)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceCard,
                    shape = Shapes.medium,
                    shadowElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = PrimaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(32.dp), tint = Primary)
                                }
                            }
                            Spacer(Modifier.width(20.dp))
                            Column {
                                Text(state.userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = TextPrimary)
                                Text("Student", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = Outline.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ACCOUNT ID", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text(currentUid, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            }
                            Surface(
                                onClick = { clipboardManager.setText(AnnotatedString(currentUid)) },
                                color = SurfaceVariant,
                                shape = Shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = Primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Copy", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Primary)
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Text("Account Actions", style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
                
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MindGateButton(
                        text = "Update Health Assessment",
                        onClick = onRetakeAssessment,
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.Primary,
                        icon = Icons.Default.Assignment
                    )
                    
                    if (state.isLinked) {
                        val countText = if (state.linkedCount > 1) "Linked Parents (${state.linkedCount})" else "Parent Linked"
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = PrimaryContainer.copy(alpha = 0.3f),
                            shape = Shapes.medium,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ActionSuccess)
                                    Spacer(Modifier.width(12.dp))
                                    Text(countText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                
                                if (state.linkedParents.isNotEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    state.linkedParents.forEach { parent ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                                                Spacer(Modifier.width(8.dp))
                                                Text(parent.parentName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                            }
                                            TextButton(onClick = { viewModel.unlinkParent(parent.parentId) }) {
                                                Text("Unlink", style = MaterialTheme.typography.labelMedium, color = ActionDanger, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        MindGateButton(
                            text = "Link Another Parent Account",
                            onClick = onParentConnect,
                            modifier = Modifier.fillMaxWidth(),
                            style = ButtonStyle.Neutral,
                            isSecondary = true,
                            icon = Icons.Default.Add
                        )

                        MindGateButton(
                            text = "Refresh Connection Status",
                            onClick = { viewModel.syncConnection() },
                            modifier = Modifier.fillMaxWidth(),
                            style = ButtonStyle.Neutral,
                            isSecondary = true,
                            isLoading = state.isLoading,
                            icon = Icons.Default.Sync
                        )
                    } else {
                        MindGateButton(
                            text = "Link a Parent Account",
                            onClick = onParentConnect,
                            modifier = Modifier.fillMaxWidth(),
                            style = ButtonStyle.Neutral,
                            isSecondary = true,
                            icon = Icons.Default.Link
                        )
                    }
                }
            }

            item {
                Text("Assessment History", style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            }

            if (state.history.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceVariant.copy(alpha = 0.5f),
                        shape = Shapes.medium
                    ) {
                        Text(
                            "No previous assessments found.",
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(state.history) { historyItem ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        color = SurfaceCard,
                        shape = Shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(historyItem.riskLevel, fontWeight = FontWeight.Bold, color = TextPrimary) },
                            supportingContent = { 
                                Text("${historyItem.riskScore} pts • ${dateFormat.format(Date(historyItem.timestamp))}", color = TextSecondary)
                            },
                            trailingContent = {
                                if (historyItem.timestamp == state.risk?.timestamp) {
                                    Surface(color = Primary, shape = CircleShape) {
                                        Text(
                                            "LATEST", 
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            item {
                Spacer(Modifier.height(24.dp))
                MindGateButton(
                    text = "Sign Out",
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Danger,
                    isSecondary = true,
                    icon = AppIcons.Logout
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
