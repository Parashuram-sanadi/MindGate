package com.mindgate.presentation.ui.teacher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.data.remote.dto.BatchFileEntry
import com.mindgate.presentation.ui.theme.*
import com.mindgate.presentation.viewmodel.TeacherViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherResultsScreen(
    onBack: () -> Unit,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    val results by viewModel.results.collectAsState()
    var selectedStudent by remember { mutableStateOf<BatchFileEntry?>(null) }
    var riskFilter by remember { mutableStateOf<String>("All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cohort Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.Back, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.mindgate.presentation.ui.theme.Surface)
            )
        },
        containerColor = Surface
    ) { padding ->
        if (results == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No data available. Please upload a dataset.")
            }
        } else {
            val data = results!!
            val students = data.results
            val summary = data.summary
            
            val filteredStudents = remember(riskFilter, students) {
                if (riskFilter == "All") {
                    students.sortedByDescending { it.prediction.riskScore }
                } else {
                    students.filter { it.prediction.riskLevel.equals(riskFilter, ignoreCase = true) }
                        .sortedByDescending { it.prediction.riskScore }
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Section 1: Aggregate Overview & Stacked Risk Bar
                AggregateOverview(
                    totalStudents = students.size,
                    riskCounts = students.groupBy { it.prediction.riskLevel.lowercase() }.mapValues { it.value.size },
                    avgRisk = (summary["avg_risk_score"] as? Double) ?: 0.0,
                    avgScreenTime = summary["avg_screen_time_numeric"]?.toString() ?: "N/A",
                    avgSleep = summary["avg_sleep_hours"]?.toString() ?: "N/A"
                )

                Spacer(Modifier.height(12.dp))

                // Section 2: Fast Filter Chips
                RiskFilterRow(selectedFilter = riskFilter) { riskFilter = it }

                Spacer(Modifier.height(8.dp))

                // Section 3: Student Roster List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredStudents) { entry ->
                        StudentRow(entry) { selectedStudent = entry }
                    }
                }
            }
        }
    }

    // Student Detail Modal
    if (selectedStudent != null) {
        StudentDetailDialog(entry = selectedStudent!!) { selectedStudent = null }
    }
}

