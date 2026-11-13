package com.mindgate.presentation.ui.blocked

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.quiz.QuizPuzzleActivity
import com.mindgate.presentation.ui.theme.*
import com.mindgate.util.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockedOverlayActivity : ComponentActivity() {

    private val viewModel: BlockedOverlayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val packageName = intent.getStringExtra(Constants.EXTRA_PACKAGE_NAME) ?: ""
        val appName = intent.getStringExtra(Constants.EXTRA_APP_NAME) ?: "App"
        val limitMinutes = intent.getIntExtra("LIMIT_MINUTES", 0)

        viewModel.init(packageName, appName)

        setContent {
            MindGateTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                if (uiState.extensionUntil > System.currentTimeMillis()) {
                    LaunchedEffect(Unit) {
                        finish()
                    }
                }

                BlockedOverlayScreen(
                    appName = appName,
                    limitMinutes = if (uiState.dailyLimitMinutes > 0) uiState.dailyLimitMinutes else limitMinutes,
                    state = uiState,
                    onSolveQuiz = {
                        startActivity(Intent(this, QuizPuzzleActivity::class.java).apply {
                            putExtra(Constants.EXTRA_PACKAGE_NAME, packageName)
                            putExtra(Constants.EXTRA_APP_NAME, appName)
                            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        })
                        finish()
                    },
                    onAskParent = {
                        viewModel.requestFromParent()
                    },
                    onCloseApp = {
                        goToHome()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        goToHome()
    }

    private fun goToHome() {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        finish()
    }
}

@Composable
fun BlockedOverlayScreen(
    appName: String,
    limitMinutes: Int,
    state: BlockedUiState,
    onSolveQuiz: () -> Unit,
    onAskParent: () -> Unit,
    onCloseApp: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lock_pulse")
    val lockScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(MindGateMotion.DurationWarning, easing = MindGateMotion.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // High-Impact Lock Icon
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .scale(lockScale),
                shape = CircleShape,
                color = Danger.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(2.dp, ActionDanger.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.Locked,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = ActionDanger
                    )
                }
            }
            
            Spacer(Modifier.height(40.dp))
            
            Text(
                text = "Time's up for today",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "You've reached your $limitMinutes minute limit for $appName. Reclaim your focus! 🌿",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(Modifier.height(56.dp))

            // Adaptive Action Area
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.TopCenter) {
                AnimatedContent(
                    targetState = state.status?.lowercase(),
                    transitionSpec = {
                        fadeIn(tween(400)) + slideInVertically { it / 2 } togetherWith
                        fadeOut(tween(400)) + slideOutVertically { -it / 2 }
                    },
                    label = "blocked_actions"
                ) { status ->
                    when (status) {
                        "pending" -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = ActionWarning.copy(alpha = 0.1f),
                                    shape = Shapes.medium,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ActionWarning.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = ActionWarning,
                                            strokeWidth = 3.dp
                                        )
                                        Spacer(Modifier.width(16.dp))
                                        Text(
                                            "Awaiting Approval...",
                                            color = ActionWarning,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Your parent has been notified.",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        "denied" -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = ActionDanger.copy(alpha = 0.1f),
                                    shape = Shapes.medium
                                ) {
                                    Text(
                                        "Request denied. Try a quiz instead!",
                                        color = ActionDanger,
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(24.dp))
                                MindGateButton(
                                    text = "Solve Quiz",
                                    onClick = onSolveQuiz,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = ButtonStyle.Primary,
                                    icon = AppIcons.Quiz
                                )
                            }
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                MindGateButton(
                                    text = "Solve Wellness Quiz",
                                    onClick = onSolveQuiz,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = ButtonStyle.Primary,
                                    icon = AppIcons.Quiz
                                )
                                
                                MindGateButton(
                                    text = "Ask Parent for Time",
                                    onClick = onAskParent,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = ButtonStyle.Neutral,
                                    isSecondary = true,
                                    isLoading = state.isLoading,
                                    icon = AppIcons.Family
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            MindGateButton(
                text = "Close App",
                onClick = onCloseApp,
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Neutral,
                isSecondary = true
            )

            if (state.error != null) {
                Surface(
                    modifier = Modifier.padding(top = 24.dp),
                    color = Danger.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Text(
                        text = state.error,
                        color = ActionDanger,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
