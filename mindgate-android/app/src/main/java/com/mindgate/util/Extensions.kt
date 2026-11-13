package com.mindgate.util

import androidx.compose.ui.graphics.Color
import com.mindgate.presentation.ui.theme.Danger
import com.mindgate.presentation.ui.theme.Primary
import com.mindgate.presentation.ui.theme.Success
import com.mindgate.presentation.ui.theme.Warning

fun riskColor(level: String): Color = when (level.lowercase()) {
    "low" -> Success
    "moderate" -> Warning
    "high" -> Danger
    "severe" -> Primary
    else -> Warning
}

fun riskExplanation(level: String): String = when (level.lowercase()) {
    "low" -> "Your device use appears healthy. Keep maintaining balance between online and offline activities."
    "moderate" -> "Some dependency signs are present. Small habit changes can prevent escalation."
    "high" -> "Your usage is significantly affecting wellbeing. Structured limits and support are recommended."
    "severe" -> "Patterns suggest severe addiction. Professional support is strongly advised alongside strict limits."
    else -> "Complete the assessment for a personalised explanation."
}

fun isValidEmail(email: String): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
