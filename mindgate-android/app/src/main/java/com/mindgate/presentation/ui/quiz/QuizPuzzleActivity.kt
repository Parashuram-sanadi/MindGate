package com.mindgate.presentation.ui.quiz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*
import com.mindgate.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class QuizPuzzleActivity : ComponentActivity() {

    private val viewModel: QuizPuzzleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val packageName = intent.getStringExtra(Constants.EXTRA_PACKAGE_NAME) ?: ""
        val appName = intent.getStringExtra(Constants.EXTRA_APP_NAME) ?: "App"
        
        viewModel.init(packageName, appName)

        setContent {
            MindGateTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = uiState,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "quiz_state_transition"
                    ) { state ->
                        when (state) {
                            is QuizUiState.Loading -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = ActionPrimary)
                                }
                            }
                            is QuizUiState.Calming -> {
                                CalmScreen(onFinished = { viewModel.onCalmFinished() })
                            }
                            is QuizUiState.ChallengeActive -> {
                                QuizChallengeScreen(
                                    appName = appName,
                                    state = state,
                                    viewModel = viewModel,
                                    onGoHome = { goToHome() }
                                )
                            }
                            is QuizUiState.Passed -> {
                                ExtensionGrantedScreen(
                                    packageName = packageName,
                                    appName = appName,
                                    expiresAt = state.expiresAt,
                                    remainingToday = state.remainingToday,
                                    onFinish = { finish() }
                                )
                            }
                            is QuizUiState.QuizPassedWaitingSubmit -> {
                                QuizPassedSubmitScreen(
                                    appName = appName,
                                    onSubmit = { viewModel.submitExtensionRequest() },
                                    onCancel = { goToHome() }
                                )
                            }
                            is QuizUiState.PendingApproval -> {
                                PendingApprovalScreen(
                                    appName = appName,
                                    onCancel = { goToHome() }
                                )
                            }
                            is QuizUiState.PendingApprovalFirestore -> {
                                PendingApprovalScreen(
                                    appName = appName,
                                    isFirestore = true,
                                    onCancel = { goToHome() }
                                )
                            }
                            is QuizUiState.Failed -> {
                                QuizFailedScreen(
                                    reason = state.reason,
                                    onGoHome = { goToHome() }
                                )
                            }
                            is QuizUiState.DailyCapReached -> {
                                DailyCapScreen(
                                    cap = state.cap,
                                    onGoHome = { goToHome() }
                                )
                            }
                        }
                    }
                }
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
fun CalmScreen(onFinished: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    var secondsLeft by remember { mutableIntStateOf(15) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
            progress = (15 - secondsLeft) / 15f
        }
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Take a Breath", style = MaterialTheme.typography.headlineLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = ActionCalm)
        Spacer(Modifier.height(16.dp))
        Text("Pause for a moment before the challenge.", textAlign = TextAlign.Center)
        
        Spacer(Modifier.height(48.dp))
        
        Box(contentAlignment = Alignment.Center) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween( MindGateMotion.DurationCalm / 2, easing = MindGateMotion.EaseInOut), RepeatMode.Reverse),
                label = "scale"
            )
            
            Box(
                Modifier.size(200.dp).graphicsLayer(scaleX = scale, scaleY = scale).background(ActionCalm.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
            )
            Text("${secondsLeft}s", style = MaterialTheme.typography.displayMedium, color = ActionCalm)
        }
        
        Spacer(Modifier.height(48.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = ActionCalm)
    }
}

@Composable
fun QuizPassedSubmitScreen(appName: String, onSubmit: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = ActionSuccess
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Challenge Passed!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = ActionSuccess
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "You've successfully completed the challenge. Would you like to request 15 more minutes of $appName from your parent?",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        
        MindGateButton(
            text = "Submit Request to Parent",
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Brand
        )
        
        Spacer(Modifier.height(16.dp))
        
        MindGateButton(
            text = "Cancel & Go Home",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Neutral,
            isSecondary = true
        )
    }
}

@Composable
fun PendingApprovalScreen(appName: String, isFirestore: Boolean = false, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp), color = ActionWarning)
        Spacer(Modifier.height(32.dp))
        Text(
            text = if (isFirestore) "Awaiting Parent Approval" else "Approval Pending",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = ActionWarning
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isFirestore)
                "Your request for 15 more minutes of $appName has been sent to your parent. This screen will update automatically once they respond."
            else
                "You've reached your daily extension limit. A request has been sent to your parent to allow 15 more minutes for $appName.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        
        MindGateButton(
            text = "Cancel & Go Home",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Neutral,
            isSecondary = true
        )
    }
}

@Composable
fun QuizFailedScreen(reason: String, onGoHome: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Challenge Failed",
            style = MaterialTheme.typography.headlineMedium,
            color = ActionDanger
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        
        MindGateButton(
            text = "Go to Home Screen",
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Neutral,
            isSecondary = true
        )
    }
}

@Composable
fun DailyCapScreen(cap: Int, onGoHome: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Daily Limit Reached",
            style = MaterialTheme.typography.headlineMedium,
            color = ActionWarning
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "You've already used your $cap extensions for today. Time to focus on something else! 🌟",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        
        MindGateButton(
            text = "Go to Home Screen",
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Neutral,
            isSecondary = true
        )
    }
}
