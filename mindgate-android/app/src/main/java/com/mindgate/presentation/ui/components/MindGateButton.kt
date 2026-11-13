package com.mindgate.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindgate.presentation.ui.theme.*

enum class ButtonStyle { Primary, Success, Danger, Warning, Neutral, Calm, Brand }

@Composable
fun MindGateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    isSecondary: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Scale animation for tactile feedback
    val scale by MindGateMotion.tapScale(isPressed && enabled && !isLoading)

    // Base colors based on style
    val baseColors = getBaseColors(style)
    
    // Animate background color:
    // If not secondary: Solid style color (default) -> Darker/Same (pressed)
    // If secondary: Transparent (default) -> Solid style color (pressed)
    val targetContainerColor = when {
        !enabled -> ActionDisabled
        isPressed -> baseColors.container
        isSecondary -> Color.Transparent
        else -> baseColors.container
    }
    
    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(MindGateMotion.DurationTap),
        label = "button_bg_anim"
    )

    // Animate content color:
    // If not secondary: White (default) -> White (pressed)
    // If secondary: Style color (default) -> White (pressed)
    val targetContentColor = when {
        !enabled -> ActionDisabledText
        isPressed -> if (style == ButtonStyle.Neutral) TextPrimary else Color.White
        isSecondary -> {
            if (style == ButtonStyle.Neutral) TextPrimary else baseColors.container
        }
        else -> Color.White
    }
    
    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(MindGateMotion.DurationTap),
        label = "button_content_anim"
    )

    val shape = if (style == ButtonStyle.Brand) Shapes.medium else Shapes.small

    // Using Surface instead of Button to avoid default Material 3 background/striping issues
    Surface(
        onClick = { if (!isLoading && enabled) onClick() },
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .clip(shape),
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = if (enabled) BorderStroke(
            width = 1.5.dp,
            color = if (isPressed) Color.Transparent else baseColors.container
        ) else BorderStroke(1.dp, ActionDisabled),
        // Disable elevation for secondary/transparent state to avoid artifacting
        shadowElevation = if (!isSecondary && enabled && !isPressed && containerColor != Color.Transparent) 2.dp else 0.dp,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            ButtonContent(text, icon, iconPainter, isLoading, contentColor)
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    iconPainter: Painter?,
    isLoading: Boolean,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.background(Color.Transparent)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.5.dp
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = contentColor)
                Spacer(Modifier.width(10.dp))
            } else if (iconPainter != null) {
                Icon(iconPainter, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Unspecified)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text, 
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    background = Color.Transparent
                )
            )
        }
    }
}

private data class SemanticColors(val container: Color, val content: Color)

@Composable
private fun getBaseColors(style: ButtonStyle): SemanticColors {
    return when (style) {
        ButtonStyle.Primary -> SemanticColors(ActionPrimary, Color.White)
        ButtonStyle.Success -> SemanticColors(ActionSuccess, Color.White)
        ButtonStyle.Danger -> SemanticColors(ActionDanger, Color.White)
        ButtonStyle.Warning -> SemanticColors(ActionWarning, Color.White)
        ButtonStyle.Neutral -> SemanticColors(Outline, TextPrimary)
        ButtonStyle.Calm -> SemanticColors(ActionCalm, Color.White)
        ButtonStyle.Brand -> SemanticColors(Primary, Color.White)
    }
}