@Composable
fun AggregateOverview(
    totalStudents: Int,
    riskCounts: Map<String, Int>,
    avgRisk: Double,
    avgScreenTime: String,
    avgSleep: String
) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        color = SurfaceCard,
        shape = Shapes.medium,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Class Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = TextPrimary
            )
            Text(
                text = "$totalStudents students evaluated in cohort",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(Modifier.height(16.dp))

            // Stacked Multi-Color Risk Distribution Bar
            Text(
                text = "Class Risk Distribution",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            StackedRiskBar(riskCounts = riskCounts, totalStudents = totalStudents)

            Spacer(Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RiskDonutChart(
                    modifier = Modifier.size(100.dp),
                    counts = riskCounts
                )
                
                Spacer(Modifier.width(24.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RiskLegendItem("Severe", riskCounts["severe"] ?: 0, Danger)
                    RiskLegendItem("High", riskCounts["high"] ?: 0, Warning)
                    RiskLegendItem("Moderate", riskCounts["moderate"] ?: 0, Color(0xFFFBC02D))
                    RiskLegendItem("Low", riskCounts["low"] ?: 0, Success)
                }
            }
            
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Outline.copy(alpha = 0.5f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard("Avg Risk", String.format(Locale.getDefault(), "%.1f", avgRisk), Modifier.weight(1f))
                StatCard("Avg Screen", "$avgScreenTime h", Modifier.weight(1f))
                StatCard("Avg Sleep", "$avgSleep h", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StackedRiskBar(riskCounts: Map<String, Int>, totalStudents: Int) {
    if (totalStudents == 0) return

    val lowCount = riskCounts["low"] ?: 0
    val modCount = riskCounts["moderate"] ?: 0
    val highCount = riskCounts["high"] ?: 0
    val sevCount = riskCounts["severe"] ?: 0

    val lowWeight = lowCount.toFloat() / totalStudents
    val modWeight = modCount.toFloat() / totalStudents
    val highWeight = highCount.toFloat() / totalStudents
    val sevWeight = sevCount.toFloat() / totalStudents

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(Outline.copy(alpha = 0.2f))
        ) {
            if (lowWeight > 0) {
                Box(
                    modifier = Modifier
                        .weight(lowWeight)
                        .fillMaxHeight()
                        .background(Success)
                )
            }
            if (modWeight > 0) {
                Box(
                    modifier = Modifier
                        .weight(modWeight)
                        .fillMaxHeight()
                        .background(Color(0xFFFBC02D))
                )
            }
            if (highWeight > 0) {
                Box(
                    modifier = Modifier
                        .weight(highWeight)
                        .fillMaxHeight()
                        .background(Warning)
                )
            }
            if (sevWeight > 0) {
                Box(
                    modifier = Modifier
                        .weight(sevWeight)
                        .fillMaxHeight()
                        .background(Danger)
                )
            }
        }
    }
}

@Composable
fun RiskDonutChart(modifier: Modifier, counts: Map<String, Int>) {
    val total = counts.values.sum().toFloat()
    if (total == 0f) return

    val severe = (counts["severe"] ?: 0) / total
    val high = (counts["high"] ?: 0) / total
    val moderate = (counts["moderate"] ?: 0) / total
    val low = (counts["low"] ?: 0) / total

    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        
        var startAngle = -90f
        
        // Severe
        if (severe > 0) {
            drawArc(
                color = Danger,
                startAngle = startAngle,
                sweepAngle = severe * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += severe * 360f
        }
        
        // High
        if (high > 0) {
            drawArc(
                color = Warning,
                startAngle = startAngle,
                sweepAngle = high * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += high * 360f
        }
        
        // Moderate
        if (moderate > 0) {
            drawArc(
                color = Color(0xFFFBC02D),
                startAngle = startAngle,
                sweepAngle = moderate * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += moderate * 360f
        }
        
        // Low
        if (low > 0) {
            drawArc(
                color = Success,
                startAngle = startAngle,
                sweepAngle = low * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun RiskLegendItem(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text = "$label: $count", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ActionPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun RiskFilterRow(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf("All", "Low", "Moderate", "High", "Severe")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            val filterColor = when (filter.lowercase()) {
                "low" -> Success
                "moderate" -> Color(0xFFFBC02D)
                "high" -> Warning
                "severe" -> Danger
                else -> Primary
            }
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (filter != "All") {
                            Box(Modifier.size(8.dp).background(filterColor, CircleShape))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(filter)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = filterColor,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun StudentRow(entry: BatchFileEntry, onClick: () -> Unit) {
    val risk = entry.prediction
    val name = entry.studentInfo["Name"] ?: entry.studentInfo["name"] ?: "Student"
    val color = getRiskColor(risk.riskLevel)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = "Score: ${String.format("%.1f", risk.riskScore)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
            ) {
                Text(
                    text = risk.riskLevel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = color,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailDialog(entry: BatchFileEntry, onDismiss: () -> Unit) {
    val risk = entry.prediction
    val name = entry.studentInfo["Name"] ?: entry.studentInfo["name"] ?: "Student"
    
    val subScores = listOf(
        "Compulsion" to 85f,
        "Sleep/Physical" to 70f,
        "Academic" to 45f,
        "Emotional" to 60f,
        "Social" to 30f,
        "Control" to 90f
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(text = "$name Breakdown") },
        text = {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Overall Risk:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${risk.riskLevel} (${String.format("%.1f", risk.riskScore)})",
                        color = getRiskColor(risk.riskLevel),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                Text(text = "Risk Factors", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                subScores.forEach { (label, score) ->
                    SubScoreBar(label, score)
                    Spacer(Modifier.height(12.dp))
                }
                
                if (!risk.topFactors.isNullOrEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(text = "Top Contributing Factors:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    risk.topFactors.forEach { factor ->
                        Text(text = "• $factor", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
    )
}

@Composable
fun SubScoreBar(label: String, score: Float) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(text = "${score.toInt()}/100", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = when {
                score >= 70 -> Danger
                score >= 55 -> Warning
                score >= 35 -> Color(0xFFFBC02D)
                else -> Success
            },
            trackColor = Outline.copy(alpha = 0.2f)
        )
    }
}

fun getRiskColor(level: String): Color = when (level.lowercase()) {
    "severe" -> Danger
    "high" -> Warning
    "moderate" -> Color(0xFFFBC02D)
    "low" -> Success
    else -> TextSecondary
}
