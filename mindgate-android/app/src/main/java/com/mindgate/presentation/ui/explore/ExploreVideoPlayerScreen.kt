package com.mindgate.presentation.ui.explore

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mindgate.data.local.content.InspirationVideo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreVideoPlayerScreen(
    video: InspirationVideo,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(video.title, fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (video.videoId != null) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            
                            // Essential for YouTube embed authorization
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Mobile Safari/537.36"
                            
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()

                            val html = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <style>
                                        body { margin: 0; padding: 0; background: black; }
                                        .video-container { position: relative; padding-bottom: 56.25%; height: 0; overflow: hidden; }
                                        .video-container iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 0; }
                                    </style>
                                </head>
                                <body>
                                    <div class="video-container">
                                        <iframe 
                                            src="https://www.youtube.com/embed/${video.videoId}?autoplay=1&modestbranding=1&rel=0&showinfo=0&origin=https://www.youtube.com" 
                                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                                            allowfullscreen
                                            referrerpolicy="strict-origin-when-cross-origin">
                                        </iframe>
                                    </div>
                                </body>
                                </html>
                            """.trimIndent()

                            // Load with a trusted base URL to satisfy YouTube's origin checks
                            loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f)
                )
            } else {
                // Fallback path
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Video not available directly.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${video.searchQuery ?: video.title}"))
                        context.startActivity(intent)
                    }) {
                        Text("Search on YouTube")
                    }
                }
            }
        }
    }
}
