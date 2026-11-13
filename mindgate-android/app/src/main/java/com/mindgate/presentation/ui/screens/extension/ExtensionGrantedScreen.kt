package com.mindgate.presentation.ui.screens.extension

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindgate.presentation.ui.blocked.BlockedOverlayActivity
import com.mindgate.presentation.ui.theme.*
import kotlinx.coroutines.delay

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
    val isWarning = minutes < 2

    // Background color animation
    val backgroundColor by animateColorAsState(
        targetValue = if (isWarning) WarningContainer else PrimaryContainer,
        animationSpec = tween(1000),
        label = "bg_color"
    )

    // Breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        while (remainingMs > 0) {
            delay(1000)
            remainingMs = expiresAt - System.currentTimeMillis()
        }
        // Expiry logic
        val intent = Intent(context, BlockedOverlayActivity::class.java).apply {
            putExtra("PACKAGE_NAME", packageName)
            putExtra("APP_NAME", appName)
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
                colors = CardDefaults.cardColors(containerColor = Warning),
                shape = Shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2 minutes left — start wrapping up ⚠️",
                        color = SurfaceCard,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                            if (isWarning) Warning.copy(alpha = 0.2f) else Success.copy(alpha = 0.2f),
                            CircleShape
                        )
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(remainingMs),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 64.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        ),
                        color = if (isWarning) Warning else Primary
                    )
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            Text(
                text = "15 Minutes Granted",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Badge
            Surface(
                color = SurfaceCard,
                shape = CircleShape,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "Extensions used today: ${3 - remainingToday} / 3",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(64.dp))

            Button(
                onClick = {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                    onFinish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = Shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = "Open $appName",
                    style = MaterialTheme.typography.titleMedium,
                    color = SurfaceCard
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
