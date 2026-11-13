package com.mindgate.presentation.ui.screens.overlay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import com.mindgate.R
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.quiz.QuizPuzzleActivity
import com.mindgate.presentation.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockedOverlayActivity : ComponentActivity() {
    
    private val viewModel: BlockedOverlayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val packageName = intent.getStringExtra("PACKAGE_NAME") ?: ""
        val appName = intent.getStringExtra("APP_NAME") ?: "App"

        viewModel.loadEligibility()
        viewModel.observeLimitStatus(packageName)

        setContent {
            MindGateTheme {
                val state by viewModel.state.collectAsState()

                if (state.extensionGranted || state.isUnblockedManually) {
                    // Logic to handle auto-finish or show granted state
                    // Reusing logic: for simplicity just finish here if granted
                    finish()
                }

                BlockedOverlayScreen(
                    appName = appName,
                    packageName = packageName,
                    uiState = state,
                    onRequestExtension = {
                        startActivity(Intent(this, QuizPuzzleActivity::class.java).apply {
                            putExtra("PACKAGE_NAME", packageName)
                            putExtra("APP_NAME", appName)
                            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        })
                        finish()
                    },
                    onUnlockViaStudy = {
                        viewModel.unlockViaStudy(packageName)
                    },
                    onCloseApp = {
                        goToHome()
                    }
                )
            }
        }
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
    packageName: String,
    uiState: BlockedOverlayUiState,
    onRequestExtension: () -> Unit,
    onUnlockViaStudy: () -> Unit,
    onCloseApp: () -> Unit
) {
    val context = LocalContext.current
    
    // Grayscale matrix
    val matrix = remember { ColorMatrix().apply { setToSaturation(0f) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlockedBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon with red circle behind it
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Danger.copy(alpha = 0.2f), CircleShape)
                        .border(2.dp, Danger.copy(alpha = 0.5f), CircleShape)
                )
                
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(packageName)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(Shapes.small),
                    colorFilter = ColorFilter.colorMatrix(matrix)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Lottie pulsing lock
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lock_pulse))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(120.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Time's Up",
                style = MaterialTheme.typography.headlineLarge,
                color = SurfaceCard,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "$appName has reached its daily limit",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Extension dots
            ExtensionsRemainingDots(used = 2) // Hardcoded for UI demo, should be dynamic

            Spacer(Modifier.height(48.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(color = Primary)
            } else {
                val eligibility = uiState.eligibility
                
                // Option 1: Study Unlock (If eligible)
                if (eligibility?.studyUnlockAvailable == true) {
                    MindGateButton(
                        text = "Unlocked via study time ✓",
                        onClick = onUnlockViaStudy,
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.Success,
                        icon = Icons.Default.CheckCircle
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Option 2: Solve Challenge
                MindGateButton(
                    text = if (eligibility?.studyUnlockAvailable == true) "Or Solve a Puzzle" else "Solve a Puzzle",
                    onClick = onRequestExtension,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Warning
                )

                if (eligibility != null && !eligibility.studyUnlockAvailable) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Study ${eligibility.targetMinutes - eligibility.studyMinutesToday}m more to unlock automatically",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            MindGateButton(
                text = "Go Back",
                onClick = onCloseApp,
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Neutral,
                isSecondary = true
            )
        }
    }
}

@Composable
fun ExtensionsRemainingDots(used: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val isUsed = i < used
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        if (isUsed) Primary else Color.Transparent,
                        CircleShape
                    )
                    .border(1.5.dp, Primary, CircleShape)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Extensions remaining",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
