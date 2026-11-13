package com.mindgate.presentation.ui.screens.challenge

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindgate.presentation.ui.quiz.QuizPuzzleViewModel
import com.mindgate.presentation.ui.quiz.QuizUiState
import com.mindgate.presentation.ui.theme.*

@Composable
fun TriviaChallengeScreen(
    state: QuizUiState.ChallengeActive,
    viewModel: QuizPuzzleViewModel
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }
    val view = LocalView.current
    
    LaunchedEffect(state.lastAnswerWrong) {
        if (state.lastAnswerWrong) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            repeat(4) {
                shakeOffset.animateTo(16f, tween(50, easing = LinearEasing))
                shakeOffset.animateTo(-16f, tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, tween(50, easing = LinearEasing))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Attempt Indicator
            AttemptIndicator(attemptsLeft = state.attemptsLeft)
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                text = state.challenge.question,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(Modifier.height(32.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.offset(x = shakeOffset.value.dp)
            ) {
                state.challenge.options.forEach { option ->
                    val isSelected = selectedOption == option
                    
                    Surface(
                        onClick = { selectedOption = option },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        color = if (isSelected) PrimaryContainer else Surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Primary else Outline
                        )
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) Primary else TextPrimary
                        )
                    }
                }
            }
            
            if (state.lastAnswerWrong) {
                Text(
                    text = "Incorrect! ${state.attemptsLeft} attempts left",
                    color = Danger,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            Spacer(Modifier.height(40.dp))
            
            Button(
                onClick = { selectedOption?.let { viewModel.submitAnswer(it) } },
                enabled = selectedOption != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = Shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Confirm", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
