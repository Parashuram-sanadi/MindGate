package com.mindgate.presentation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),    // Buttons, Small Cards
    medium = RoundedCornerShape(20.dp),   // Main Cards
    large = RoundedCornerShape(28.dp),    // Dashboards, Overlays
    extraLarge = RoundedCornerShape(36.dp)
)
