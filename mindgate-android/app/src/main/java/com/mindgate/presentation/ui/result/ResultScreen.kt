package com.mindgate.presentation.ui.result

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*
import com.mindgate.util.riskColor
import com.mindgate.util.riskExplanation

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onSetLimits: () -> Unit,
    onProfileClick: () -> Unit,
    onBackToHome: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    
    BackHandler(onBack = onBackToHome)

    if (com.mindgate.BuildConfig.DEBUG) android.util.Log.d("MINDGATE_RESULT", "UiState received: $result")
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (result == null) {
        Box(Modifier.fillMaxSize().background(com.mindgate.presentation.ui.theme.Surface), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val r = result!!
    val color = riskColor(r.riskLevel)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis Result", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(AppIcons.Back, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(AppIcons.Profile, contentDescription = "Profile", tint = TextSecondary)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (r.fromCache) {
                Surface(
                    color = ActionNeutral,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        "OFFLINE RESULT", 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = TextSecondary
                    )
                }
            }

            // High Impact Score Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = color.copy(alpha = if (r.riskLevel.equals("Severe", true)) pulseAlpha else 0.15f),
                shape = Shapes.medium,
                border = androidx.compose.foundation.BorderStroke(2.dp, color.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ADDICTION RISK", 
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.2.sp), 
                        color = color,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = r.riskLevel.uppercase(), 
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black), 
                        color = color
                    )
                    Spacer(Modifier.height(24.dp))
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f }, 
                            modifier = Modifier.size(140.dp), 
                            color = color.copy(alpha = 0.1f),
                            strokeWidth = 12.dp,
                            strokeCap = StrokeCap.Round
                        )
                        CircularProgressIndicator(
                            progress = { (r.riskScore / 100f).toFloat() }, 
                            modifier = Modifier.size(140.dp), 
                            color = color,
                            strokeWidth = 12.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${r.riskScore.toInt()}", 
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                                color = TextPrimary
                            )
                            Text(
                                text = "/ 100", 
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Enhanced Key Risk Drivers (SHAP Factors) with Horizontal Weight Bars
            if (r.topFactors.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = Shapes.medium
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.Analytics, null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Key Risk Drivers (AI Analysis)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        
                        r.topFactors.forEachIndexed { index, factor ->
                            val relativeWeight = when (index) {
                                0 -> 1.0f
                                1 -> 0.75f
                                else -> 0.55f
                            }
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = factor,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${(relativeWeight * 100).toInt()}% Impact",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = color
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { relativeWeight },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = color,
                                    trackColor = ActionNeutral,
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }

            if (r.riskLevel.equals("Severe", true)) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ActionDanger.copy(alpha = 0.1f),
                    shape = Shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ActionDanger.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Professional Support Recommended", 
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ActionDanger
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your risk level indicates significant impact. Consider reaching out to a counselor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        TextButton(
                            onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:9152987821"))) },
                            contentPadding = PaddingValues(0.dp)
                        ) { 
                            Text("Call Helpline: 9152987821", fontWeight = FontWeight.Black, color = ActionDanger) 
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Outline.copy(alpha = 0.5f))
            
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Understanding Your Risk", 
                        style = MaterialTheme.typography.titleSmall, 
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, 
                        null, 
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = riskExplanation(r.riskLevel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp),
                    lineHeight = 22.sp
                )
            }

            Text(
                "Personalized Actions", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                color = TextPrimary
            )
            
            // Category-Specific Recommendation Cards
            r.recommendations.forEach { rec ->
                val recIcon = getRecommendationIcon(rec)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), 
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = Shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = color.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(recIcon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(rec, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
            
            MindGateButton(
                text = "Apply Smart Limits",
                onClick = onSetLimits,
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Brand,
                icon = AppIcons.Extension
            )
            
            Spacer(Modifier.height(48.dp))
        }
    }
}

private fun getRecommendationIcon(rec: String): ImageVector {
    val lower = rec.lowercase()
    return when {
        lower.contains("sleep") || lower.contains("bedtime") || lower.contains("night") -> Icons.Default.Bedtime
        lower.contains("limit") || lower.contains("timer") || lower.contains("hour") || lower.contains("minute") || lower.contains("curfew") || lower.contains("lock") -> Icons.Default.HourglassTop
        lower.contains("exercise") || lower.contains("physical") || lower.contains("hobby") || lower.contains("activity") -> Icons.Default.DirectionsRun
        lower.contains("notification") || lower.contains("mute") || lower.contains("check") -> Icons.Default.NotificationsOff
        lower.contains("parent") || lower.contains("guardian") || lower.contains("mentor") -> Icons.Default.FamilyRestroom
        lower.contains("counselor") || lower.contains("counsel") || lower.contains("mental health") || lower.contains("helpline") || lower.contains("guidance") -> Icons.Default.Psychology
        lower.contains("study") || lower.contains("class") || lower.contains("lecture") || lower.contains("academic") || lower.contains("school") -> AppIcons.School
        else -> Icons.Default.CheckCircle
    }
}
