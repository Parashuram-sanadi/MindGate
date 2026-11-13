package com.mindgate.presentation.ui.applimit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindgate.data.local.entities.AppLimitEntity
import com.mindgate.presentation.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleLimitSheet(
    limit: AppLimitEntity,
    onDismiss: () -> Unit,
    onSave: (AppLimitEntity) -> Unit
) {
    var isCustom by remember { mutableStateOf(limit.isCustomSchedule) }
    var dailyMinutes by remember { mutableIntStateOf(limit.dailyLimitMinutes) }
    
    var mon by remember { mutableIntStateOf(limit.monLimit ?: limit.dailyLimitMinutes) }
    var tue by remember { mutableIntStateOf(limit.tueLimit ?: limit.dailyLimitMinutes) }
    var wed by remember { mutableIntStateOf(limit.wedLimit ?: limit.dailyLimitMinutes) }
    var thu by remember { mutableIntStateOf(limit.thuLimit ?: limit.dailyLimitMinutes) }
    var fri by remember { mutableIntStateOf(limit.friLimit ?: limit.dailyLimitMinutes) }
    var sat by remember { mutableIntStateOf(limit.satLimit ?: limit.dailyLimitMinutes) }
    var sun by remember { mutableIntStateOf(limit.sunLimit ?: limit.dailyLimitMinutes) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Reschedule ${limit.appName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = Primary)
                Spacer(Modifier.width(12.dp))
                Text("Custom weekly schedule", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Switch(checked = isCustom, onCheckedChange = { isCustom = it })
            }
            
            Spacer(Modifier.height(24.dp))
            
            if (!isCustom) {
                LimitSlider(
                    label = "Daily Limit",
                    minutes = dailyMinutes,
                    onChanged = { dailyMinutes = it }
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val values = listOf(mon, tue, wed, thu, fri, sat, sun)
                    val setters = listOf<(Int)->Unit>(
                        {mon=it}, {tue=it}, {wed=it}, {thu=it}, {fri=it}, {sat=it}, {sun=it}
                    )
                    
                    days.forEachIndexed { index, day ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(day, modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold)
                            Slider(
                                value = values[index].toFloat(),
                                onValueChange = { setters[index](it.toInt()) },
                                valueRange = 5f..480f,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${values[index]}m", modifier = Modifier.width(45.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        }
                    }
                    
                    Button(
                        onClick = {
                            val max = values.max()
                            mon=max; tue=max; wed=max; thu=max; fri=max; sat=max; sun=max
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer, contentColor = Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy highest to all days")
                    }
                    
                    OutlinedButton(
                        onClick = { isCustom = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Revert to flat daily limit")
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = {
                    onSave(limit.copy(
                        isCustomSchedule = isCustom,
                        dailyLimitMinutes = dailyMinutes,
                        monLimit = if (isCustom) mon else null,
                        tueLimit = if (isCustom) tue else null,
                        wedLimit = if (isCustom) wed else null,
                        thuLimit = if (isCustom) thu else null,
                        friLimit = if (isCustom) fri else null,
                        satLimit = if (isCustom) sat else null,
                        sunLimit = if (isCustom) sun else null
                    ))
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = Shapes.small
            ) {
                Text("Update Schedule")
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun LimitSlider(label: String, minutes: Int, onChanged: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            
            var textValue by remember(minutes) { mutableStateOf(minutes.toString()) }
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    it.toIntOrNull()?.let { v -> onChanged(v.coerceIn(5, 480)) }
                },
                modifier = Modifier.width(80.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                suffix = { Text("min") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }
        Slider(
            value = minutes.toFloat(),
            onValueChange = { onChanged(it.toInt()) },
            valueRange = 5f..480f,
            steps = 31
        )
        Text(
            text = "${minutes / 60}h ${minutes % 60}m",
            style = MaterialTheme.typography.labelSmall,
            color = Primary,
            modifier = Modifier.align(Alignment.End)
        )
    }
}
