package com.mindgate.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mindgate.util.AppInfoHelper
import com.mindgate.util.UsageAccuracyReport
import com.mindgate.util.UsageMethod
import com.mindgate.util.UsageStatsHelper
import kotlin.math.abs

fun formatSecondsToStr(sec: Long): String {
    val h = sec / 3600L
    val m = (sec % 3600L) / 60L
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

fun parseDwInputToSeconds(input: String): Long {
    val trimmed = input.trim().lowercase()
    if (trimmed.isBlank()) return 0L

    try {
        if (trimmed.contains("h") || trimmed.contains("m")) {
            var total = 0L
            val hMatch = Regex("(\\d+)\\s*h").find(trimmed)
            val mMatch = Regex("(\\d+)\\s*m").find(trimmed)
            if (hMatch != null) total += hMatch.groupValues[1].toLong() * 3600L
            if (mMatch != null) total += mMatch.groupValues[1].toLong() * 60L
            return total
        }
        val mins = trimmed.toLongOrNull() ?: 0L
        return mins * 60L
    } catch (e: Exception) {
        return 0L
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageAccuracyReportDialog(
    usageStatsHelper: UsageStatsHelper,
    appInfoHelper: AppInfoHelper,
    onDismiss: () -> Unit
) {
    var report by remember { mutableStateOf<UsageAccuracyReport?>(null) }
    var activeMethod by remember { mutableStateOf(UsageStatsHelper.USAGE_METHOD) }
    var dwTotalInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        report = usageStatsHelper.generateAccuracyReport()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Usage Accuracy Audit",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "Select Active Calculation Method:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UsageMethod.values().forEach { method ->
                        FilterChip(
                            selected = activeMethod == method,
                            onClick = {
                                activeMethod = method
                                UsageStatsHelper.USAGE_METHOD = method
                                report = usageStatsHelper.generateAccuracyReport()
                            },
                            label = { Text(method.name.replace("METHOD_", "M-"), fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = dwTotalInput,
                    onValueChange = { dwTotalInput = it },
                    label = { Text("Digital Wellbeing Total (e.g., '4h 39m' or '279')") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true
                )

                val dwTotalSec = parseDwInputToSeconds(dwTotalInput)

                report?.let { rep ->
                    val sumA = rep.methodAResults.values.sum()
                    val sumB = rep.methodBResults.values.sum()
                    val sumC = rep.methodCResults.values.sum()
                    val sumD = rep.methodDResults.values.sum()

                    val diffA = if (dwTotalSec > 0) abs(sumA - dwTotalSec) / 60 else null
                    val diffB = if (dwTotalSec > 0) abs(sumB - dwTotalSec) / 60 else null
                    val diffC = if (dwTotalSec > 0) abs(sumC - dwTotalSec) / 60 else null
                    val diffD = if (dwTotalSec > 0) abs(sumD - dwTotalSec) / 60 else null

                    val minDiff = listOfNotNull(diffA, diffB, diffC, diffD).minOrNull()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Totals & Differences vs. Digital Wellbeing:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Method A (Events Single): ${formatSecondsToStr(sumA)}" + if (diffA != null) " (Diff: ${diffA}m)" else "")
                            Text("Method B (Events Class): ${formatSecondsToStr(sumB)}" + if (diffB != null) " (Diff: ${diffB}m)" else "")
                            Text("Method C (System Aggregated): ${formatSecondsToStr(sumC)}" + if (diffC != null) " (Diff: ${diffC}m)" else "", color = if (diffC == minDiff && minDiff != null) MaterialTheme.colorScheme.primary else Color.Unspecified, fontWeight = if (diffC == minDiff && minDiff != null) FontWeight.Bold else FontWeight.Normal)
                            Text("Method D (INTERVAL_DAILY): ${formatSecondsToStr(sumD)}" + if (diffD != null) " (Diff: ${diffD}m)" else "")

                            if (minDiff != null) {
                                val closestName = when (minDiff) {
                                    diffC -> "Method C (System Aggregated)"
                                    diffA -> "Method A (Events Single)"
                                    diffB -> "Method B (Events Class)"
                                    else -> "Method D (INTERVAL_DAILY)"
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("Closest to Digital Wellbeing: $closestName", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }
                        }
                    }

                    val allPackages = (rep.methodAResults.keys + rep.methodBResults.keys + rep.methodCResults.keys + rep.methodDResults.keys).distinct().sorted()

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("Per-App Breakdown Matrix:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        items(allPackages, key = { it }) { pkg ->
                            val appName = appInfoHelper.getAppName(pkg)
                            val secA = rep.methodAResults[pkg] ?: 0L
                            val secB = rep.methodBResults[pkg] ?: 0L
                            val secC = rep.methodCResults[pkg] ?: 0L
                            val secD = rep.methodDResults[pkg] ?: 0L

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Text(appName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(pkg, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(2.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("A: ${formatSecondsToStr(secA)}", fontSize = 11.sp)
                                        Text("B: ${formatSecondsToStr(secB)}", fontSize = 11.sp)
                                        Text("C: ${formatSecondsToStr(secC)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("D: ${formatSecondsToStr(secD)}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        if (rep.excludedPackagesLog.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text("Excluded Packages & Reasons:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            items(rep.excludedPackagesLog, key = { it.first }) { (pkg, reason) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(pkg, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text(reason, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
