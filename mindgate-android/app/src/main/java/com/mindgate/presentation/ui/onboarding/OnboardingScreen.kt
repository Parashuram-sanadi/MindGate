package com.mindgate.presentation.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pages = listOf(
        OnboardingPage(
            "Understand Your Habit",
            "Take a science-backed assessment to learn how your device use affects your wellbeing.",
            Icons.Default.Assignment
        ),
        OnboardingPage(
            "Personalized Limits",
            "Get smart daily limits tailored to your addiction risk level.",
            AppIcons.Extension
        ),
        OnboardingPage(
            "Stay in Control",
            "Earn extensions through mindful challenges — not mindless scrolling.",
            AppIcons.Approved
        )
    )
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = Surface) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(240.dp),
                        shape = CircleShape,
                        color = PrimaryContainer.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                modifier = Modifier.size(160.dp),
                                shape = CircleShape,
                                color = PrimaryContainer,
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = pages[page].icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = Primary
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(64.dp))
                    
                    Text(
                        text = pages[page].title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        ),
                        color = TextPrimary
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    Text(
                        text = pages[page].description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        ),
                        color = TextSecondary
                    )
                }
            }
            
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modern Page indicator
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 32.dp else 12.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "indicator_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(if (isSelected) Primary else Outline)
                        )
                    }
                }

                if (pagerState.currentPage == pages.lastIndex) {
                    MindGateButton(
                        text = "Get Started",
                        onClick = {
                            scope.launch {
                                viewModel.completeOnboarding()
                                onDone()
                            }
                        },
                        style = ButtonStyle.Primary
                    )
                } else {
                    Surface(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = Primary,
                        contentColor = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(AppIcons.Forward, contentDescription = "Next", modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}
