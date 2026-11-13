package com.mindgate.presentation.ui.quiz

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mindgate.domain.model.ChallengeType
import com.mindgate.presentation.ui.blocked.BlockedOverlayActivity
import com.mindgate.util.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*

@Composable
fun QuizChallengeScreen(
    appName: String,
    state: QuizUiState.ChallengeActive,
    viewModel: QuizPuzzleViewModel,
    onGoHome: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(state.timerSeconds) }
    
    LaunchedEffect(key1 = state.challenge) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        viewModel.onTimerExpired()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wellness Challenge",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = TextPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Quiz, null, modifier = Modifier.size(16.dp), tint = TextSecondary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Solve to unlock 15m for $appName",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Timer Area with Pulse
        val timerProgress = timeLeft.toFloat() / state.timerSeconds
        val timerColor = when {
            timeLeft < 10 -> ActionDanger
            timeLeft < 20 -> ActionWarning
            else -> ActionSuccess
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { timerProgress },
                    modifier = Modifier.size(100.dp),
                    color = timerColor,
                    strokeWidth = 8.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    trackColor = timerColor.copy(alpha = 0.1f)
                )
                Text(
                    text = "$timeLeft",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = timerColor
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Seconds Left", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Attempts Track
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { i ->
                val active = i < state.attemptsLeft
                Surface(
                    modifier = Modifier.size(width = 40.dp, height = 6.dp),
                    shape = CircleShape,
                    color = if (active) ActionPrimary else Outline,
                    shadowElevation = if (active) 2.dp else 0.dp
                ) {}
            }
        }
        
        Spacer(Modifier.height(40.dp))
        
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            AnimatedContent(
                targetState = state.challenge.type,
                transitionSpec = {
                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                },
                label = "challenge_type"
            ) { type ->
                when (type) {
                    ChallengeType.MATH -> MathChallenge(state, viewModel)
                    ChallengeType.TRIVIA -> TriviaChallenge(state, viewModel)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))

        MindGateButton(
            text = "I give up",
            onClick = onGoHome,
            style = ButtonStyle.Neutral,
            isSecondary = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MathChallenge(state: QuizUiState.ChallengeActive, viewModel: QuizPuzzleViewModel) {
    var answer by remember { mutableStateOf("") }
    val shakeOffset = remember { Animatable(0f) }
    
    LaunchedEffect(state.lastAnswerWrong) {
        if (state.lastAnswerWrong) {
            repeat(3) {
                shakeOffset.animateTo(6f, tween(50, easing = LinearEasing))
                shakeOffset.animateTo(-6f, tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, tween(50, easing = LinearEasing))
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(x = shakeOffset.value.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PrimaryContainer.copy(alpha = 0.5f),
            shape = Shapes.medium
        ) {
            Text(
                text = state.challenge.question,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                color = Primary
            )
        }
        
        Spacer(Modifier.height(20.dp))
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = Shapes.small,
            color = Surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = if (state.lastAnswerWrong) ActionDanger else Primary.copy(alpha = 0.3f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (answer.isEmpty()) {
                    Text("Type Answer", color = TextSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = answer,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (state.lastAnswerWrong) ActionDanger else Primary
                )
            }
        }
        
        if (state.lastAnswerWrong) {
            Text(
                "Try again! ${state.attemptsLeft} chances left", 
                color = ActionDanger, 
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp),
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        NumberPad(
            onNumberClick = { if (answer.length < 5) answer += it },
            onDeleteClick = { if (answer.isNotEmpty()) answer = answer.dropLast(1) }
        )
        
        Spacer(Modifier.height(24.dp))
        
        MindGateButton(
            text = "Submit Answer",
            onClick = { viewModel.submitAnswer(answer) },
            enabled = answer.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Brand,
            icon = Icons.Default.Check
        )
    }
}

@Composable
fun NumberPad(onNumberClick: (String) -> Unit, onDeleteClick: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(280.dp)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { item ->
                    if (item.isNotEmpty()) {
                        Surface(
                            onClick = { if (item == "DEL") onDeleteClick() else onNumberClick(item) },
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.weight(1f).height(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (item == "DEL") {
                                    Icon(Icons.Default.Backspace, null, modifier = Modifier.size(24.dp), tint = Primary)
                                } else {
                                    Text(item, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Primary)
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun TriviaChallenge(state: QuizUiState.ChallengeActive, viewModel: QuizPuzzleViewModel) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ActionWarning.copy(alpha = 0.08f),
            shape = Shapes.medium,
            border = androidx.compose.foundation.BorderStroke(1.dp, ActionWarning.copy(alpha = 0.2f))
        ) {
            Text(
                text = state.challenge.question,
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = TextPrimary
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        state.challenge.options.forEach { option ->
            val isSelected = selectedOption == option
            Surface(
                onClick = { selectedOption = option },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = Shapes.medium,
                color = if (isSelected) Primary.copy(alpha = 0.1f) else Surface,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Primary else Outline
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = Primary)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                        color = if (isSelected) Primary else TextPrimary
                    )
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        MindGateButton(
            text = "Submit Answer",
            onClick = { selectedOption?.let { viewModel.submitAnswer(it) } },
            enabled = selectedOption != null,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Brand,
            icon = Icons.Default.Done
        )
    }
}

@Composable
fun ExtensionGrantedScreen(
    packageName: String,
    appName: String,
    expiresAt: Long,
    remainingToday: Int,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var remainingMs by remember { mutableLongStateOf(expiresAt - System.currentTimeMillis()) }
    
    val minutes = (remainingMs / 1000) / 60
    val seconds = (remainingMs / 1000) % 60
    val isWarning = minutes < 2

    // Background color animation
    val backgroundColor by animateColorAsState(
        targetValue = if (isWarning) WarningContainer else ActionNeutral,
        animationSpec = tween(1000),
        label = "bg_color"
    )

    // Breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween( MindGateMotion.DurationCalm / 2, easing = MindGateMotion.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        while (remainingMs > 0) {
            delay(1000)
            remainingMs = expiresAt - System.currentTimeMillis()
        }
        // Time's up - trigger block again
        val intent = Intent(context, BlockedOverlayActivity::class.java).apply {
            putExtra(Constants.EXTRA_PACKAGE_NAME, packageName)
            putExtra(Constants.EXTRA_APP_NAME, appName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        context.startActivity(intent)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Warning Banner
        AnimatedVisibility(
            visible = isWarning,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = ActionWarning),
                shape = Shapes.medium
            ) {
                Text(
                    text = "2 minutes left — start wrapping up ⚠️",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Breathing circle
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(breathingScale)
                        .background(
                            if (isWarning) ActionWarning.copy(alpha = 0.2f) else ActionSuccess.copy(alpha = 0.2f),
                            CircleShape
                        )
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%02d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 64.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        ),
                        color = if (isWarning) ActionWarning else ActionSuccess
                    )
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Extension status info
            Text(
                text = "Extension Unlocked!",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Approved, null, tint = ActionSuccess, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Great job! You earned extra time.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ActionSuccess,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
            Text(
                text = "Access available until ${timeFormat.format(java.util.Date(expiresAt))}.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(32.dp))

            MindGateButton(
                text = "Open $appName",
                onClick = {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                    onFinish()
                },
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Success
            )
        }
    }
}
