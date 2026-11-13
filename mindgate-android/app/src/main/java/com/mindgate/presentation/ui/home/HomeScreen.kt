package com.mindgate.presentation.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindgate.presentation.ui.applimit.LimitsTabScreen
import com.mindgate.presentation.ui.profile.ProfileTabScreen
import com.mindgate.presentation.ui.screens.dashboard.DashboardScreen
import com.mindgate.presentation.ui.theme.*
import com.mindgate.util.riskColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRetakeAssessment: () -> Unit,
    onLogout: () -> Unit,
    onAddRestriction: () -> Unit,
    onParentConnect: () -> Unit,
    onProfileClick: () -> Unit,
    onExploreClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Home", "Limits", "Growth", "Profile")
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkUsagePermissionAndRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        tabs[tab],
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    if (tab != 3) {
                        IconButton(onClick = onProfileClick) {
                            Icon(AppIcons.Profile, contentDescription = "Profile", tint = TextSecondary)
                        }
                    }
                    if (tab == 0) {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(AppIcons.Sync, contentDescription = "Sync", tint = Primary)
                        }
                    }
                    if (tab == 3) {
                        IconButton(onClick = { viewModel.logout(onLogout) }) {
                            Icon(AppIcons.Logout, contentDescription = "Logout", tint = ActionDanger)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                containerColor = SurfaceCard
            ) {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Dashboard
                                    1 -> AppIcons.Extension
                                    2 -> Icons.Default.TrendingUp
                                    else -> AppIcons.Profile
                                }, 
                                contentDescription = null
                            )
                        },
                        label = { Text(label, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            indicatorColor = PrimaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onAddRestriction = onAddRestriction,
                    onExploreClick = onExploreClick
                )
                1 -> LimitsTabScreen()
                2 -> GrowthTab(viewModel)
                3 -> ProfileTabScreen(
                    onRetakeAssessment = onRetakeAssessment,
                    onParentConnect = onParentConnect,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
private fun GrowthTab(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Growth Journey", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = Shapes.medium
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Weekly Screen Time Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Mon - Sun usage breakdown", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Surface(color = PrimaryContainer, shape = CircleShape) {
                            Text("Target: 3h/day", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Primary)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    
                    // Custom 7-Day Screen Time Canvas Bar Chart
                    val todayMins = (state.totalScreenTimeSeconds / 60L).toInt()
                    WeeklyScreenTimeChart(todayMinutes = todayMins)
                }
            }
        }
        
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                        Text("${state.totalReclaimedMinutes} min", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Time Reclaimed", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF5722))
                        Text("${state.streak} Days", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Wellness Streak", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        
        item {
            Text("Did you know?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(
                    "Blue light from screens can suppress melatonin production, making it harder to fall asleep.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun WeeklyScreenTimeChart(todayMinutes: Int) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    // 7-day usage sample with today's live minutes on the last day
    val usageMinutes = remember(todayMinutes) {
        listOf(130, 225, 90, 255, 170, 190, todayMinutes.coerceAtLeast(15))
    }
    val dailyLimitMinutes = 180 // 3 hours limit threshold
    val maxChartMinutes = 300f // 5 hours max scale

    val successColor = ActionSuccess
    val dangerColor = ActionDanger
    val outlineColor = Outline.copy(alpha = 0.5f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val width = size.width
            val height = size.height
            val numBars = days.size
            val barWidth = 24.dp.toPx()
            val spacing = (width - (numBars * barWidth)) / (numBars + 1)

            // Draw Limit Threshold Line
            val limitY = height - (height * (dailyLimitMinutes / maxChartMinutes))
            drawLine(
                color = dangerColor.copy(alpha = 0.6f),
                start = Offset(0f, limitY),
                end = Offset(width, limitY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Draw Bars
            days.forEachIndexed { index, _ ->
                val mins = usageMinutes[index]
                val isExceeded = mins > dailyLimitMinutes
                val barColor = if (isExceeded) dangerColor else successColor

                val barHeight = (height * (mins / maxChartMinutes)).coerceAtMost(height)
                val x = spacing + index * (barWidth + spacing)
                val y = height - barHeight

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Day Labels & Time Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEachIndexed { index, day ->
                val mins = usageMinutes[index]
                val h = mins / 60
                val m = mins % 60
                val timeLabel = if (h > 0) "${h}h" else "${m}m"

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp)
                ) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (mins > dailyLimitMinutes) ActionDanger else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
