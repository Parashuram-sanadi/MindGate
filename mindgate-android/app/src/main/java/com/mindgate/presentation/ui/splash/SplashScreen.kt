package com.mindgate.presentation.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mindgate.R

@Composable
fun SplashScreen(
    destination: String?,
    onNavigate: (String) -> Unit
) {
    var scale by remember { mutableFloatStateOf(0.5f) }
    var alpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(0.5f, 1f, animationSpec = tween(800)) { value, _ -> scale = value }
        animate(0f, 1f, animationSpec = tween(800)) { value, _ -> alpha = value }
    }

    LaunchedEffect(destination) {
        destination?.let { onNavigate(it) }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_mindgate_logo),
                contentDescription = "MindGate",
                modifier = Modifier.size(120.dp).scale(scale).alpha(alpha)
            )
            Spacer(Modifier.height(16.dp))
            Text("MindGate", style = MaterialTheme.typography.displayLarge, modifier = Modifier.alpha(alpha))
            Text("Digital Wellness", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.alpha(alpha))
        }
    }
}
