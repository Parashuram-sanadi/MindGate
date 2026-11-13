package com.mindgate.presentation.ui.blocked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mindgate.presentation.ui.theme.MindGateTheme
import kotlinx.coroutines.delay

class ExtensionGrantedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appName = intent.getStringExtra("app_name") ?: "App"
        setContent {
            MindGateTheme {
                ExtensionTimerScreen(appName) { finish() }
            }
        }
    }
}

@Composable
fun ExtensionTimerScreen(appName: String, onDone: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(15 * 60) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        onDone()
    }

    val color = if (secondsLeft <= 120) Color.Red else MaterialTheme.colorScheme.primary
    val mins = secondsLeft / 60
    val secs = secondsLeft % 60

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Extension Granted", style = MaterialTheme.typography.titleLarge)
        Text("You may use $appName for:", style = MaterialTheme.typography.bodyLarge)
        Text("%02d:%02d".format(mins, secs), style = MaterialTheme.typography.displayLarge, color = color)
        if (secondsLeft == 120) Text("2 minutes left — wrap up")
    }
}
