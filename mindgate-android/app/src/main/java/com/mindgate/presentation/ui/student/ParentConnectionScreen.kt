package com.mindgate.presentation.ui.student

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindgate.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*
import com.mindgate.presentation.viewmodel.ParentConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentConnectionScreen(
    onBack: () -> Unit,
    viewModel: ParentConnectionViewModel = hiltViewModel()
) {
    val code by viewModel.linkCode.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val isExpired by viewModel.isExpired.collectAsState()
    val isLinked by viewModel.isLinked.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    val successScale = remember { Animatable(0f) }
    LaunchedEffect(isLinked) {
        if (isLinked) {
            successScale.animateTo(
                targetValue = 1f,
                animationSpec = MindGateMotion.SpringSuccess
            )
        }
    }

    LaunchedEffect(Unit) {
        if (code == null) {
            viewModel.generateCode()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parent Connection", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.Back, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Surface
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLinked) {
                Surface(
                    modifier = Modifier.size(160.dp).scale(successScale.value),
                    shape = CircleShape,
                    color = ActionSuccess.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, ActionSuccess.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppIcons.Approved,
                            null,
                            modifier = Modifier.size(80.dp),
                            tint = ActionSuccess
                        )
                    }
                }
                Spacer(Modifier.height(40.dp))
                Text(
                    text = "Connection Secured", 
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = TextPrimary
                )
                Text(
                    text = "Your parent can now manage your wellness goals.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(56.dp))
                
                MindGateButton(
                    text = "Return to Profile",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Neutral,
                    isSecondary = true,
                    icon = AppIcons.Back
                )
            } else {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mindgate_logo),
                        contentDescription = "MindGate Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(Modifier.height(40.dp))
                
                Text(
                    "Parental Link", 
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = TextPrimary
                )
                Text(
                    "Share this temporary code with your parent to link your device.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
                )

                if (code != null) {
                    if (!isExpired) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SurfaceVariant,
                            shape = Shapes.medium,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = code!!,
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 4.sp,
                                        color = Primary
                                    )
                                )
                                
                                Spacer(Modifier.height(24.dp))
                                
                                MindGateButton(
                                    text = "Copy Link Code",
                                    onClick = { clipboardManager.setText(AnnotatedString(code!!)) },
                                    modifier = Modifier.wrapContentWidth(),
                                    style = ButtonStyle.Neutral,
                                    isSecondary = true,
                                    icon = Icons.Default.ContentCopy
                                )
                            }
                        }
                    } else {
                        // Expired state prominent action
                        CodeExpiredView(isLoading, onRefresh = { viewModel.generateCode() })
                    }
                    
                    if (!isExpired) {
                        Spacer(Modifier.height(32.dp))
                        ExpiryCountdown(timeLeft, isExpired)
                    }
                } else if (error != null) {
                    // Show Refresh button if initial generation failed
                    CodeExpiredView(isLoading, title = "Generation Failed", onRefresh = { viewModel.generateCode() })
                } else {
                    CircularProgressIndicator(color = Primary, strokeWidth = 4.dp)
                }
            }

            if (error != null) {
                Surface(
                    modifier = Modifier.padding(top = 24.dp),
                    color = Danger.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = error.toString(), 
                        color = ActionDanger, 
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun CodeExpiredView(isLoading: Boolean, title: String = "Code Expired", onRefresh: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Danger.copy(alpha = 0.05f),
        shape = Shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, ActionDanger.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TimerOff,
                contentDescription = null,
                tint = ActionDanger,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = ActionDanger
            )
            Spacer(Modifier.height(24.dp))
            MindGateButton(
                text = "Get New Code",
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Primary,
                isLoading = isLoading,
                icon = AppIcons.Sync
            )
        }
    }
}

@Composable
fun ExpiryCountdown(timeLeft: String?, isExpired: Boolean) {
    if (!isExpired && timeLeft != null) {
        Surface(
            color = ActionNeutral,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Timer, 
                    contentDescription = null, 
                    tint = ActionPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Expires in $timeLeft", 
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ActionPrimary
                )
            }
        }
    }
}
