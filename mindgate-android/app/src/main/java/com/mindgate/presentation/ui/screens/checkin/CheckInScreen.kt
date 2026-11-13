package com.mindgate.presentation.ui.screens.checkin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.presentation.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInScreen(
    onComplete: () -> Unit,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    var selectedMood by remember { mutableIntStateOf(0) }
    var selectedReason by remember { mutableStateOf("") }
    var reflectionResponse by remember { mutableStateOf("") }
    
    var isSkippable by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000)
        isSkippable = true
    }

    Scaffold(
        containerColor = Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Quick Check-in",
                style = MaterialTheme.typography.headlineLarge,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(32.dp))

            // Mood Scale
            Text("How are you feeling?", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val emojis = listOf("😢", "😟", "😐", "🙂", "🤩")
                emojis.forEachIndexed { index, emoji ->
                    val moodValue = index + 1
                    IconButton(
                        onClick = { selectedMood = moodValue },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Text(
                            emoji, 
                            fontSize = if (selectedMood == moodValue) 40.sp else 32.sp,
                            modifier = Modifier.alpha(if (selectedMood == 0 || selectedMood == moodValue) 1f else 0.5f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Reason Chips
            Text("Why are you opening your device?", style = MaterialTheme.typography.titleMedium)
            val reasons = listOf("Boredom", "Habit", "Need", "Avoiding something", "Other")
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                reasons.forEach { reason ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = { Text(reason) },
                        modifier = Modifier.padding(4.dp),
                        shape = Shapes.large
                    )
                }
            }

            if (state.showDailyReflection) {
                Spacer(Modifier.height(32.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryContainer.copy(alpha = 0.3f)),
                    shape = Shapes.medium
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Reflection",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(state.reflectionPrompt, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = reflectionResponse,
                            onValueChange = { reflectionResponse = it },
                            placeholder = { Text("Your thoughts...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { 
                    viewModel.submit(
                        selectedMood, 
                        selectedReason, 
                        if (state.showDailyReflection) reflectionResponse else null,
                        onComplete
                    ) 
                },
                enabled = selectedMood > 0 && selectedReason.isNotBlank() && !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = Shapes.small
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = SurfaceCard, modifier = Modifier.size(24.dp))
                } else {
                    Text("Continue to Home")
                }
            }

            if (isSkippable) {
                TextButton(onClick = onComplete) {
                    Text("Skip for now", color = TextSecondary)
                }
            }
        }
    }
}
