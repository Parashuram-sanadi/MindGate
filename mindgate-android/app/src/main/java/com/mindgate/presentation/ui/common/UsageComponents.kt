package com.mindgate.presentation.ui.common

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppUsageListItem(
    appName: String,
    usedMinutes: Int,
    limitMinutes: Int,
    enabled: Boolean,
    icon: Bitmap?,
    onClick: (() -> Unit)? = null,
    middleContent: @Composable (ColumnScope.() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = { onClick?.invoke() },
        enabled = onClick != null
    ) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(appName, fontWeight = FontWeight.Bold)
                val limitText = if (enabled) "${usedMinutes}m / ${limitMinutes}m" else "No limit set"
                Text(limitText, style = MaterialTheme.typography.bodySmall)

                if (enabled) {
                    val progress = (usedMinutes.toFloat() / limitMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(4.dp)
                            .clip(CircleShape),
                        color = if (progress >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                
                middleContent?.invoke(this)
            }

            trailingContent?.invoke(this)
        }
    }
}

@Composable
fun UsageSummaryCard(
    totalMinutes: Int,
    limitMinutes: Int,
    exceeded: Boolean,
    title: String = "Today's Usage"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (exceeded) MaterialTheme.colorScheme.errorContainer 
                           else MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = totalMinutes.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (exceeded) MaterialTheme.colorScheme.error 
                            else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = " / $limitMinutes min",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val progress = (totalMinutes.toFloat() / limitMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (exceeded) MaterialTheme.colorScheme.error 
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            if (exceeded) {
                Text(
                    "Limit exceeded by ${totalMinutes - limitMinutes} min",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
