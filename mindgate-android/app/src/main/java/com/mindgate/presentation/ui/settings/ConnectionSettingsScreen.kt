package com.mindgate.presentation.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.R
import com.mindgate.data.local.SessionManager
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*
import kotlinx.coroutines.launch

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSettingsScreen(
    sessionManager: SessionManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUrl by sessionManager.apiBaseUrl.collectAsStateWithLifecycle(initialValue = "")
    var urlInput by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    LaunchedEffect(currentUrl) {
        if (urlInput.isEmpty()) urlInput = currentUrl
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Configuration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.Back, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Identity
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_mindgate_logo),
                    contentDescription = "MindGate Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                "Configure Backend API URL",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "Use this to point the app to your machine's local IP (e.g., http://192.168.1.10:8000/).",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("API Base URL") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("http://10.0.2.2:8000/") },
                singleLine = true,
                shape = Shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MindGateButton(
                    text = "Save URL",
                    onClick = {
                        scope.launch {
                            val normalized = if (urlInput.endsWith("/")) urlInput else "$urlInput/"
                            sessionManager.saveApiBaseUrl(normalized)
                            testResult = "URL Saved! Connection updated successfully."
                        }
                    },
                    modifier = Modifier.weight(1f),
                    style = ButtonStyle.Brand
                )

                MindGateButton(
                    text = "Test Connection",
                    onClick = {
                        scope.launch {
                            isTesting = true
                            testResult = "Testing connection...\n• [Step 1/3] Resolving DNS..."
                            try {
                                val urlStr = if (urlInput.startsWith("http://") || urlInput.startsWith("https://")) urlInput else "http://$urlInput"
                                val url = java.net.URL(urlStr)
                                val host = url.host
                                val port = if (url.port != -1) url.port else (if (url.protocol == "https") 443 else 80)
                                
                                val ipAddress = try {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        java.net.InetAddress.getByName(host).hostAddress
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                                
                                if (ipAddress == null) {
                                    testResult = "Failed DNS resolution!\nCould not resolve host: $host\nCheck IP spelling or WiFi connection."
                                    return@launch
                                }
                                
                                testResult = "Testing connection...\n✔ DNS resolved to $ipAddress\n• [Step 2/3] Connecting TCP socket..."
                                
                                val tcpConnected = try {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        java.net.Socket().use { socket ->
                                            socket.connect(java.net.InetSocketAddress(ipAddress, port), 4000)
                                            true
                                        }
                                    }
                                } catch (e: Exception) {
                                    false
                                }
                                
                                if (!tcpConnected) {
                                    testResult = "Failed TCP connection!\nDNS resolved to $ipAddress\nCould not connect to TCP port $port.\nCheck if backend is running on this IP and port."
                                    return@launch
                                }
                                
                                testResult = "Testing connection...\n✔ DNS resolved to $ipAddress\n✔ TCP connected to port $port\n• [Step 3/3] Requesting HTTP GET /health..."
                                
                                val client = okhttp3.OkHttpClient.Builder()
                                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                    .build()
                                val request = okhttp3.Request.Builder()
                                    .url("${urlStr.removeSuffix("/")}/health")
                                    .build()
                                
                                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    client.newCall(request).execute()
                                }
                                
                                if (response.isSuccessful) {
                                    testResult = "Success! Server is fully reachable.\n✔ DNS: $ipAddress\n✔ TCP: Port $port open\n✔ HTTP Status: ${response.code} (Healthy)"
                                } else {
                                    testResult = "Partial Success!\n✔ DNS: $ipAddress\n✔ TCP: Port $port open\n✘ HTTP Status: ${response.code} (Server returned error)"
                                }
                            } catch (e: Exception) {
                                testResult = "Failed HTTP health check!\nCheck URL format.\nError: ${e.message}"
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1.2f),
                    enabled = !isTesting,
                    style = ButtonStyle.Neutral,
                    isSecondary = true,
                    isLoading = isTesting
                )
            }

            if (testResult != null) {
                val isPositive = testResult!!.contains("Success") || 
                                testResult!!.contains("Saved") || 
                                testResult!!.contains("Healthy") ||
                                testResult!!.startsWith("Testing")
                
                val feedbackColor = if (isPositive) Success else Danger
                val feedbackBackground = if (isPositive) Success.copy(alpha = 0.08f) else Danger.copy(alpha = 0.08f)
                val feedbackBorder = if (isPositive) Success.copy(alpha = 0.2f) else Danger.copy(alpha = 0.2f)

                Surface(
                    modifier = Modifier.padding(top = 32.dp).fillMaxWidth(),
                    color = feedbackBackground,
                    shape = Shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, feedbackBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (isPositive && !testResult!!.contains("Partial") && !testResult!!.contains("Testing")) 
                                Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = feedbackColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = testResult!!, 
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = feedbackColor
                        )
                    }
                }
            }
        }
    }
}
