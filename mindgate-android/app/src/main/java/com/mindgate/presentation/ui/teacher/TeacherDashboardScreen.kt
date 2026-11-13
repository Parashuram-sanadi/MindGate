package com.mindgate.presentation.ui.teacher

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*
import com.mindgate.presentation.viewmodel.TeacherViewModel
import com.mindgate.presentation.viewmodel.TeacherUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    onLogout: () -> Unit,
    onNavigateToResults: () -> Unit,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadFile(context, it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { destUri ->
            viewModel.downloadTemplate(context)?.let { templateFile ->
                context.contentResolver.openOutputStream(destUri)?.use { output ->
                    templateFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Analytics", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(AppIcons.Logout, contentDescription = "Logout", tint = ActionDanger)
                    }
                }
            )
        },
        containerColor = com.mindgate.presentation.ui.theme.Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(32.dp),
                color = PrimaryContainer,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Primary
                    )
                }
            }
            
            Spacer(Modifier.height(40.dp))
            
            Text(
                "Assessment Center",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = TextPrimary
            )
            
            Text(
                "Upload bulk student datasets for AI analysis and risk reporting.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = TextSecondary,
                modifier = Modifier.padding(top = 12.dp, bottom = 56.dp),
                style = MaterialTheme.typography.bodyLarge
            )

            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "teacher_state"
            ) { state ->
                when (state) {
                    is TeacherUiState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Primary, strokeWidth = 4.dp)
                            Spacer(Modifier.height(16.dp))
                            Text("Processing Dataset...", fontWeight = FontWeight.Bold, color = Primary)
                        }
                    }
                    is TeacherUiState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, tint = ActionDanger, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(state.message, color = ActionDanger, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(24.dp))
                            MindGateButton(
                                text = "Try Again",
                                onClick = { launcher.launch("*/*") },
                                style = ButtonStyle.Brand,
                                icon = Icons.Default.Refresh
                            )
                        }
                    }
                    is TeacherUiState.Success -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(AppIcons.Approved, null, tint = ActionSuccess, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Dataset Analysis Complete!", fontWeight = FontWeight.Black, color = ActionSuccess)
                            Spacer(Modifier.height(32.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MindGateButton(
                                    text = "Results",
                                    onClick = onNavigateToResults,
                                    style = ButtonStyle.Success,
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Visibility
                                )
                                MindGateButton(
                                    text = "Reset",
                                    onClick = { launcher.launch("*/*") },
                                    style = ButtonStyle.Neutral,
                                    isSecondary = true,
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Delete
                                )
                            }
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MindGateButton(
                                text = "Upload Spreadsheet",
                                onClick = { launcher.launch("*/*") },
                                style = ButtonStyle.Brand,
                                icon = Icons.Default.CloudUpload,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(Modifier.height(16.dp))
                            
                            MindGateButton(
                                text = "Download CSV Format Guide",
                                onClick = { exportLauncher.launch("mindgate_template.csv") },
                                modifier = Modifier.fillMaxWidth(),
                                style = ButtonStyle.Neutral,
                                isSecondary = true,
                                icon = Icons.Default.Download
                            )
                        }
                    }
                }
            }
        }
    }
}
