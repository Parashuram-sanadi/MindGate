package com.mindgate.presentation.ui.screens.applimit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mindgate.data.local.PreferencesManager
import com.mindgate.data.local.entities.AppLimitEntity
import com.mindgate.data.repository.AssessmentRepository
import com.mindgate.data.repository.UsageRepository
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*
import com.mindgate.util.AppInfoHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLimitSetupScreen(
    packageNames: List<String>,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: AppLimitSetupViewModel = hiltViewModel()
) {
    var limitMinutes by remember { mutableIntStateOf(60) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    var bedtimeLockEnabled by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val appName = remember(packageNames) {
        if (packageNames.size == 1) {
            try {
                val pm = context.packageManager
                val info = pm.getApplicationInfo(packageNames[0], 0)
                pm.getApplicationLabel(info).toString()
            } catch (e: Exception) { "App" }
        } else ""
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Limits", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(AppIcons.Back, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(AppIcons.Profile, contentDescription = "Profile", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.mindgate.presentation.ui.theme.Surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = SurfaceCard
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MindGateButton(
                        text = "Cancel",
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        style = ButtonStyle.Neutral,
                        isSecondary = true
                    )
                    MindGateButton(
                        text = "Activate",
                        onClick = { 
                            viewModel.saveLimits(packageNames, limitMinutes)
                            onSave() 
                        },
                        modifier = Modifier.weight(1f),
                        style = ButtonStyle.Primary,
                        icon = AppIcons.Approved
                    )
                }
            }
        },
        containerColor = Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (packageNames.size == 1) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(packageNames[0]).build(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(Shapes.small)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                } else {
                    Box(
                        modifier = Modifier.size(48.dp).background(PrimaryContainer, Shapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Apps, null, tint = Primary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "${packageNames.size} Apps selected",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }
            }

            HorizontalDivider(color = Outline, thickness = 1.dp)
            Spacer(Modifier.height(24.dp))

            // Time Limit Picker Header
            Text(
                text = "Daily Time Limit",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // Quick Preset Limit Chips ("15m", "30m", "45m", "1h", "2h")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h", 120 to "2h")
                presets.forEach { (mins, label) ->
                    val isSelected = limitMinutes == mins
                    FilterChip(
                        selected = isSelected,
                        onClick = { limitMinutes = mins },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Slider with 15m increments
            Slider(
                value = limitMinutes.toFloat(),
                onValueChange = { limitMinutes = (it / 15).toInt() * 15 },
                valueRange = 15f..480f,
                steps = (480 - 15) / 15 - 1,
                colors = SliderDefaults.colors(
                    thumbColor = Primary,
                    activeTrackColor = Primary,
                    inactiveTrackColor = Outline
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatMinutes(limitMinutes),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(32.dp))

            // Day Selector
            Text(
                text = "Active Days",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            DaySelector(
                selectedDays = selectedDays,
                onDaysChanged = { selectedDays = it }
            )

            Spacer(Modifier.height(32.dp))

            // Bedtime Lock
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = Shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bedtime, null, tint = Primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Bedtime Lock", style = MaterialTheme.typography.titleMedium)
                        }
                        Switch(
                            checked = bedtimeLockEnabled,
                            onCheckedChange = { bedtimeLockEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                        )
                    }
                    if (bedtimeLockEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Locks app between 11:00 PM and 7:00 AM",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            
            // Preview Text
            Text(
                text = "App will be blocked after ${formatMinutes(limitMinutes)} per day",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun DaySelector(
    selectedDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit
) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, day ->
            val dayNum = index + 1
            val isSelected = selectedDays.contains(dayNum)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) ActionPrimary else Color.Transparent)
                    .border(1.dp, if (isSelected) ActionPrimary else Outline, CircleShape)
                    .clickable {
                        val newDays = if (isSelected) selectedDays - dayNum else selectedDays + dayNum
                        onDaysChanged(newDays)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "$h hr $m min"
        h > 0 -> "$h hr"
        else -> "$m min"
    }
}

@HiltViewModel
class AppLimitSetupViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val assessmentRepository: AssessmentRepository,
    private val prefs: PreferencesManager,
    private val appInfoHelper: AppInfoHelper
) : ViewModel() {
    
    fun saveLimits(packageNames: List<String>, minutes: Int) {
        viewModelScope.launch {
            val userId = prefs.currentUserId.first()
            if (userId <= 0L) return@launch
            
            val risk = assessmentRepository.getLatestResult(userId)?.riskLevel ?: "Moderate"
            val suggested = usageRepository.suggestedLimitForRisk(risk)
            
            val limits = packageNames.map { pkg ->
                AppLimitEntity(
                    userId = userId,
                    packageName = pkg,
                    appName = appInfoHelper.getAppName(pkg),
                    dailyLimitMinutes = minutes,
                    isEnabled = true,
                    suggestedLimit = suggested
                )
            }
            usageRepository.saveLimits(userId, limits)
        }
    }
}
