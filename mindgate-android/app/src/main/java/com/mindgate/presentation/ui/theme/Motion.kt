package com.mindgate.presentation.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

object MindGateMotion {
    // Duration Tokens
    const val DurationTap = 100
    const val DurationSuccess = 400
    const val DurationDeny = 200
    const val DurationWarning = 600
    const val DurationCalm = 4000
    const val DurationTransition = 250

    // Curve Tokens
    val EaseOut = FastOutSlowInEasing
    val EaseInOut = LinearOutSlowInEasing
    val SpringSuccess = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Motion Templates
    @Composable
    fun tapScale(pressed: Boolean): State<Float> {
        return animateFloatAsState(
            targetValue = if (pressed) 0.96f else 1f,
            animationSpec = tween(DurationTap, easing = EaseOut),
            label = "tap_scale"
        )
    }
}
