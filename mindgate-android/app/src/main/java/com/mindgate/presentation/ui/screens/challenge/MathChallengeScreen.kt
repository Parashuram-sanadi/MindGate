package com.mindgate.presentation.ui.screens.challenge

import android.view.HapticFeedbackConstants
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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindgate.presentation.ui.quiz.QuizPuzzleViewModel
import com.mindgate.presentation.ui.quiz.QuizUiState
import com.mindgate.presentation.ui.theme.*

@Composable
fun MathChallengeScreen(
    state: QuizUiState.ChallengeActive,
    viewModel: QuizPuzzleViewModel
) {
    var answer by remember { mutableStateOf("") }
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
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Answer Display
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .offset(x = shakeOffset.value.dp),
                shape = Shapes.small,
                color = Surface,
                border = if (state.lastAnswerWrong) androidx.compose.foundation.BorderStroke(2.dp, Danger) else null
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (state.lastAnswerWrong) Danger else Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (state.lastAnswerWrong) {
                Text(
                    text = "Wrong! ${state.attemptsLeft} attempts left",
                    color = Danger,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Custom Number Pad
            NumberPad(
                onNumberClick = { if (answer.length < 5) answer += it },
                onDeleteClick = { if (answer.isNotEmpty()) answer = answer.dropLast(1) }
            )
            
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = { viewModel.submitAnswer(answer) },
                enabled = answer.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = Shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Submit", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun AttemptIndicator(attemptsLeft: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val isFilled = i < attemptsLeft
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (isFilled) Primary else Color.Transparent, CircleShape)
                    .border(1.5.dp, Primary, CircleShape)
            )
        }
    }
}

@Composable
fun NumberPad(onNumberClick: (String) -> Unit, onDeleteClick: () -> Unit) {
    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DEL")
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.width(280.dp),
        userScrollEnabled = false
    ) {
        items(numbers.size) { index ->
            val num = numbers[index]
            if (num.isNotEmpty()) {
                Surface(
                    onClick = { if (num == "DEL") onDeleteClick() else onNumberClick(num) },
                    shape = RoundedCornerShape(8.dp),
                    color = Surface,
                    tonalElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (num == "DEL") {
                            Icon(Icons.Default.Backspace, null, tint = TextPrimary)
                        } else {
                            Text(
                                text = num,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
    }
}
