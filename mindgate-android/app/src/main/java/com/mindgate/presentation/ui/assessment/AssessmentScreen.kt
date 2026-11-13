package com.mindgate.presentation.ui.assessment

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.presentation.ui.components.AssessmentQuestionCard
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.LikertSegmentedControl
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentScreen(
    onComplete: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: AssessmentViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showInfo by remember { mutableStateOf(false) }

    // Reset scroll position to top when navigating between assessment sections
    LaunchedEffect(state.currentStep) {
        scrollState.animateScrollTo(0)
    }

    if (state.isAnalyzing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.mindgate.presentation.ui.theme.Surface),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(40.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(72.dp),
                    color = Primary,
                    strokeWidth = 6.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Analyzing Wellness Data",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = TextPrimary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Our AI model is calculating your risk level and generating personalized recommendations.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        }
        return
    }

    if (showInfo) {
        ModalBottomSheet(
            onDismissRequest = { showInfo = false },
            containerColor = SurfaceCard,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Outline) }
        ) {
            Column(Modifier.padding(horizontal = 32.dp, vertical = 24.dp)) {
                Text(
                    "Methodology",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = TextPrimary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "MindGate uses a machine learning model to analyze your responses across 5 key areas: usage patterns, physical health, academic impact, psychological state, and control.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 24.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Your data is processed locally and securely to provide personalized digital wellness insights.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 24.sp
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Self Assessment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onProfileClick) {
                        Icon(AppIcons.Profile, contentDescription = "Profile", tint = TextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.mindgate.presentation.ui.theme.Surface)
            )
        },
        containerColor = Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Step Progress Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceCard,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { state.currentStep / 5f },
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(CircleShape),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        color = Primary,
                        trackColor = Outline.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Surface(
                        color = PrimaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${state.currentStep}/5",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Animated Content for Question Steps
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = state.currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }.using(SizeTransform(clip = false))
                    },
                    label = "assessment_steps"
                ) { step ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AssessmentQuestions.forStep(step).forEach { q ->
                            key(q.id) {
                                AssessmentQuestionCard(q.text) {
                                    QuestionInput(q, state.answers.get(q.id), viewModel::setAnswer, state.answers)
                                }
                            }
                        }
                        
                        if (state.error != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Danger.copy(alpha = 0.1f),
                                shape = Shapes.small
                            ) {
                                Text(
                                    state.error!!, 
                                    color = Danger, 
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }

            // Navigation Buttons
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceCard,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.currentStep > 1) {
                        MindGateButton(
                            text = "Back",
                            onClick = viewModel::prevStep,
                            modifier = Modifier.weight(1f),
                            style = ButtonStyle.Neutral,
                            isSecondary = true,
                            icon = AppIcons.Back
                        )
                    }
                    
                    MindGateButton(
                        text = if (state.currentStep < 5) "Next Step" else "Complete",
                        onClick = {
                            if (state.currentStep < 5) viewModel.nextStep()
                            else viewModel.submit(onComplete)
                        },
                        enabled = viewModel.isStepValid(state.currentStep) && !state.isSubmitting,
                        modifier = Modifier.weight(1.5f),
                        style = ButtonStyle.Primary,
                        isLoading = state.isSubmitting,
                        icon = if (state.currentStep < 5) AppIcons.Forward else AppIcons.Approved
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionInput(
    q: AssessmentQuestion,
    value: String?,
    onChange: (String, String) -> Unit,
    allAnswers: com.mindgate.domain.model.AssessmentAnswers
) {
    when (q.type) {
        QuestionType.DROPDOWN, QuestionType.RADIO -> {
            q.options.forEach { opt ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = value == opt, onClick = { onChange(q.id, opt) })
                    Text(opt)
                }
            }
        }
        QuestionType.LIKERT -> LikertSegmentedControl(
            value = value?.toIntOrNull(),
            onValueChange = { onChange(q.id, it.toString()) }
        )
        QuestionType.TOGGLE -> Switch(
            checked = value == "true",
            onCheckedChange = { onChange(q.id, it.toString()) }
        )
        QuestionType.SLIDER -> {
            val range = when (q.id) {
                "sleep_hours" -> 0f..24f
                else -> 0f..10f
            }
            val fVal = value?.toFloatOrNull() ?: if (q.id == "sleep_hours") 7f else range.start
            Slider(fVal, { onChange(q.id, it.toString()) }, valueRange = range, steps = (range.endInclusive - range.start).toInt() * 2)
            Text("${"%.1f".format(fVal)} hours")
        }
        QuestionType.MULTI_SELECT -> {
            val selected = value?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                q.options.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { opt ->
                            FilterChip(
                                selected = selected.contains(opt),
                                onClick = {
                                    val newSet = selected.toMutableSet()
                                    if (newSet.contains(opt)) newSet.remove(opt)
                                    else newSet.add(opt)
                                    onChange(q.id, newSet.joinToString(","))
                                },
                                label = { Text(opt) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        QuestionType.READONLY -> {
            Text("Auto-filled based on your current status", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
